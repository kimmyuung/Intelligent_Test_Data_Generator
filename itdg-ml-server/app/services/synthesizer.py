"""
SDV(Synthetic Data Vault) 기반 데이터 합성 서비스
- GaussianCopula: 빠른 학습, 단순 분포에 적합
- CTGAN: 복잡한 분포 모방, 느린 학습
"""

import os
import uuid
import time
import pickle
import json
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
                    # .pkl 파일만 기준으로 정리 (메타데이터는 함께 삭제)
                    if not filename.endswith('.pkl'):
                        continue
                    
                    filepath = os.path.join(MODELS_DIR, filename)
                    if os.path.isfile(filepath):
                        mtime = os.path.getmtime(filepath)
                        if now - mtime > MODEL_TTL:
                            try:
                                os.remove(filepath)
                                # 메타데이터 파일도 함께 삭제
                                meta_path = filepath.replace('.pkl', '.meta.json')
                                if os.path.exists(meta_path):
                                    os.remove(meta_path)
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
            
            # 모델 타입 메타데이터 저장 (로드 시 필요)
            meta_path = os.path.join(MODELS_DIR, f"{model_id}.meta.json")
            with open(meta_path, 'w') as f:
                json.dump({"model_type": model_type}, f)
            
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
            # 모델 타입 메타데이터 읽기
            meta_path = os.path.join(MODELS_DIR, f"{model_id}.meta.json")
            model_type = "copula"  # 기본값
            if os.path.exists(meta_path):
                with open(meta_path, 'r') as f:
                    meta = json.load(f)
                    model_type = meta.get("model_type", "copula")
            
            # 모델 타입에 따라 올바른 클래스로 로드
            if model_type == "ctgan":
                from sdv.single_table import CTGANSynthesizer
                synthesizer = CTGANSynthesizer.load(model_path)
                logger.info(f"Loaded CTGAN model: {model_id}")
            else:
                from sdv.single_table import GaussianCopulaSynthesizer
                synthesizer = GaussianCopulaSynthesizer.load(model_path)
                logger.info(f"Loaded GaussianCopula model: {model_id}")
            
            # 합성 데이터 생성
            synthetic_df = synthesizer.sample(num_rows=num_rows)
            
            # DF → JSON 변환
            data = synthetic_df.to_dict(orient='records')
            
            return {
                "success": True,
                "data": data,
                "columns": list(synthetic_df.columns),
                "rowCount": len(data),
                "modelType": model_type  # 모델 타입 포함
            }
            
        except Exception as e:
            logger.error(f"Generation failed: {e}")
            raise Exception(f"Data generation failed: {str(e)}")
    
    def get_model_info(self, model_id: str) -> Optional[Dict[str, Any]]:
        """모델 정보 조회"""
        model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
        
        if not os.path.exists(model_path):
            return None
        
        # 모델 타입 메타데이터 읽기
        meta_path = os.path.join(MODELS_DIR, f"{model_id}.meta.json")
        model_type = "copula"
        if os.path.exists(meta_path):
            try:
                with open(meta_path, 'r') as f:
                    meta = json.load(f)
                    model_type = meta.get("model_type", "copula")
            except:
                pass
        
        stat = os.stat(model_path)
        return {
            "modelId": model_id,
            "exists": True,
            "size": stat.st_size,
            "createdAt": stat.st_mtime,
            "expiresIn": max(0, MODEL_TTL - (time.time() - stat.st_mtime)),
            "modelType": model_type
        }
    
    def delete_model(self, model_id: str) -> bool:
        """모델 삭제"""
        model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
        meta_path = os.path.join(MODELS_DIR, f"{model_id}.meta.json")
        
        deleted = False
        if os.path.exists(model_path):
            os.remove(model_path)
            deleted = True
        
        # 메타데이터 파일도 삭제
        if os.path.exists(meta_path):
            os.remove(meta_path)
        
        if deleted:
            logger.info(f"Deleted model: {model_id}")
        
        return deleted


class MultiTableSynthesizer:
    """
    다중 테이블 학습을 위한 SDV HMASynthesizer 래퍼
    
    Foreign Key 관계를 포함한 여러 테이블을 동시에 학습하여
    참조 무결성을 유지하는 합성 데이터를 생성합니다.
    """
    
    def __init__(self):
        os.makedirs(MODELS_DIR, exist_ok=True)
    
    def train(self, tables: Dict[str, pd.DataFrame], relationships: list) -> Dict[str, Any]:
        """
        다중 테이블을 학습합니다.
        
        Args:
            tables: 테이블 이름 → DataFrame 매핑
            relationships: 테이블 간 관계 정의 리스트
                [
                    {
                        "parent_table": "users",
                        "child_table": "orders",
                        "parent_key": "id",
                        "child_key": "user_id"
                    }
                ]
        
        Returns:
            dict: 모델 ID, 상태, 메타데이터
        """
        start_time = time.time()
        model_id = str(uuid.uuid4())
        
        try:
            from sdv.metadata import MultiTableMetadata
            from sdv.multi_table import HMASynthesizer
            
            # 메타데이터 생성
            metadata = MultiTableMetadata()
            
            # 각 테이블의 메타데이터 자동 감지
            for table_name, df in tables.items():
                metadata.detect_table_from_dataframe(table_name, df)
            
            # Primary Key 설정 (관계에서 추론)
            primary_keys = set()
            for rel in relationships:
                parent_table = rel["parent_table"]
                parent_key = rel["parent_key"]
                if (parent_table, parent_key) not in primary_keys:
                    try:
                        metadata.update_column(
                            table_name=parent_table,
                            column_name=parent_key,
                            sdtype='id'
                        )
                        metadata.set_primary_key(parent_table, parent_key)
                        primary_keys.add((parent_table, parent_key))
                    except Exception as e:
                        logger.warning(f"Could not set primary key for {parent_table}.{parent_key}: {e}")
            
            # 테이블 간 관계 설정
            for rel in relationships:
                try:
                    metadata.add_relationship(
                        parent_table_name=rel["parent_table"],
                        child_table_name=rel["child_table"],
                        parent_primary_key=rel["parent_key"],
                        child_foreign_key=rel["child_key"]
                    )
                except Exception as e:
                    logger.warning(f"Could not add relationship: {e}")
            
            # HMA Synthesizer 학습
            synthesizer = HMASynthesizer(metadata)
            synthesizer.fit(tables)
            
            # 모델 저장
            model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
            synthesizer.save(model_path)
            
            # 메타데이터 저장
            meta_path = os.path.join(MODELS_DIR, f"{model_id}.meta.json")
            with open(meta_path, 'w') as f:
                json.dump({
                    "model_type": "hma",
                    "tables": list(tables.keys()),
                    "relationships": relationships
                }, f)
            
            training_time = time.time() - start_time
            
            return {
                "success": True,
                "modelId": model_id,
                "status": "trained",
                "modelType": "hma",
                "tables": list(tables.keys()),
                "relationships": relationships,
                "trainingTime": round(training_time, 2)
            }
            
        except Exception as e:
            logger.error(f"Multi-table training failed: {e}")
            raise Exception(f"Multi-table model training failed: {str(e)}")
    
    def generate(self, model_id: str, scale: float = 1.0) -> Dict[str, Any]:
        """
        학습된 다중 테이블 모델로 합성 데이터를 생성합니다.
        
        Args:
            model_id: 모델 UUID
            scale: 원본 대비 생성 비율 (1.0 = 동일 크기)
        
        Returns:
            dict: 테이블별 생성된 데이터
        """
        model_path = os.path.join(MODELS_DIR, f"{model_id}.pkl")
        
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Model not found: {model_id}")
        
        try:
            from sdv.multi_table import HMASynthesizer
            
            # 모델 로드
            synthesizer = HMASynthesizer.load(model_path)
            logger.info(f"Loaded HMA model: {model_id}")
            
            # 합성 데이터 생성
            synthetic_data = synthesizer.sample(scale=scale)
            
            # 각 테이블 DataFrame → JSON 변환
            result = {}
            for table_name, df in synthetic_data.items():
                result[table_name] = {
                    "data": df.to_dict(orient='records'),
                    "columns": list(df.columns),
                    "rowCount": len(df)
                }
            
            return {
                "success": True,
                "tables": result,
                "modelType": "hma"
            }
            
        except Exception as e:
            logger.error(f"Multi-table generation failed: {e}")
            raise Exception(f"Multi-table data generation failed: {str(e)}")


# 싱글톤 인스턴스
synthesizer = DataSynthesizer()
multi_table_synthesizer = MultiTableSynthesizer()
