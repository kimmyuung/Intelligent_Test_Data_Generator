# Python ML 서버용 Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 시스템 패키지 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 보안: 비루트 사용자 생성
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash appuser

# 모델 저장 디렉토리 생성
RUN mkdir -p /app/models /app/uploads && \
    chown -R appuser:appgroup /app

# 의존성 먼저 설치 (캐시 활용)
COPY itdg-ml-server/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 복사
COPY --chown=appuser:appgroup itdg-ml-server/app app

USER appuser

# 환경 변수
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV MODELS_DIR=/app/models
ENV MODEL_TTL=3600
ENV CLEANUP_INTERVAL=300

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
