# AI Coding Agent Instructions for BimariHaunter

## Project Overview

BimariHaunter is a **health crisis monitoring monorepo** containing:
- **Backend** (`bimari-haunter/backend`): FastAPI service that scrapes news/social media, runs NLP analysis, and publishes realtime reports.
- **Android** (`android/`): Mobile application (Kotlin/Gradle-based).

Focus your work on the backend unless explicitly directed to Android.

## Architecture & Data Flow

### Core Layers

**1. Scraper** (`app/scraper/`)
- `crawler.py`: Async HTTP+BeautifulSoup crawler for Pakistani news sites. Falls back to Playwright for JS-heavy pages.
- `facebook_client.py`: Integration with Facebook Graph API.
- `scheduler.py`: Triggers scraping jobs via cron-like schedules (stored in `NewsSource.schedule`).
- Data source configs stored in `news_sources` table as JSONB (e.g., `list_url`, `link_selector`, `use_playwright`).

**2. NLP Pipeline** (`app/nlp/`)
- `processor.py`: Orchestrates entity extraction (spaCy + Urdu rules), zero-shot classification, summarization, and severity scoring.
- Uses HuggingFace models: `facebook/bart-large-mnli` (classification), `facebook/bart-large-cnn` (summarization).
- Urdu-specific: `urdu_utils.py` provides entity extraction, normalization, and regex-based statistics extraction.
- Output: `ReportV1Insight` (structured summaries) and `ReportV2Full` (detailed analysis).

**3. Delivery Queue** (`app/publisher/`)
- `queue_processor.py`: Background asyncio task processing `DeliveryQueue` table every 5 seconds.
- Retries failed items (max retries configurable via `publisher.max_retry`).
- `formatter.py`: Converts DB records into payload schemas.
- `realtime.py`: Manages WebSocket connections and push delivery.

**4. API Routes** (`app/api/routes/`)
- `reports.py`: GET `/insights` (V1) and `/reports` (V2) with filtering by disease, location, severity.
- `jobs.py`: Trigger scraping/NLP jobs on-demand.
- `websocket.py`: Real-time client connections for live updates.
- `sources.py`: Manage news/social sources.

### Database

**Key Tables** (`app/database/models.py`):
- `news_sources` / `social_sources`: Config for each source.
- `raw_articles` / `raw_social_posts`: Scraped content.
- `report_v1_insight` / `report_v2_full`: Analysis output.
- `delivery_queue`: Items pending WebSocket delivery (status: queued/failed/delivered).

Uses SQLAlchemy 2.0 async ORM with `NullPool` for Cloud Run (cold-start friendly).

## Development Workflows

### Environment Setup

```bash
cd bimari-haunter/backend
pip install -r requirements.txt
# Install spaCy model (required for NLP)
python -m spacy download en_core_web_trf
```

### Local Testing

```bash
# Run all tests
pytest -q

# Run specific test (exclude live scraping)
pytest -m "not live" -q

# Run with asyncio debugging
pytest -xvs tests/test_nlp_utils.py
```

**Test Config** (`pytest.ini`):
- `asyncio_mode = auto` — auto-uses asyncio.
- Markers: `live` (real websites), `facebook` (requires `FACEBOOK_ACCESS_TOKEN`).

### Local Development Server

```bash
# Requires .env with DATABASE_URL, Facebook secrets, etc.
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Database Migrations

Uses **Alembic** with async migrations:

```bash
alembic revision --autogenerate -m "add new column"
alembic upgrade head
```

Migrations in: `app/database/migrations/`

### Building & Deployment

**Docker Build**:
```bash
docker build -t bimari_haunter:latest -f Dockerfile .
```

Pre-builds models (spaCy, HF transformer) to avoid runtime startup lag.

**Cloud Run Deployment** (PowerShell):
```powershell
cd scripts
.\deploy_cloud_run.ps1 -PROJECT_ID <YOUR_ID> -REGION asia-southeast1
```

Expects: `roles/run.admin`, `roles/cloudbuild.builds.editor`, `roles/storage.admin`.

## Key Patterns & Conventions

### Async/Concurrency

- **Everything is async**: Use `async`/`await`, not threads.
- Background job: `asyncio.create_task()` in FastAPI lifespan (see `main.py`).
- Crawler limits: `max_concurrent_scrapers` (config) prevents overload.

### Configuration

- Central `app/config.py` using `pydantic-settings`.
- Loads from `.env` file (case-insensitive).
- Key secrets: `facebook_app_id`, `facebook_app_secret`, `google_cloud_project` (for Secret Manager).

### Error Handling

- Uses `structlog` for JSON logging (structured context preservation).
- Retryable failures: captured in `delivery_queue` with `retry_count` and `status` tracking.
- API errors: raise `HTTPException` with appropriate status codes.

### Urdu Language Support

- **Not all NLP models handle Urdu well**. For Urdu text:
  - Use custom regex patterns in `urdu_utils.py` (safer than relying on spaCy).
  - Normalize via `normalize_urdu()` before processing.
  - Extraction fallback: keyword matching over entity extraction.

### Scraper Design

- Scrapers are **stateless** (created per job, discarded after).
- Config stored in DB (`news_sources.scrape_config`).
- Playwright used only when JS rendering needed (slower, set `use_playwright: true`).
- User-Agent header important for news sites (included by default).

## File Locations & Examples

| Task | File(s) | Example |
|------|---------|---------|
| Add scraper source | `models.py` + insert row | `NewsSource(name="Dawn", list_url="...", link_selector="article")` |
| Add API endpoint | `routes/*.py` | Mimic `reports.py` structure: router + Depends(get_db) + query builder |
| Modify NLP logic | `nlp/processor.py` | Edit `CANDIDATE_LABELS` or add extraction step before classification |
| Add migration | Alembic | `alembic revision --autogenerate -m "..."`; edit `migrations/versions/` |
| Add test | `tests/` | Follow pattern: `@pytest.mark.asyncio` + mock DB via fixtures |
| Update config | `.env` + `config.py` | Add field to `Settings` class, document in README |

## Common Gotchas

1. **Cold start delays**: Models loaded lazily in NLP pipeline; first request will be slow.
2. **WebSocket cleanup**: Ensure clients unregister properly; lingering connections prevent graceful shutdown.
3. **Urdu text encoding**: Always validate UTF-8; some sources provide mojibake. Normalize early.
4. **Database connection pooling**: Cloud Run uses `NullPool` (no persistent connections); don't rely on session state across requests.
5. **Playwright browser install**: Only included in Docker image. Local dev needs manual `playwright install chromium --with-deps`.

## References

- **README.md**: Quick-start, high-level overview.
- **DEPLOY.md**: Cloud Run deployment detailed walkthrough.
- **Dockerfile**: Build order and system dependencies.
- **classifier/classification.py**: Legacy TF-IDF classifier (standalone, not currently integrated).
