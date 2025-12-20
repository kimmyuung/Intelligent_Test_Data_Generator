"""
DataSynthesizer 단위 테스트

단일 테이블 학습 및 데이터 생성 테스트
"""
import pytest
import os
import json
from unittest.mock import patch, MagicMock

# 테스트 대상 모듈
from app.services.synthesizer import DataSynthesizer, MODELS_DIR


class TestDataSynthesizer:
    """DataSynthesizer 클래스 테스트"""
    
    @pytest.fixture
    def synthesizer(self, tmp_path):
        """테스트용 Synthesizer 인스턴스"""
        # MODELS_DIR을 임시 디렉토리로 패치
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            synth = DataSynthesizer()
            yield synth
    
    def test_init_creates_models_dir(self, tmp_path):
        """초기화 시 models 디렉토리 생성 확인"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path / "new_models")):
            synth = DataSynthesizer()
            assert os.path.exists(str(tmp_path / "new_models"))
    
    def test_train_copula_success(self, synthesizer, sample_dataframe, tmp_path):
        """GaussianCopula 모델 학습 성공 테스트"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            result = synthesizer.train(sample_dataframe, model_type="copula")
        
        assert result["success"] is True
        assert "modelId" in result
        assert result["modelType"] == "copula"
        assert result["status"] == "trained"
        assert result["rowCount"] == 5
        assert "trainingTime" in result
        
        # 모델 파일 생성 확인
        model_path = os.path.join(str(tmp_path), f"{result['modelId']}.pkl")
        meta_path = os.path.join(str(tmp_path), f"{result['modelId']}.meta.json")
        assert os.path.exists(model_path)
        assert os.path.exists(meta_path)
        
        # 메타데이터 확인
        with open(meta_path, 'r') as f:
            meta = json.load(f)
            assert meta["model_type"] == "copula"
    
    def test_train_ctgan_success(self, synthesizer, sample_dataframe, tmp_path):
        """CTGAN 모델 학습 성공 테스트 (epochs 제한으로 빠르게)"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            # CTGAN은 시간이 오래 걸리므로 mock 사용
            with patch('sdv.single_table.CTGANSynthesizer') as mock_ctgan:
                mock_instance = MagicMock()
                mock_ctgan.return_value = mock_instance
                
                result = synthesizer.train(sample_dataframe, model_type="ctgan")
        
        assert result["success"] is True
        assert result["modelType"] == "ctgan"
    
    def test_train_with_empty_dataframe_fails(self, synthesizer, tmp_path):
        """빈 DataFrame으로 학습 시 실패 테스트"""
        import pandas as pd
        empty_df = pd.DataFrame()
        
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            with pytest.raises(Exception):
                synthesizer.train(empty_df)
    
    def test_generate_success(self, synthesizer, sample_dataframe, tmp_path):
        """데이터 생성 성공 테스트"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            # 먼저 학습
            train_result = synthesizer.train(sample_dataframe, model_type="copula")
            model_id = train_result["modelId"]
            
            # 생성
            gen_result = synthesizer.generate(model_id, num_rows=10)
        
        assert gen_result["success"] is True
        assert gen_result["rowCount"] == 10
        assert len(gen_result["data"]) == 10
        assert "columns" in gen_result
        assert gen_result["modelType"] == "copula"
    
    def test_generate_with_invalid_model_id_fails(self, synthesizer, tmp_path):
        """존재하지 않는 모델 ID로 생성 시 실패"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            with pytest.raises(FileNotFoundError):
                synthesizer.generate("invalid-model-id-12345")
    
    def test_get_model_info_exists(self, synthesizer, sample_dataframe, tmp_path):
        """모델 정보 조회 - 존재하는 모델"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            train_result = synthesizer.train(sample_dataframe, model_type="copula")
            model_id = train_result["modelId"]
            
            info = synthesizer.get_model_info(model_id)
        
        assert info is not None
        assert info["modelId"] == model_id
        assert info["exists"] is True
        assert info["modelType"] == "copula"
        assert "size" in info
        assert "expiresIn" in info
    
    def test_get_model_info_not_exists(self, synthesizer, tmp_path):
        """모델 정보 조회 - 존재하지 않는 모델"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            info = synthesizer.get_model_info("non-existent-model")
        
        assert info is None
    
    def test_delete_model_success(self, synthesizer, sample_dataframe, tmp_path):
        """모델 삭제 성공 테스트"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            train_result = synthesizer.train(sample_dataframe)
            model_id = train_result["modelId"]
            
            # 삭제 전 확인
            model_path = os.path.join(str(tmp_path), f"{model_id}.pkl")
            meta_path = os.path.join(str(tmp_path), f"{model_id}.meta.json")
            assert os.path.exists(model_path)
            assert os.path.exists(meta_path)
            
            # 삭제
            result = synthesizer.delete_model(model_id)
        
        assert result is True
        assert not os.path.exists(model_path)
        assert not os.path.exists(meta_path)
    
    def test_delete_model_not_exists(self, synthesizer, tmp_path):
        """존재하지 않는 모델 삭제"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            result = synthesizer.delete_model("non-existent-model")
        
        assert result is False


class TestModelTypeLoading:
    """모델 타입별 올바른 로드 테스트 (CTGAN 버그 수정 검증)"""
    
    def test_copula_model_loads_correctly(self, sample_dataframe, tmp_path):
        """Copula 모델이 올바르게 로드되는지 테스트"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            synth = DataSynthesizer()
            train_result = synth.train(sample_dataframe, model_type="copula")
            model_id = train_result["modelId"]
            
            # 메타데이터 확인
            meta_path = os.path.join(str(tmp_path), f"{model_id}.meta.json")
            with open(meta_path, 'r') as f:
                meta = json.load(f)
                assert meta["model_type"] == "copula"
            
            # 생성 시 올바른 클래스로 로드되는지 확인
            gen_result = synth.generate(model_id, num_rows=3)
            assert gen_result["success"] is True
            assert gen_result["modelType"] == "copula"
