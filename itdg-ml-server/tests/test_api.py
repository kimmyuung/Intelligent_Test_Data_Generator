"""
FastAPI 엔드포인트 테스트

API 통합 테스트
"""
import pytest
import io
import pandas as pd
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock

# FastAPI 앱 임포트
from app.main import app


@pytest.fixture
def client():
    """테스트용 FastAPI 클라이언트"""
    return TestClient(app)


class TestHealthEndpoint:
    """헬스체크 엔드포인트 테스트"""
    
    def test_health_check(self, client):
        """GET /health 테스트"""
        response = client.get("/health")
        
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "version" in data
        assert "sdv_available" in data


class TestAnalyzeEndpoint:
    """파일 분석 엔드포인트 테스트"""
    
    def test_analyze_csv_success(self, client, sample_dataframe):
        """POST /api/v1/analyze - CSV 파일 분석 성공"""
        # CSV 파일 생성
        csv_buffer = io.BytesIO()
        sample_dataframe.to_csv(csv_buffer, index=False)
        csv_buffer.seek(0)
        
        response = client.post(
            "/api/v1/analyze",
            files={"file": ("test.csv", csv_buffer, "text/csv")}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "fileId" in data
        assert "columns" in data
        assert "rowCount" in data
        assert data["rowCount"] == 5
    
    def test_analyze_excel_success(self, client, sample_dataframe):
        """POST /api/v1/analyze - Excel 파일 분석 성공"""
        # Excel 파일 생성
        excel_buffer = io.BytesIO()
        sample_dataframe.to_excel(excel_buffer, index=False, engine='openpyxl')
        excel_buffer.seek(0)
        
        response = client.post(
            "/api/v1/analyze",
            files={"file": ("test.xlsx", excel_buffer, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
    
    def test_analyze_unsupported_file_fails(self, client):
        """POST /api/v1/analyze - 지원되지 않는 파일 형식"""
        response = client.post(
            "/api/v1/analyze",
            files={"file": ("test.txt", b"some text content", "text/plain")}
        )
        
        assert response.status_code == 400
    
    def test_analyze_empty_file_fails(self, client):
        """POST /api/v1/analyze - 빈 파일"""
        csv_buffer = io.BytesIO(b"")
        
        response = client.post(
            "/api/v1/analyze",
            files={"file": ("empty.csv", csv_buffer, "text/csv")}
        )
        
        assert response.status_code == 400 or response.status_code == 500


class TestTrainEndpoint:
    """모델 학습 엔드포인트 테스트"""
    
    @pytest.fixture
    def uploaded_file_id(self, client, sample_dataframe):
        """파일 업로드 후 file_id 반환"""
        csv_buffer = io.BytesIO()
        sample_dataframe.to_csv(csv_buffer, index=False)
        csv_buffer.seek(0)
        
        response = client.post(
            "/api/v1/analyze",
            files={"file": ("test.csv", csv_buffer, "text/csv")}
        )
        return response.json()["fileId"]
    
    def test_train_copula_success(self, client, uploaded_file_id):
        """POST /api/v1/train - Copula 모델 학습 성공"""
        response = client.post(
            "/api/v1/train",
            params={"file_id": uploaded_file_id, "model_type": "copula"}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "modelId" in data
        assert data["modelType"] == "copula"
        assert data["status"] == "trained"
    
    def test_train_invalid_file_id_fails(self, client):
        """POST /api/v1/train - 잘못된 file_id"""
        response = client.post(
            "/api/v1/train",
            params={"file_id": "invalid-file-id", "model_type": "copula"}
        )
        
        assert response.status_code == 404
    
    def test_train_invalid_model_type_uses_default(self, client, uploaded_file_id):
        """POST /api/v1/train - 잘못된 model_type은 기본값 사용"""
        response = client.post(
            "/api/v1/train",
            params={"file_id": uploaded_file_id, "model_type": "invalid"}
        )
        
        # copula가 기본값으로 사용됨 (또는 에러)
        assert response.status_code in [200, 400]


class TestGenerateEndpoint:
    """데이터 생성 엔드포인트 테스트"""
    
    @pytest.fixture
    def trained_model_id(self, client, sample_dataframe):
        """파일 업로드 + 학습 후 model_id 반환"""
        # 업로드
        csv_buffer = io.BytesIO()
        sample_dataframe.to_csv(csv_buffer, index=False)
        csv_buffer.seek(0)
        
        response = client.post(
            "/api/v1/analyze",
            files={"file": ("test.csv", csv_buffer, "text/csv")}
        )
        file_id = response.json()["fileId"]
        
        # 학습
        response = client.post(
            "/api/v1/train",
            params={"file_id": file_id, "model_type": "copula"}
        )
        return response.json()["modelId"]
    
    def test_generate_success(self, client, trained_model_id):
        """POST /api/v1/generate/{model_id} - 데이터 생성 성공"""
        response = client.post(
            f"/api/v1/generate/{trained_model_id}",
            params={"num_rows": 10}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["rowCount"] == 10
        assert len(data["data"]) == 10
    
    def test_generate_invalid_model_id_fails(self, client):
        """POST /api/v1/generate/{model_id} - 잘못된 model_id"""
        response = client.post(
            "/api/v1/generate/invalid-model-id",
            params={"num_rows": 10}
        )
        
        assert response.status_code == 404


class TestModelEndpoint:
    """모델 관리 엔드포인트 테스트"""
    
    @pytest.fixture
    def trained_model_id(self, client, sample_dataframe):
        """학습된 모델 ID"""
        csv_buffer = io.BytesIO()
        sample_dataframe.to_csv(csv_buffer, index=False)
        csv_buffer.seek(0)
        
        response = client.post(
            "/api/v1/analyze",
            files={"file": ("test.csv", csv_buffer, "text/csv")}
        )
        file_id = response.json()["fileId"]
        
        response = client.post(
            "/api/v1/train",
            params={"file_id": file_id, "model_type": "copula"}
        )
        return response.json()["modelId"]
    
    def test_get_model_info_success(self, client, trained_model_id):
        """GET /api/v1/model/{model_id} - 모델 정보 조회 성공"""
        response = client.get(f"/api/v1/model/{trained_model_id}")
        
        assert response.status_code == 200
        data = response.json()
        assert data["exists"] is True
        assert data["modelId"] == trained_model_id
    
    def test_get_model_info_not_found(self, client):
        """GET /api/v1/model/{model_id} - 존재하지 않는 모델"""
        response = client.get("/api/v1/model/non-existent-model")
        
        assert response.status_code == 404
    
    def test_delete_model_success(self, client, trained_model_id):
        """DELETE /api/v1/model/{model_id} - 모델 삭제 성공"""
        response = client.delete(f"/api/v1/model/{trained_model_id}")
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        
        # 삭제 후 조회 시 404
        response = client.get(f"/api/v1/model/{trained_model_id}")
        assert response.status_code == 404
    
    def test_delete_model_not_found(self, client):
        """DELETE /api/v1/model/{model_id} - 존재하지 않는 모델"""
        response = client.delete("/api/v1/model/non-existent-model")
        
        assert response.status_code == 404
