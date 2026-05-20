"""
Lightweight keyword-based disease classifier for BimariHaunter.

Replaces the 1.6GB facebook/bart-large-mnli transformer with a fast
rule-based approach. Runs in <5MB RAM — suitable for Cloud Run free tier.

Returns the same dict structure expected by the scheduler so no other
code needs to change.
"""
from __future__ import annotations
import re
from datetime import datetime, timezone
from typing import Any

# ── Disease keyword map ────────────────────────────────────
DISEASE_KEYWORDS: dict[str, list[str]] = {
    "dengue": ["dengue", "aedes aegypti", "breakbone fever", "dengue fever",
               "platelet count", "hemorrhagic fever", "dengue hemorrhagic"],
    "cholera": ["cholera", "vibrio cholerae", "watery diarrhea", "rice-water stool",
                "oral rehydration", "cholera outbreak"],
    "malaria": ["malaria", "plasmodium", "antimalarial", "chloroquine", "falciparum",
                "vivax", "malarial fever"],
    "typhoid": ["typhoid", "enteric fever", "salmonella typhi", "typhoid fever",
                "typhoid cases"],
    "covid": ["covid", "coronavirus", "sars-cov", "omicron", "delta variant",
              "pcr positive", "covid-19", "corona virus"],
    "polio": ["polio", "poliovirus", "paralysis", "oral polio vaccine", "opv",
              "poliomyelitis"],
    "measles": ["measles", "rubeola", "morbilli", "mmr vaccine", "measles outbreak"],
    "tuberculosis": ["tuberculosis", " tb ", "mycobacterium", "pulmonary tb",
                     "drug-resistant tb", "tb cases", "anti-tb"],
    "hepatitis": ["hepatitis", "jaundice", "liver disease", "hbsag", "hcv",
                  "hepatitis a", "hepatitis b", "hepatitis c", "hepatitis e"],
    "influenza": ["influenza", " flu ", "h1n1", "h3n2", "respiratory illness",
                  "seasonal flu", "flu outbreak", "flu cases"],
    "outbreak": ["outbreak", "epidemic", "cases reported", "health emergency",
                 "disease spread", "health alert", "disease outbreak",
                 "public health", "health crisis"],
}

# ── Severity keywords ──────────────────────────────────────
SEVERITY_HIGH: list[str] = [
    "critical", "emergency", "deaths", "fatalities", "epidemic",
    "rapid spread", "hundreds of cases", "thousands of cases",
    "surge", "crisis", "urgent", "alarming", "mass casualty",
    "death toll", "killed", "died",
]
SEVERITY_LOW: list[str] = [
    "mild", "minor", "contained", "isolated", "single case",
    "precautionary", "no deaths", "fully recovered",
]

# ── Pakistan city/region → coordinates ────────────────────
PAKISTAN_LOCATIONS: dict[str, tuple[float, float]] = {
    "karachi": (24.8607, 67.0011),
    "lahore": (31.5204, 74.3587),
    "islamabad": (33.6844, 73.0479),
    "rawalpindi": (33.5984, 73.0441),
    "peshawar": (34.0151, 71.5249),
    "quetta": (30.1798, 66.9750),
    "multan": (30.1575, 71.5249),
    "faisalabad": (31.4504, 73.1350),
    "hyderabad": (25.3960, 68.3578),
    "sialkot": (32.4925, 74.5310),
    "gujranwala": (32.1877, 74.1945),
    "abbottabad": (34.1463, 73.2117),
    "sukkur": (27.7052, 68.8574),
    "larkana": (27.5570, 68.2247),
    "gwadar": (25.1264, 62.3225),
    "sindh": (25.8920, 68.5247),
    "punjab": (31.1704, 72.7097),
    "kpk": (34.9526, 72.3311),
    "khyber pakhtunkhwa": (34.9526, 72.3311),
    "balochistan": (28.4907, 65.0958),
    "azad kashmir": (34.0837, 73.9040),
    "gilgit": (35.9208, 74.3087),
    "pakistan": (30.3753, 69.3451),
}

# ── Symptom keywords ───────────────────────────────────────
SYMPTOM_KEYWORDS: dict[str, list[str]] = {
    "fever": ["fever", "high temperature", "pyrexia", "febrile"],
    "cough": ["cough", "coughing", "dry cough", "wet cough"],
    "headache": ["headache", "head pain", "migraine"],
    "diarrhea": ["diarrhea", "diarrhoea", "loose stool", "watery stool"],
    "vomiting": ["vomiting", "nausea", "vomit"],
    "joint pain": ["joint pain", "muscle pain", "body ache", "myalgia", "arthralgia"],
    "rash": ["rash", "skin rash", "eruption", "spots on skin"],
    "fatigue": ["fatigue", "weakness", "lethargy", "tiredness"],
    "breathing difficulty": ["breathing difficulty", "shortness of breath", "dyspnea"],
    "jaundice": ["jaundice", "yellow skin", "yellow eyes"],
}


def classify_article(title: str, text: str) -> dict[str, Any] | None:
    """
    Classify an article for disease relevance.

    Returns None if the article is not health/disease related.
    Returns a result dict compatible with the scheduler's expected structure
    if the article is relevant.
    """
    combined = (" " + title + " " + text + " ").lower()

    # ── 1. Disease detection ───────────────────────────────
    matched_disease: str | None = None
    best_count = 0
    for disease, keywords in DISEASE_KEYWORDS.items():
        count = sum(1 for kw in keywords if kw in combined)
        if count > best_count:
            best_count = count
            matched_disease = disease

    if best_count == 0:
        return None  # Not health-related — skip article

    # ── 2. Severity determination ──────────────────────────
    severity_str = "medium"
    for kw in SEVERITY_HIGH:
        if kw in combined:
            severity_str = "high"
            break
    if severity_str == "medium":
        for kw in SEVERITY_LOW:
            if kw in combined:
                severity_str = "low"
                break

    # Map severity string to float score for scheduler compatibility
    severity_score = {"high": 0.8, "medium": 0.5, "low": 0.2}[severity_str]

    # ── 3. Location extraction ─────────────────────────────
    found_locations: list[str] = []
    found_coords: tuple[float, float] = PAKISTAN_LOCATIONS["pakistan"]
    for city, coords in PAKISTAN_LOCATIONS.items():
        if city in combined:
            found_locations.append(city.title())
            if found_coords == PAKISTAN_LOCATIONS["pakistan"]:
                found_coords = coords  # Use first matched city

    if not found_locations:
        found_locations = ["Pakistan"]

    # ── 4. Symptom extraction ──────────────────────────────
    found_symptoms: list[str] = []
    for symptom, keywords in SYMPTOM_KEYWORDS.items():
        if any(kw in combined for kw in keywords):
            found_symptoms.append(symptom.title())

    # ── 5. Affected count ──────────────────────────────────
    affected_match = re.search(
        r"(\d[\d,]*)\s*(?:people|persons|patients|cases|affected|confirmed|reported)",
        combined,
    )
    affected_count = (
        int(affected_match.group(1).replace(",", "")) if affected_match else 0
    )
    death_match = re.search(
        r"(\d[\d,]*)\s*(?:deaths?|died|fatalities|killed)",
        combined,
    )
    death_count = (
        int(death_match.group(1).replace(",", "")) if death_match else 0
    )

    # ── 6. Summary extraction ──────────────────────────────
    sentences = [
        s.strip()
        for s in re.split(r"[.!?]", text)
        if len(s.strip()) > 40
    ]
    summary_sentences = sentences[:3] if sentences else [title]

    # ── 7. Confidence score ────────────────────────────────
    confidence = min(0.45 + (best_count * 0.08), 0.95)

    # ── Return in scheduler-compatible format ──────────────
    return {
        "language": "en",
        "entities": {
            "diseases": [matched_disease.title()] if matched_disease else [],
            "symptoms": found_symptoms,
            "locations": found_locations,
            "organizations": [],
            "people": [],
            "dates": [],
        },
        "classification": {
            "label": "disease outbreak alert",
            "score": confidence,
            "labels": ["disease outbreak alert"],
            "scores": [confidence],
        },
        "summary": ". ".join(summary_sentences),
        "statistics": {
            "affected": affected_count,
            "deaths": death_count,
            "recovered": 0,
        },
        "severity": severity_score,
        "processing_time_ms": 1,
        "model_metadata": {
            "ner": "keyword-rules-v1",
            "classifier": "keyword-classifier-v1",
            "summarizer": "sentence-extractor-v1",
        },
        # Extra fields used by map/insights
        "_disease": matched_disease or "outbreak",
        "_severity_str": severity_str,
        "_coordinates": found_coords,
        "_locations": found_locations,
        "_symptoms": found_symptoms,
        "_summary_list": summary_sentences,
        "_confidence": confidence,
    }
