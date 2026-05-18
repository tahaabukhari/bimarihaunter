
# ============================================================
# BimariHunter - FastAPI Application Entry Point
# ============================================================
# Run with:
#   uvicorn app.main:app --reload
# ============================================================

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional

from app.classifier import classify_text, classify_batch
from app.keyword_filter import keyword_match, best_keyword_category

# ── App Initialization ────────────────────────────────────
app = FastAPI(
    title="BimariHunter - Crisis Text Classifier",
    description=(
        "AI-powered system that classifies news articles and social media posts "
        "into crisis categories using keyword filtering + ML classification."
    ),
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Request / Response Schemas ────────────────────────────

class NewsInput(BaseModel):
    text: str = Field(..., min_length=5, description="Raw news or social media text to classify")

class BatchInput(BaseModel):
    texts: list[str] = Field(..., min_items=1, max_items=100, description="List of texts to classify")

class ClassificationResult(BaseModel):
    input_text:          str
    keyword_matches:     dict
    best_keyword_match:  Optional[str]
    prediction:          str
    confidence:          float

class BatchResult(BaseModel):
    results: list[ClassificationResult]

# ── Health Check ──────────────────────────────────────────

@app.get("/", tags=["Health"])
def root():
    return {"status": "ok", "service": "BimariHunter Crisis Classifier"}

@app.get("/health", tags=["Health"])
def health():
    return {"status": "healthy"}

# ── Single Classification ─────────────────────────────────

@app.post("/classify_news", response_model=ClassificationResult, tags=["Classification"])
def classify_news(data: NewsInput):
    """
    Classify a single news article or social media post.

    Returns:
    - **keyword_matches**: Rule-based category scores (fast filter)
    - **best_keyword_match**: Top rule-based category
    - **prediction**: ML model category prediction
    - **confidence**: ML model confidence (0–1)
    """
    try:
        keyword_results = keyword_match(data.text)
        ml_result       = classify_text(data.text)
    except FileNotFoundError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Classification error: {e}")

    return ClassificationResult(
        input_text         = data.text,
        keyword_matches    = keyword_results,
        best_keyword_match = best_keyword_category(data.text),
        prediction         = ml_result["category"],
        confidence         = ml_result["confidence"],
    )

# ── Batch Classification ──────────────────────────────────

@app.post("/classify_batch", response_model=BatchResult, tags=["Classification"])
def classify_news_batch(data: BatchInput):
    """
    Classify up to 100 texts in a single efficient request.
    """
    try:
        ml_results = classify_batch(data.texts)
    except FileNotFoundError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Batch classification error: {e}")

    results = [
        ClassificationResult(
            input_text         = text,
            keyword_matches    = keyword_match(text),
            best_keyword_match = best_keyword_category(text),
            prediction         = ml["category"],
            confidence         = ml["confidence"],
        )
        for text, ml in zip(data.texts, ml_results)
    ]

    return BatchResult(results=results)

# ── List Categories ───────────────────────────────────────

@app.get("/categories", tags=["Info"])
def list_categories():
    """Return all supported crisis categories."""
    from app.categories import CATEGORIES
    return {"categories": list(CATEGORIES.keys()), "total": len(CATEGORIES)}
