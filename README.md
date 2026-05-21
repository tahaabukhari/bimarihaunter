# BimariHaunter
> Realtime outbreak alerts for Pakistan — fast, localised, actionable.

<p style="text-align:center;margin:8px 0 18px 0;"><img src="android/ghosts/ghost_sleep.png" alt="ghost sleep" width="360" /></p>

- What: ingest news + social, classify outbreaks, notify nearby users.
- Why: quicker awareness → faster response.

Quick start:
1. Open `android/` in Android Studio and run.
2. Backend & infra: see `bimari-haunter/backend`.

Full docs, architecture, and assets live in `docs/app-doc/README.md`.

That's it — short, sharp, and ready for contributors.

## Stack & Workflows

- Core stack: Android (Jetpack Compose), Room (local cache), Google Maps, Firebase (Auth, Firestore, FCM), Backend: Python (FastAPI) on Cloud Run. NLP and classification run in the backend pipeline.
- Data flow: scraper → backend NLP pipeline → `reports` (Firestore) → Android app (real-time listener) → Room cache → proximity alerts.
- Development workflow:
	1. Android: open `android/` in Android Studio, run on device/emulator.
	2. Backend: develop in `bimari-haunter/backend/`; use the FastAPI dev server for local testing.
 3. Run tests:

```bash
# run Android unit/instrumented tests from Android Studio
# run backend tests
cd bimari-haunter/backend
pytest -q
```

- Deployment: backend builds via Docker and deploys to Cloud Run; migrations use Alembic for the DB where applicable.
- Notes: assets and mascot art live in `docs/app-doc/images/` and `android/ghosts/`.

Built 100% over antigravity with patience and perseverance. 👻 by team bitscare

