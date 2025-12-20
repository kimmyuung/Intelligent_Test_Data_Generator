"""
합성 데이터 학습 및 생성 API 엔드포인트
"""

from fastapi import APIRouter, HTTPException, Query
from app.services import file_manager
from app.services.synthesizer import synthesizer
import pandas as pd
import os

router = APIRouter()


@router.post("/train")
async def train_model(
    file_id: str = Query(..., description="분석된 파일의 UUID"),
    model_type: str = Query("copula", description="모델 타입: copula 또는 ctgan")
):
    """
    업로드된 샘플 데이터로 SDV 모델을 학습합니다.
    
    - **file_id**: /api/v1/analyze에서 받은 fileId
    - **model_type**: "copula" (빠름, 기본값) 또는 "ctgan" (정교함, 느림)
    """
    # 1. 파일 경로 조회
    filepath = file_manager.get_file_path(file_id)
    if not filepath:
        raise HTTPException(status_code=404, detail=f"File not found: {file_id}")
    
    # 2. 파일 로드
    try:
        ext = os.path.splitext(filepath)[1].lower()
        df = None
        if ext == ".csv":
            df = pd.read_csv(filepath)
        elif ext == ".json":
            df = pd.read_json(filepath)
        elif ext in [".xls", ".xlsx"]:
            df = pd.read_excel(filepath)
        
        if df is None or df.empty:
            raise HTTPException(status_code=400, detail="Failed to load file or file is empty")
            
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to read file: {str(e)}")
    
    # 3. 모델 학습
    try:
        if model_type not in ["copula", "ctgan"]:
            raise HTTPException(status_code=400, detail="model_type must be 'copula' or 'ctgan'")
        
        result = synthesizer.train(df, model_type)
        return result
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Training failed: {str(e)}")


@router.post("/generate/{model_id}")
async def generate_data(
    model_id: str,
    num_rows: int = Query(100, ge=1, le=100000, description="생성할 행 수 (1-100000)")
):
    """
    학습된 모델로 합성 데이터를 생성합니다.
    
    - **model_id**: /api/v1/train에서 받은 modelId
    - **num_rows**: 생성할 데이터 행 수 (기본값: 100, 최대: 100000)
    """
    try:
        result = synthesizer.generate(model_id, num_rows)
        return result
        
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail=f"Model not found: {model_id}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Generation failed: {str(e)}")


@router.get("/model/{model_id}")
async def get_model_status(model_id: str):
    """
    모델의 상태 정보를 조회합니다.
    
    - **model_id**: 조회할 모델 UUID
    """
    info = synthesizer.get_model_info(model_id)
    if not info:
        raise HTTPException(status_code=404, detail=f"Model not found: {model_id}")
    
    return {
        "success": True,
        **info
    }


@router.delete("/model/{model_id}")
async def delete_model(model_id: str):
    """
    학습된 모델을 삭제합니다.
    
    - **model_id**: 삭제할 모델 UUID
    """
    success = synthesizer.delete_model(model_id)
    if success:
        return {"success": True, "message": f"Model {model_id} deleted"}
    else:
        raise HTTPException(status_code=404, detail=f"Model not found: {model_id}")


# ========================================
# 다중 테이블 학습 API (Multi-Table Learning)
# ========================================

from app.services.synthesizer import multi_table_synthesizer
from pydantic import BaseModel
from typing import List, Dict, Any

class TableData(BaseModel):
    name: str
    data: List[Dict[str, Any]]

class Relationship(BaseModel):
    parent_table: str
    child_table: str
    parent_key: str
    child_key: str

class MultiTableTrainRequest(BaseModel):
    tables: List[TableData]
    relationships: List[Relationship]


@router.post("/multi-table/train")
async def train_multi_table(request: MultiTableTrainRequest):
    """
    다중 테이블을 동시에 학습합니다 (HMASynthesizer).
    
    테이블 간 Foreign Key 관계를 학습하여 참조 무결성을 유지하는
    합성 데이터를 생성할 수 있습니다.
    
    - **tables**: 학습할 테이블 데이터 목록
    - **relationships**: 테이블 간 관계 정의
    
    Example:
    ```json
    {
        "tables": [
            {"name": "users", "data": [{"id": 1, "name": "John"}, ...]},
            {"name": "orders", "data": [{"id": 1, "user_id": 1, "amount": 100}, ...]}
        ],
        "relationships": [
            {"parent_table": "users", "child_table": "orders", "parent_key": "id", "child_key": "user_id"}
        ]
    }
    ```
    """
    try:
        import pandas as pd
        
        # 테이블 데이터를 DataFrame으로 변환
        tables_dict = {}
        for table in request.tables:
            if table.data:
                tables_dict[table.name] = pd.DataFrame(table.data)
        
        if len(tables_dict) < 2:
            raise HTTPException(status_code=400, detail="At least 2 tables are required for multi-table learning")
        
        # 관계 정의 변환
        relationships = [rel.dict() for rel in request.relationships]
        
        if not relationships:
            raise HTTPException(status_code=400, detail="At least 1 relationship is required")
        
        # 학습 수행
        result = multi_table_synthesizer.train(tables_dict, relationships)
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Multi-table training failed: {str(e)}")


@router.post("/multi-table/generate/{model_id}")
async def generate_multi_table(
    model_id: str,
    scale: float = Query(1.0, ge=0.1, le=10.0, description="원본 대비 생성 비율 (0.1-10.0)")
):
    """
    학습된 다중 테이블 모델로 합성 데이터를 생성합니다.
    
    - **model_id**: /api/v1/multi-table/train에서 받은 modelId
    - **scale**: 원본 대비 생성 비율 (기본값: 1.0 = 동일 크기)
    """
    try:
        result = multi_table_synthesizer.generate(model_id, scale)
        return result
        
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail=f"Model not found: {model_id}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Multi-table generation failed: {str(e)}")

