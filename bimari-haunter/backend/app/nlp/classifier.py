import os
import joblib

# Global references to model and vectorizer for reuse
model = None
vectorizer = None

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

def init_classifier():
    """Load model and vectorizer once at startup."""
    global model, vectorizer
    if model is None or vectorizer is None:
        # Resolve absolute paths to the joblib files
        base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        model_path = os.path.join(base_dir, "classifier", "news_classifier.joblib")
        vec_path = os.path.join(base_dir, "classifier", "tfidf_vectorizer.joblib")
        
        # Fallback to relative path if absolute fails
        if not os.path.exists(model_path):
            model_path = "classifier/news_classifier.joblib"
            vec_path = "classifier/tfidf_vectorizer.joblib"
            
        model = joblib.load(model_path)
        vectorizer = joblib.load(vec_path)

def classify_article(text: str) -> dict:
    """Classify a single article text using preloaded model/vectorizer."""
    global model, vectorizer
    if model is None or vectorizer is None:
        init_classifier()
        
    vec = vectorizer.transform([text])
    label = model.predict(vec)[0]
    confidence = float(model.predict_proba(vec).max())
    return {"category": label, "confidence": confidence}

def extract_matched_keywords(text: str, category: str) -> list:
    """Extract keywords that matched for the given category in the text."""
    keywords = CATEGORIES.get(category, [])
    lowered = text.lower()
    return [kw for kw in keywords if kw in lowered]
