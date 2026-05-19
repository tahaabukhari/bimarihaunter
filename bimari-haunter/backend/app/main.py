"""
FastAPI application entry-point.

Assembles all routers, starts background tasks (delivery queue
processor) on startup, and cancels them on shutdown.
"""

from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
from typing import AsyncGenerator

import structlog
from fastapi import FastAPI

from app.api.routes import jobs, reports, sources, websocket
from app.publisher.queue_processor import run_delivery_queue

logger = structlog.get_logger(__name__)

# Background task references
_background_tasks: list[asyncio.Task] = []


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Startup / shutdown lifecycle manager."""
    logger.info("starting_background_tasks")

    # Load news classifier once at server startup
    logger.info("loading_news_classifier_at_startup")
    try:
        from app.nlp.classifier import init_classifier
        init_classifier()
        logger.info("news_classifier_loaded_successfully")
    except Exception as e:
        logger.error("news_classifier_loading_failed", error=str(e))

    # Start delivery-queue processor
    task = asyncio.create_task(run_delivery_queue())
    _background_tasks.append(task)

    yield

    # Cancel background tasks
    logger.info("shutting_down_background_tasks")
    for task in _background_tasks:
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass
    _background_tasks.clear()
    logger.info("shutdown_complete")


app = FastAPI(
    title="BimariHaunter",
    description=(
        "Health / disease outbreak monitoring backend for Pakistan. "
        "Scrapes news & social media, processes with NLP, and pushes "
        "real-time reports to Android clients via WebSocket."
    ),
    version="0.1.0",
    lifespan=lifespan,
)

# ── Include routers ─────────────────────────────────────────

app.include_router(sources.router)
app.include_router(jobs.router)
app.include_router(reports.router)
app.include_router(websocket.router)


# ── Pydantic Request Schemas ────────────────────────────────

from pydantic import BaseModel, Field

class ClassifyRequest(BaseModel):
    text: str = Field(..., min_length=1, description="Text to classify")


# ── Classifier Endpoints ────────────────────────────────────

from app.nlp.classifier import classify_article, extract_matched_keywords

@app.post("/classify_news")
async def classify_news(payload: ClassifyRequest):
    """Classify a single text article into a crisis category."""
    result = classify_article(payload.text)
    return {
        "category": result["category"],
        "confidence": result["confidence"],
        "keywords_matched": extract_matched_keywords(payload.text, result["category"])
    }


@app.post("/classify_batch")
async def classify_batch(articles: list[ClassifyRequest]):
    """Classify a batch of articles in one call."""
    return [classify_article(a.text) for a in articles]


# ── Feed and Search Endpoints ───────────────────────────────

from typing import Optional
from app.services.firestore import get_firestore_client

@app.get("/feed")
async def get_feed(
    category: Optional[str] = None,
    page: int = 1,
    limit: int = 20,
    region: Optional[str] = None
):
    """Fetch sorted, paginated articles and social posts from Firestore."""
    fs = get_firestore_client()
    query = fs.collection("articles")
    
    if category:
        query = query.where("category", "==", category)
    if region:
        query = query.where("region", "==", region)
        
    # Retrieve all documents to perform sorting and pagination
    docs = query.order_by("timestamp", direction="DESCENDING").stream()
    
    start_idx = (page - 1) * limit
    end_idx = start_idx + limit
    paginated_docs = docs[start_idx:end_idx]
    
    results = []
    for d in paginated_docs:
        data = d.to_dict()
        if not data:
            continue
        
        conf = data.get("confidence", 0.0)
        # Apply severity badge rules
        if conf >= 0.85:
            severity = "HIGH"
        elif conf >= 0.65:
            severity = "MEDIUM"
        else:
            severity = "LOW"
            
        doc_res = dict(data)
        doc_res["severity"] = severity
        results.append(doc_res)
        
    return results


@app.get("/search")
async def search_articles(
    q: str,
    category: Optional[str] = None,
    page: int = 1,
    limit: int = 20
):
    """Search for articles matching a case-insensitive query in title or body."""
    fs = get_firestore_client()
    query = fs.collection("articles")
    
    if category:
        query = query.where("category", "==", category)
        
    docs = query.stream()
    
    q_low = q.lower()
    matched_docs = []
    
    for d in docs:
        data = d.to_dict()
        if not data:
            continue
        title = data.get("title", "") or ""
        body = data.get("body", "") or ""
        if q_low in title.lower() or q_low in body.lower():
            matched_docs.append(data)
            
    # Sort matched articles by timestamp descending
    matched_docs = sorted(
        matched_docs,
        key=lambda x: x.get("timestamp", ""),
        reverse=True
    )
    
    start_idx = (page - 1) * limit
    end_idx = start_idx + limit
    paginated = matched_docs[start_idx:end_idx]
    
    results = []
    for data in paginated:
        conf = data.get("confidence", 0.0)
        # Apply severity badge rules
        if conf >= 0.85:
            severity = "HIGH"
        elif conf >= 0.65:
            severity = "MEDIUM"
        else:
            severity = "LOW"
            
        doc_res = dict(data)
        doc_res["severity"] = severity
        results.append(doc_res)
        
    return results


# ── Health check ────────────────────────────────────────────


@app.get("/health")
async def health_check():
    return {
        "status": "ok",
        "service": "bimari-haunter",
    }
