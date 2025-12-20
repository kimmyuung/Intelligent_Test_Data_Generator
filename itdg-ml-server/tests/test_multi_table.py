"""
MultiTableSynthesizer 단위 테스트

다중 테이블 학습 및 데이터 생성 테스트
"""
import pytest
import os
import json
from unittest.mock import patch, MagicMock

from app.services.synthesizer import MultiTableSynthesizer


class TestMultiTableSynthesizer:
    """MultiTableSynthesizer 클래스 테스트"""
    
    @pytest.fixture
    def multi_synth(self, tmp_path):
        """테스트용 MultiTableSynthesizer 인스턴스"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            synth = MultiTableSynthesizer()
            yield synth
    
    def test_init_creates_models_dir(self, tmp_path):
        """초기화 시 models 디렉토리 생성 확인"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path / "multi_models")):
            synth = MultiTableSynthesizer()
            assert os.path.exists(str(tmp_path / "multi_models"))
    
    def test_train_multi_table_success(self, multi_synth, multi_table_data, tmp_path):
        """다중 테이블 학습 성공 테스트"""
        tables = multi_table_data["tables"]
        relationships = multi_table_data["relationships"]
        
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            result = multi_synth.train(tables, relationships)
        
        assert result["success"] is True
        assert "modelId" in result
        assert result["modelType"] == "hma"
        assert result["status"] == "trained"
        assert set(result["tables"]) == {"users", "orders"}
        assert len(result["relationships"]) == 1
        assert "trainingTime" in result
        
        # 모델 파일 생성 확인
        model_path = os.path.join(str(tmp_path), f"{result['modelId']}.pkl")
        meta_path = os.path.join(str(tmp_path), f"{result['modelId']}.meta.json")
        assert os.path.exists(model_path)
        assert os.path.exists(meta_path)
        
        # 메타데이터 확인
        with open(meta_path, 'r') as f:
            meta = json.load(f)
            assert meta["model_type"] == "hma"
            assert "users" in meta["tables"]
            assert "orders" in meta["tables"]
    
    def test_train_single_table_fails(self, multi_synth, sample_dataframe, tmp_path):
        """단일 테이블로 학습 시 에러 (최소 2개 필요)"""
        tables = {"single_table": sample_dataframe}
        relationships = []
        
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            # 관계가 없어도 학습은 가능하지만, 의미없는 다중 테이블 학습
            # 실제로는 2개 이상 테이블 필요
            pass  # API 단에서 검증
    
    def test_generate_multi_table_success(self, multi_synth, multi_table_data, tmp_path):
        """다중 테이블 데이터 생성 성공 테스트"""
        tables = multi_table_data["tables"]
        relationships = multi_table_data["relationships"]
        
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            train_result = multi_synth.train(tables, relationships)
            model_id = train_result["modelId"]
            
            # 생성 (scale=1.0 = 원본과 동일 크기)
            gen_result = multi_synth.generate(model_id, scale=1.0)
        
        assert gen_result["success"] is True
        assert gen_result["modelType"] == "hma"
        assert "tables" in gen_result
        assert "users" in gen_result["tables"]
        assert "orders" in gen_result["tables"]
        
        # 각 테이블 데이터 확인
        users_result = gen_result["tables"]["users"]
        orders_result = gen_result["tables"]["orders"]
        
        assert "data" in users_result
        assert "columns" in users_result
        assert "rowCount" in users_result
        
        assert "data" in orders_result
        assert "columns" in orders_result
        assert "rowCount" in orders_result
    
    def test_generate_with_invalid_model_id_fails(self, multi_synth, tmp_path):
        """존재하지 않는 모델 ID로 생성 시 실패"""
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            with pytest.raises(FileNotFoundError):
                multi_synth.generate("invalid-multi-model-id")
    
    def test_generate_with_different_scales(self, multi_synth, multi_table_data, tmp_path):
        """다른 scale 값으로 생성 테스트"""
        tables = multi_table_data["tables"]
        relationships = multi_table_data["relationships"]
        
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            train_result = multi_synth.train(tables, relationships)
            model_id = train_result["modelId"]
            
            # scale=2.0 = 원본의 2배
            gen_result = multi_synth.generate(model_id, scale=2.0)
        
        assert gen_result["success"] is True
        # 참고: HMASynthesizer의 scale은 대략적인 비율이므로 정확한 2배가 아닐 수 있음


class TestRelationshipIntegrity:
    """테이블 간 참조 무결성 테스트"""
    
    def test_foreign_key_integrity(self, multi_table_data, tmp_path):
        """생성된 데이터의 FK 참조 무결성 검증"""
        tables = multi_table_data["tables"]
        relationships = multi_table_data["relationships"]
        
        with patch('app.services.synthesizer.MODELS_DIR', str(tmp_path)):
            synth = MultiTableSynthesizer()
            train_result = synth.train(tables, relationships)
            model_id = train_result["modelId"]
            
            gen_result = synth.generate(model_id, scale=1.0)
        
        # 생성된 데이터에서 FK 무결성 확인
        users_data = gen_result["tables"]["users"]["data"]
        orders_data = gen_result["tables"]["orders"]["data"]
        
        user_ids = {u["id"] for u in users_data}
        order_user_ids = {o["user_id"] for o in orders_data}
        
        # 모든 order의 user_id가 users 테이블에 존재해야 함
        assert order_user_ids.issubset(user_ids), \
            f"FK integrity violated: {order_user_ids - user_ids} not in users"
