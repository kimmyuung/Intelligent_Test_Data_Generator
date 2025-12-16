from fastapi import APIRouter, File, UploadFile, HTTPException
from app.services import file_manager, stat_extractor
import pandas as pd
import os
import shutil

router = APIRouter()

@router.post("/analyze")
async def analyze_sample(
    file: UploadFile = File(...)
):
    # 1. Validation Logic
    filename = file.filename
    ext = os.path.splitext(filename)[1].lower()
    
    allowed_exts = [".csv", ".json", ".xls", ".xlsx"]
    if ext not in allowed_exts:
        raise HTTPException(status_code=400, detail="Unsupported file extension only .csv, .json, .xls, .xlsx are allowed")
    
    # 2. Save File Temporarily
    content = await file.read()
    file_id, filepath = file_manager.save_temp_file(content, ext)
    
    try:
        # 3. Analyze Data
        df = None
        if ext == ".csv":
            df = pd.read_csv(filepath)
        elif ext == ".json":
            df = pd.read_json(filepath)
        elif ext in [".xls", ".xlsx"]:
            df = pd.read_excel(filepath)
            
        if df is None:
             raise HTTPException(status_code=400, detail="Failed to parse file")

        stats = stat_extractor.extract_statistics(df)
        
        return {
            "success": True,
            "fileId": file_id,
            "filename": filename,
            "rows": int(len(df)),
            "stats": stats,
            "message": "Analysis successful. File will be auto-deleted in 30 minutes."
        }
        
    except Exception as e:
        # If analysis fails, delete immediately? Or keep for debug?
        # User wants cleanup, so let's delete on error to be safe.
        file_manager.delete_file(file_id)
        raise HTTPException(status_code=500, detail=str(e))

@router.delete("/cleanup/{file_id}")
def cleanup_file(file_id: str):
    success = file_manager.delete_file(file_id)
    if success:
        return {"success": True, "message": "File deleted"}
    else:
        raise HTTPException(status_code=404, detail="File not found or already deleted")
