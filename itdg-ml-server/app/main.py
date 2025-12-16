from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import uvicorn
import threading
import time
import os
import shutil

from app.api.v1.endpoints import analysis
from app.services.file_manager import start_cleanup_task, stop_cleanup_task

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Create temp directory and start cleanup thread
    os.makedirs("temp", exist_ok=True)
    cleanup_thread = start_cleanup_task()
    yield
    # Shutdown: Stop cleanup thread
    stop_cleanup_task()
    # Optional: Clean up all temp files on shutdown?
    # shutil.rmtree("temp", ignore_errors=True)

app = FastAPI(
    title="ITDG ML Server",
    description="Data Analysis & Learning Service",
    version="1.0.0",
    lifespan=lifespan
)

# CORS Configuration
origins = [
    "http://localhost:3000", # React Frontend
    "http://localhost:8080", # Spring Backend (if needed)
    "*"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(analysis.router, prefix="/api/v1", tags=["analysis"])

@app.get("/health")
def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
