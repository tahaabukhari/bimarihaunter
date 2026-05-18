# ── Stage: Production Image ─────────────────────────────────
FROM python:3.11-slim

# System deps for Playwright, psycopg, lxml
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    libnss3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libxcomposite1 \
    libxrandr2 \
    libxdamage1 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
    libxshmfence1 \
    fonts-liberation \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Install Playwright browsers (Chromium only for scraping)
RUN playwright install chromium --with-deps

# Download spaCy transformer model
RUN python -m spacy download en_core_web_trf

# Pre-download HuggingFace models during build
RUN python -c "\
from transformers import pipeline; \
pipeline('zero-shot-classification', model='facebook/bart-large-mnli'); \
pipeline('summarization', model='facebook/bart-large-cnn'); \
print('Models cached successfully')"

# Copy application code
COPY . .

EXPOSE 8080

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
