# BimariHaunter (Monorepo)

This repository contains two top-level projects:

- `bimari-haunter/backend` — FastAPI backend that scrapes news/social media, runs NLP, and publishes realtime reports.
- `android/` — Android application (placeholder). Add your Android Studio project here.

Quick start (backend):

1. cd `bimari-haunter/backend`
2. Create a `.env` from `.env.example` and set environment variables.
3. Install dependencies: `pip install -r requirements.txt`
4. Run tests: `pytest -q`
5. Run locally: `uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`

Deployment:
- Backend is configured for Docker or direct `uvicorn` run. See `bimari-haunter/backend/Dockerfile` and Railway guide in repository issues.
- Android builds are handled in CI; place the Android Studio project in `/android`.

Contributing:
- Use path-based CI workflows. Backend changes should be made under `bimari-haunter/backend` and Android changes under `android/`.
