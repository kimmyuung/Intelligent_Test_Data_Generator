"""
SDV(Synthetic Data Vault) 기반 데이터 합성 서비스
- GaussianCopula: 빠른 학습, 단순 분포에 적합
- CTGAN: 복잡한 분포 모방, 느린 학습
"""

import os
import uuid
import time
import pickle
import logging
import threading
from typing import Optional, Dict, Any

import pandas as pd

logger = logging.getLogger(__name__)

MODELS_DIR = "models"
MODEL_TTL = 60 * 60  # 1 hour in seconds
CLEANUP_INTERVAL = 60  # Check every 60 seconds

_models_stop_event = threading.Event()


def start_models_cleanup_task():
    """모델 자동 정리 스레드 시작"""
    os.makedirs(MODELS_DIR, exist_ok=True)
    t = threading.Thread(target=_models_cleanup_loop, daemon=True)
    t.start()
    logger.info("Models cleanup background task started")
    return t


def stop_models_cleanup_task():
    """모델 자동 정리 스레드 중지"""
    _models_stop_event.set()


def _models_cleanup_loop():
    """모델 파일 자동 삭제 루프"""
    while not _models_stop_event.is_set():
        try:
            now = time.time()
            if os.path.exists(MODELS_DIR):
                for filename in os.listdir(MODELS_DIR):
                    filepath = os.path.join(MODELS_DIR, filename)
                    if os.path.isfile(filepath):
                        mtime = os.path.getmtime(filepath)
                        if now - mtime > MODEL_TTL:
                            try:
                                os.remove(filepath)
                                logger.info(f"Auto-deleted expired model: {filename}")
                            except Exception as e:
                                logger.error(f"Failed to delete model {filename}: {e}")
        except Exception as e:
            logger.error(f"Error in models cleanup loop: {e}")
        
        time.sleep(CLEANUP_INTERVAL)


class DataSynthesizer:
    """SDV 모델 래퍼 클래스"""
    
    def __init__(self):
        os.makedirs(MODELS_DIR, exist_ok=True)
    
    def train(self, df: pd.DataFrame, model_type: str = "copula") -> Dict[str, Any]:
        """
        데이터를 학습하고 모델을 저장합니다.
        
        Args:
            df: 학습할 DataFrame
            model_type: "copula" (GaussianCopula) 또는 "ctgan" (CTGAN)
        
        Returns:
            dict: 모델 ID, 상태, 메타데이터
        """
        start_time = time.time()
        model_id = str(uuid.uuid4())
        
        try:
            # SDV 임포트 (lazy loading - 서버 시작 시간 단축)
            from sdv.metadata import SingleTableMetadata
            
            # 메타데이터 자동 감지
            metadata = SingleTableMetadata()
            metadata.detect_from_dataframe(df)
            
            # 모델 선택 및 학습
            if model_type == "ctgan":
                from sdv.single_table import CTGANSynthesizer
                synthesizer = CTGANSynthesizer(
                    metadata,
                    epochs=100,  # 빠른 학습을 위해 epochs 제한
                    verbose=True
                )
            else:  # default: copula
                from sdv.single_table import GaussianCopulaSynthesizer
                synthesizer = GaussianCopulaSynthesizer(metadata)
            
            # 학습 수행
            synthesizer.fit(df)
            
            # 모델 저장
            model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
            synthesizer.save(model_path)
            
            training_time = time.time() - start_time
            
            return {
                "success": True,
                "modelId": model_id,
                "status": "trained",
                "modelType": model_type,
                "columns": list(df.columns),
                "rowCount": len(df),
                "trainingTime": round(training_time, 2)
            }
            
        except Exception as e:
            logger.error(f"Training failed: {e}")
            raise Exception(f"Model training failed: {str(e)}")
    
    def generate(self, model_id: str, num_rows: int = 100) -> Dict[str, Any]:
        """
        학습된 모델로 합성 데이터를 생성합니다.
        
        Args:
            model_id: 모델 UUID
            num_rows: 생성할 행 수
        
        Returns:
            dict: 생성된 데이터와 메타정보
        """
        model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
        
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Model not found: {model_id}")
        
        try:
            # SDV 임포트
            from sdv.single_table import GaussianCopulaSynthesizer
            
            # 모델 로드 (GaussianCopula와 CTGAN 모두 동일한 방식)
            synthesizer = GaussianCopulaSynthesizer.load(model_path)
            
            # 합성 데이터 생성
            synthetic_df = synthesizer.sample(num_rows=num_rows)
            
            # DF → JSON 변환
            data = synthetic_df.to_dict(orient='records')
            
            return {
                "success": True,
                "data": data,
                "columns": list(synthetic_df.columns),
                "rowCount": len(data)
            }
            
        except Exception as e:
            logger.error(f"Generation failed: {e}")
            raise Exception(f"Data generation failed: {str(e)}")
    
    def get_model_info(self, model_id: str) -> Optional[Dict[str, Any]]:
        """모델 정보 조회"""
        model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
        
        if not os.path.exists(model_path):
            return None
        
        stat = os.stat(model_path)
        return {
            "modelId": model_id,
            "exists": True,
            "size": stat.st_size,
            "createdAt": stat.st_mtime,
            "expiresIn": max(0, MODEL_TTL - (time.time() - stat.st_mtime))
        }
    
    def delete_model(self, model_id: str) -> bool:
        """모델 삭제"""
        model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
        
        if os.path.exists(model_path):
            os.remove(model_path)
            logger.info(f"Deleted model: {model_id}")
            return True
        return False


# 싱글톤 인스턴스
synthesizer = DataSynthesizer()
