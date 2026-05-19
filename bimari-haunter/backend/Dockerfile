# ── Stage: Production Image ─────────────────────────────────
# Use the official Microsoft Playwright image based on Ubuntu Jammy (22.04)
# This prevents apt-get dependency issues with ttf-ubuntu-font-family
FROM mcr.microsoft.com/playwright/python:v1.41.0-jammy

# Note: We do not need to run apt-get for system deps because the playwright 
# base image already includes all required libraries for Chromium.

WORKDIR /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Download spaCy transformer model
RUN python -m spacy download en_core_web_trf

# Copy the models download script first to cache models
COPY scripts/download_models.py scripts/download_models.py

# Pre-download HuggingFace models during build using robust script
RUN python scripts/download_models.py

# Copy the rest of the application code
COPY . .

EXPOSE 8080

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
