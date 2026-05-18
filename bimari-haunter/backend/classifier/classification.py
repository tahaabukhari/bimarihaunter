
# ==============================================================
# BIMARIHUNTER
# ==============================================================
#
#   BIMARIHUNTER - Consolidated Classifier (All-in-One File)
#   =========================================================
#   This single file contains ALL modules of the BimariHunter
#   crisis text classification system:
#
#     Section 1 │ CATEGORIES     – Crisis category definitions & keywords
#     Section 2 │ PREPROCESS     – Text cleaning utilities
#     Section 3 │ KEYWORD FILTER – Rule-based keyword matcher
#     Section 4 │ TRAIN MODEL    – TF-IDF + Logistic Regression trainer
#     Section 5 │ CLASSIFIER     – ML inference wrapper
#     Section 6 │ FASTAPI APP    – REST API endpoints
#     Section 7 │ DEMO / MAIN    – CLI demo & training entrypoint
#
#   Usage:
#     Train model  ->  python classification.py --train
#     Run demo     ->  python classification.py --demo
#     Start API    ->  uvicorn classification:app --reload
#
# ==============================================================


# ==============================================================
# SECTION 1 │ CATEGORIES
# ==============================================================

CATEGORIES = {
    "Disease Outbreak": [
        "disease", "outbreak", "epidemic", "pandemic",
        "virus", "infection", "quarantine", "vaccine",
        "contagious", "pathogen", "symptoms", "fever",
        "transmission", "immunization", "mortality"
    ],
    "Natural Disaster": [
        "flood", "earthquake", "wildfire",
        "storm", "cyclone", "tsunami",
        "hurricane", "tornado", "drought",
        "avalanche", "landslide", "volcanic"
    ],
    "Infrastructure Failure": [
        "power outage", "bridge collapse",
        "internet outage", "water supply",
        "blackout", "grid failure", "pipeline",
        "system failure", "network down", "sewage"
    ],
    "Accidents/Emergencies": [
        "accident", "explosion", "fire",
        "collision", "chemical spill",
        "crash", "blast", "emergency",
        "hazardous", "toxic leak", "evacuation"
    ],
    "Community Response/Aid": [
        "volunteers", "donations",
        "relief efforts", "shelter",
        "rescue", "aid workers", "charity",
        "fundraising", "support center", "humanitarian"
    ],
    "Prevention/Safety Tips": [
        "prevention", "guidelines",
        "precautions", "first aid",
        "safety tips", "awareness", "hygiene",
        "protective gear", "mask", "sanitize",
        "social distancing", "lockdown advice"
    ],
    "Pharmacy/Cost Updates": [
        "drug prices", "medication",
        "shortage", "pharmacy",
        "medicine cost", "stock", "supply chain",
        "pharmaceutical", "prescription", "healthcare cost"
    ],
    "General News": [
        "government announcement",
        "community event",
        "press release", "official statement",
        "policy update", "public notice"
    ]
}


# ==============================================================
# SECTION 2 │ PREPROCESS
# ==============================================================

import re
import nltk
from nltk.corpus import stopwords

nltk.download("stopwords", quiet=True)
nltk.download("punkt", quiet=True)

_stop_words = set(stopwords.words("english"))


def clean_text(text: str) -> str:
    """
    Preprocess raw text for ML classification.

    Steps:
      1. Lowercase
      2. Remove URLs
      3. Remove non-alphabetic characters
      4. Strip English stopwords

    Args:
        text (str): Raw input string.

    Returns:
        str: Cleaned token string.
    """
    if not isinstance(text, str):
        return ""

    text = text.lower()
    text = re.sub(r"http\S+|www\.\S+", "", text)
    text = re.sub(r"[^a-zA-Z ]", "", text)

    words = text.split()
    words = [w for w in words if w not in _stop_words and len(w) > 1]

    return " ".join(words)


# ==============================================================
# SECTION 3 │ KEYWORD FILTER
# ==============================================================

def keyword_match(text: str) -> dict:
    """
    Scan text for keyword matches across all crisis categories.

    Args:
        text (str): Raw input text.

    Returns:
        dict: {category: match_count} for categories with ≥1 match.
    """
    lowered = text.lower()
    matches = {}

    for category, keywords in CATEGORIES.items():
        count = sum(1 for keyword in keywords if keyword in lowered)
        if count > 0:
            matches[category] = count

    return matches


def best_keyword_category(text: str):
    """Return the top keyword-matched category or None."""
    matches = keyword_match(text)
    return max(matches, key=matches.get) if matches else None


# ==============================================================
# SECTION 4 │ TRAIN MODEL
# ==============================================================

import os
import pandas as pd
import joblib
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report

# Paths (relative to this file's location)
_BASE_DIR  = os.path.dirname(os.path.abspath(__file__))
_DATA_PATH = os.path.join(_BASE_DIR, "data", "dataset.csv")
_MODEL_DIR = os.path.join(_BASE_DIR, "app", "model")
_MODEL_PATH = os.path.join(_MODEL_DIR, "classifier.pkl")
_VEC_PATH   = os.path.join(_MODEL_DIR, "vectorizer.pkl")


def train_model(dataset_path: str = _DATA_PATH) -> None:
    """
    Train TF-IDF + Logistic Regression model and save artifacts.

    Args:
        dataset_path (str): Path to the labeled CSV file.
    """
    os.makedirs(_MODEL_DIR, exist_ok=True)

    print("[1/5] Loading dataset ...")
    df = pd.read_csv(dataset_path)
    print(f"      {len(df)} samples | {df['label'].nunique()} categories")

    print("[2/5] Cleaning text ...")
    df["clean_text"] = df["text"].apply(clean_text)

    X = df["clean_text"]
    y = df["label"]

    print("[3/5] Vectorizing with TF-IDF ...")
    vectorizer = TfidfVectorizer(
        max_features=5000,
        ngram_range=(1, 2),
        sublinear_tf=True
    )
    X_vec = vectorizer.fit_transform(X)

    X_train, X_test, y_train, y_test = train_test_split(
        X_vec, y, test_size=0.2, random_state=42, stratify=y
    )

    print("[4/5] Training Logistic Regression ...")
    model = LogisticRegression(
        max_iter=1000,
        C=1.0,
        solver="lbfgs",
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    print("\n-- Classification Report ------------------------------------------")
    print(classification_report(y_test, y_pred, zero_division=0))

    print("[5/5] Saving artifacts ...")
    joblib.dump(model, _MODEL_PATH)
    joblib.dump(vectorizer, _VEC_PATH)
    print(f"      Model      -> {_MODEL_PATH}")
    print(f"      Vectorizer -> {_VEC_PATH}")
    print("\n[OK] Training complete!\n")


# ==============================================================
# SECTION 5 │ CLASSIFIER (ML Inference)
# ==============================================================

# Lazy-loaded globals
_model      = None
_vectorizer = None


def _ensure_model_loaded():
    """Load model artifacts from disk on first call."""
    global _model, _vectorizer
    if _model is None or _vectorizer is None:
        if not os.path.exists(_MODEL_PATH) or not os.path.exists(_VEC_PATH):
            raise FileNotFoundError(
                "Model artifacts not found. "
                "Run:  python classification.py --train"
            )
        _model      = joblib.load(_MODEL_PATH)
        _vectorizer = joblib.load(_VEC_PATH)


def classify_text(text: str) -> dict:
    """
    Classify a single text using the trained ML model.

    Args:
        text (str): Raw news or social media text.

    Returns:
        dict: {"category": str, "confidence": float}
    """
    _ensure_model_loaded()

    cleaned    = clean_text(text)
    vectorized = _vectorizer.transform([cleaned])

    prediction  = _model.predict(vectorized)[0]
    probability = float(_model.predict_proba(vectorized).max())

    return {
        "category":   prediction,
        "confidence": round(probability, 4)
    }


def classify_batch(texts: list) -> list:
    """
    Classify multiple texts in one efficient vectorizer pass.

    Args:
        texts (list[str]): List of raw input strings.

    Returns:
        list[dict]: One result dict per input.
    """
    _ensure_model_loaded()

    cleaned    = [clean_text(t) for t in texts]
    vectorized = _vectorizer.transform(cleaned)

    predictions   = _model.predict(vectorized)
    probabilities = _model.predict_proba(vectorized).max(axis=1)

    return [
        {"category": pred, "confidence": round(float(prob), 4)}
        for pred, prob in zip(predictions, probabilities)
    ]


# ==============================================================
# SECTION 6 │ FASTAPI APPLICATION
# ==============================================================

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, List

app = FastAPI(
    title="BimariHunter – Crisis Text Classifier",
    description=(
        "AI-powered system that classifies news and social media posts "
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


# ── Schemas ──────────────────────────────────────────────────

class NewsInput(BaseModel):
    text: str = Field(..., min_length=5, description="Raw text to classify")

class BatchInput(BaseModel):
    texts: List[str] = Field(..., description="List of texts (max 100)")

class ClassificationResult(BaseModel):
    input_text:          str
    keyword_matches:     dict
    best_keyword_match:  Optional[str]
    prediction:          str
    confidence:          float


# ── Endpoints ─────────────────────────────────────────────────

@app.get("/", tags=["Health"])
def root():
    """Health check."""
    return {"status": "ok", "service": "BimariHunter Crisis Classifier"}


@app.get("/health", tags=["Health"])
def health():
    return {"status": "healthy"}


@app.get("/categories", tags=["Info"])
def list_categories():
    """List all supported crisis categories."""
    return {"categories": list(CATEGORIES.keys()), "total": len(CATEGORIES)}


@app.post("/classify_news", response_model=ClassificationResult, tags=["Classification"])
def classify_news(data: NewsInput):
    """
    Classify a single news article or social media post.

    Returns keyword-based and ML-based predictions with confidence score.
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


@app.post("/classify_batch", tags=["Classification"])
def classify_news_batch(data: BatchInput):
    """
    Classify up to 100 texts in a single efficient request.
    """
    if len(data.texts) > 100:
        raise HTTPException(status_code=400, detail="Maximum 100 texts per batch.")
    try:
        ml_results = classify_batch(data.texts)
    except FileNotFoundError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Batch error: {e}")

    return {
        "results": [
            {
                "input_text":         text,
                "keyword_matches":    keyword_match(text),
                "best_keyword_match": best_keyword_category(text),
                "prediction":         ml["category"],
                "confidence":         ml["confidence"],
            }
            for text, ml in zip(data.texts, ml_results)
        ]
    }


# ==============================================================
# SECTION 7 │ CLI DEMO / ENTRYPOINT
# ==============================================================

DEMO_TEXTS = [
    "Massive flood destroys hundreds of homes near the river bank",
    "New virus spreading rapidly in the capital city",
    "Power outage affects main hospital for six hours",
    "Explosion reported at chemical factory downtown",
    "Volunteers helping flood victims in relief camps",
    "Health officials issue new COVID prevention guidelines",
    "Drug prices spike as pharmacy shelves run empty",
    "Government announces new policy for disaster relief",
]


def run_demo():
    """Run a quick classification demo on sample texts."""
    print("=" * 60)
    print("  BimariHunter – Classification Demo")
    print("=" * 60)

    try:
        results = classify_batch(DEMO_TEXTS)
    except FileNotFoundError:
        print("\n[!] Model not found. Training now...\n")
        train_model()
        results = classify_batch(DEMO_TEXTS)

    for text, result in zip(DEMO_TEXTS, results):
        kw = best_keyword_category(text)
        print(f"\n[TEXT]     {text}")
        print(f"[KEYWORD]  {kw or 'No match'}")
        print(f"[ML]       {result['category']}  (conf: {result['confidence']:.2%})")

    print("\n" + "=" * 60)
    print("[OK] Demo complete.")


if __name__ == "__main__":
    import sys

    if "--train" in sys.argv:
        train_model()
    elif "--demo" in sys.argv:
        run_demo()
    else:
        print(__doc__ or "")
        print("Usage:")
        print("  python classification.py --train   -> Train the ML model")
        print("  python classification.py --demo    -> Run classification demo")
        print("  uvicorn classification:app --reload -> Start API server")
