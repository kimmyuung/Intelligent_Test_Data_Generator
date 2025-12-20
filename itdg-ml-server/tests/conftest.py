"""
pytest 공통 설정 및 픽스처
"""
import os
import sys
import pytest
import pandas as pd
import tempfile
import shutil

# 프로젝트 루트를 Python Path에 추가
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


@pytest.fixture
def sample_dataframe():
    """테스트용 샘플 DataFrame"""
    return pd.DataFrame({
        "id": [1, 2, 3, 4, 5],
        "name": ["Alice", "Bob", "Charlie", "David", "Eve"],
        "age": [25, 30, 35, 28, 32],
        "salary": [50000.0, 60000.0, 75000.0, 55000.0, 80000.0],
        "is_active": [True, False, True, True, False]
    })


@pytest.fixture
def sample_csv_file(sample_dataframe, tmp_path):
    """테스트용 CSV 파일"""
    csv_path = tmp_path / "test_data.csv"
    sample_dataframe.to_csv(csv_path, index=False)
    return csv_path


@pytest.fixture
def multi_table_data():
    """다중 테이블 테스트용 데이터"""
    users = pd.DataFrame({
        "id": [1, 2, 3],
        "name": ["Alice", "Bob", "Charlie"],
        "email": ["alice@test.com", "bob@test.com", "charlie@test.com"]
    })
    
    orders = pd.DataFrame({
        "id": [1, 2, 3, 4, 5],
        "user_id": [1, 1, 2, 3, 2],
        "amount": [100.0, 250.0, 150.0, 300.0, 75.0],
        "product": ["A", "B", "A", "C", "B"]
    })
    
    return {
        "tables": {"users": users, "orders": orders},
        "relationships": [
            {
                "parent_table": "users",
                "child_table": "orders",
                "parent_key": "id",
                "child_key": "user_id"
            }
        ]
    }


@pytest.fixture
def temp_models_dir(tmp_path):
    """임시 모델 디렉토리"""
    models_dir = tmp_path / "models"
    models_dir.mkdir()
    return models_dir


@pytest.fixture(autouse=True)
def clean_temp_files(tmp_path):
    """테스트 후 임시 파일 정리"""
    yield
    # 테스트 후 정리
    if tmp_path.exists():
        shutil.rmtree(tmp_path, ignore_errors=True)
