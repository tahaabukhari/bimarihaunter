# Scraper to Firestore Migration Plan

## Current State Analysis
I reviewed the scraper code (`app/scraper/scheduler.py`) to confirm if it is posting to Firebase. **It is currently NOT posting to Firebase.**

When we previously set up the `/feed` API and Firebase Auth to "just link things up", we intentionally left the scraper untouched to avoid breaking the application before deployment. The scraper is still hardcoded to use PostgreSQL (`async_session`, `pg_insert`, and `SQLAlchemy` ORM models).

To make the scraper post real-time data to Firebase, we need to completely rip out the PostgreSQL database dependencies from the scraping pipeline and replace them with the Google Cloud Firestore SDK.

## Proposed Changes

### 1. Refactor `app/scraper/scheduler.py`
We will replace all SQLAlchemy usage with our new `firestore.db` client. 
- **Fetching Sources**: Instead of doing `select(NewsSource)`, we will do `db.collection('news_sources').where('is_active', '==', True).stream()`.
- **Storing Jobs**: We will create documents in a `/scrape_jobs` collection.
- **Storing Articles (News)**: When `NewsCrawler` finds an article, we will generate a document ID using the `content_hash` (to prevent duplicates) and `set()` it in a `/reports` collection.
- **Storing Posts (Social)**: When `FacebookScraper` finds a post, we will do the same for social posts.

### 2. Update Database Seeding 
Since the scraper pulls the list of websites to scrape from the database, we need to manually insert a few initial `news_sources` into your new Firestore database so the scraper has something to actually do!
- We will create a `scripts/seed_firestore.py` script that adds default sources (e.g., Dawn News, Geo News) into Firestore.

### 3. Cleanup PostgreSQL Dependencies
Once the scraper and API routes are fully migrated to Firestore, we can safely delete `app/database/models.py`, `app/database/engine.py`, and remove `sqlalchemy`, `asyncpg`, and `alembic` from `requirements.txt`.

## User Review Required

> [!WARNING]  
> This refactor will permanently disconnect the scraper from the old PostgreSQL database and point it directly at your new Firebase project.

## Open Questions

1. **Firestore Indexes**: The scraper relies heavily on querying `is_active == True`. In Firestore, this might require building a composite index depending on how we sort. Are you okay with configuring indexes in the Firebase Console if prompted?
2. **Execution**: Shall I proceed with refactoring `scheduler.py` and creating the Firestore seeding script?
