
# ============================================================
# BimariHunter - ML Classifier Module
# ============================================================
# Loads the trained TF-IDF vectorizer and Logistic Regression
# model, and exposes classify_text() for the API layer.
#
# Ensure you have trained the model first:
#   python -m app.train_model
# ============================================================

import os
import joblib

from app.preprocess import clean_text

# ── Resolve model paths ───────────────────────────────────
_MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "news_classifier.joblib")
_VEC_PATH   = os.path.join(os.path.dirname(__file__), "..", "tfidf_vectorizer.joblib")


def _load_artifacts():
    """Load model and vectorizer from disk with a clear error message."""
    if not os.path.exists(_MODEL_PATH) or not os.path.exists(_VEC_PATH):
        raise FileNotFoundError(
            "Model artifacts not found. "
            "Run  python -m app.train_model  first to generate them."
        )
    model      = joblib.load(_MODEL_PATH)
    vectorizer = joblib.load(_VEC_PATH)
    return model, vectorizer


# Load once at import time (lazy globals)
_model      = None
_vectorizer = None


def _ensure_loaded():
    """Lazy-load model and vectorizer on first use."""
    global _model, _vectorizer
    if _model is None or _vectorizer is None:
        _model, _vectorizer = _load_artifacts()


# ── Public API ────────────────────────────────────────────

def classify_text(text: str) -> dict:
    """
    Classify a raw text string into one of the 8 crisis categories.

    Args:
        text (str): Raw news article or social media post.

    Returns:
        dict: {
            "category":   str,   # predicted label
            "confidence": float  # probability of top class (0–1)
        }
    """
    _ensure_loaded()

    cleaned    = clean_text(text)
    vectorized = _vectorizer.transform([cleaned])

    prediction  = _model.predict(vectorized)[0]
    probability = float(_model.predict_proba(vectorized).max())

    return {
        "category":   prediction,
        "confidence": round(probability, 4)
    }


def classify_batch(texts: list[str]) -> list[dict]:
    """
    Classify a list of texts in one vectorizer pass (efficient batch).

    Args:
        texts (list[str]): List of raw input strings.

    Returns:
        list[dict]: One result dict per input text.
    """
    _ensure_loaded()

    cleaned    = [clean_text(t) for t in texts]
    vectorized = _vectorizer.transform(cleaned)

    predictions   = _model.predict(vectorized)
    probabilities = _model.predict_proba(vectorized).max(axis=1)

    return [
        {"category": pred, "confidence": round(float(prob), 4)}
        for pred, prob in zip(predictions, probabilities)
    ]
