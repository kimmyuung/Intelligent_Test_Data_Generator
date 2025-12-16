import os
import time
import shutil
import uuid
import threading
import logging

logger = logging.getLogger(__name__)

TEMP_DIR = "temp"
CLEANUP_INTERVAL = 60 # Check every 60 seconds
FILE_TTL = 30 * 60 # 30 minutes in seconds

_stop_event = threading.Event()

def start_cleanup_task():
    t = threading.Thread(target=_cleanup_loop, daemon=True)
    t.start()
    logger.info("Cleanup background task started")
    return t

def stop_cleanup_task():
    _stop_event.set()

def _cleanup_loop():
    while not _stop_event.is_set():
        try:
            now = time.time()
            if os.path.exists(TEMP_DIR):
                for filename in os.listdir(TEMP_DIR):
                    filepath = os.path.join(TEMP_DIR, filename)
                    if os.path.isfile(filepath):
                        # Check modification time
                        mtime = os.path.getmtime(filepath)
                        if now - mtime > FILE_TTL:
                            try:
                                os.remove(filepath)
                                logger.info(f"Auto-deleted orphaned file: {filename}")
                            except Exception as e:
                                logger.error(f"Failed to delete file {filename}: {e}")
        except Exception as e:
            logger.error(f"Error in cleanup loop: {e}")
        
        time.sleep(CLEANUP_INTERVAL)

def save_temp_file(file_content: bytes, extension: str) -> str:
    if not os.path.exists(TEMP_DIR):
        os.makedirs(TEMP_DIR)
    
    file_id = str(uuid.uuid4())
    filename = f"{file_id}{extension}"
    filepath = os.path.join(TEMP_DIR, filename)
    
    with open(filepath, "wb") as f:
        f.write(file_content)
        
    return file_id, filepath

def get_file_path(file_id: str) -> str:
    # Scan dir to find file with this ID (ignoring extension)
    if os.path.exists(TEMP_DIR):
        for filename in os.listdir(TEMP_DIR):
            if filename.startswith(file_id):
                return os.path.join(TEMP_DIR, filename)
    return None

def delete_file(file_id: str):
    path = get_file_path(file_id)
    if path and os.path.exists(path):
        os.remove(path)
        logger.info(f"Explicitly deleted file: {file_id}")
        return True
    return False
