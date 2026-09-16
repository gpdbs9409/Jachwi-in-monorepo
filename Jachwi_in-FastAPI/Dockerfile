FROM python:3.11-slim

WORKDIR /app

# 의존성 먼저 복사 (레이어 캐시 활용)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 모델 사전 다운로드 (빌드 시 한 번만)
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('jhgan/ko-sroberta-multitask')"

COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
