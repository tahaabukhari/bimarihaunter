"""
Central NLP processor for BimariHaunter.

Orchestrates entity extraction (spaCy / Urdu rules), zero-shot
classification, summarisation, statistics extraction, and severity
scoring.  All heavy models are loaded lazily via app.nlp.models.
"""

from __future__ import annotations

import re
import time
from typing import Any, Optional

import structlog

from app.nlp.models import (
    load_spacy_model,
    load_summarizer,
    load_zero_shot_classifier,
)
from app.nlp.urdu_utils import (
    extract_urdu_entities,
    extract_urdu_statistics,
    is_urdu,
    normalize_urdu,
)

logger = structlog.get_logger(__name__)

# ── Classification labels ──────────────────────────────────

CANDIDATE_LABELS: list[str] = [
    "disease outbreak alert",
    "health advisory warning",
    "medical treatment news",
    "general health information",
    "hospital capacity report",
    "vaccination campaign",
]

# ── English statistics regex patterns ──────────────────────

_AFFECTED_PATTERN = re.compile(
    r"(\d[\d,]*)\s*(?:people|persons|individuals|patients|cases|"
    r"affected|confirmed|reported|tested\s+positive)",
    re.IGNORECASE,
)
_DEATH_PATTERN = re.compile(
    r"(\d[\d,]*)\s*(?:deaths?|died|passed\s+away|fatalities|"
    r"killed|lost\s+their\s+life)",
    re.IGNORECASE,
)
_RECOVERED_PATTERN = re.compile(
    r"(\d[\d,]*)\s*(?:recovered|discharged|healed|"
    r"released\s+from\s+hospital)",
    re.IGNORECASE,
)


class NLPProcessor:
    """Stateless processor – instantiate once, call ``process`` per text."""

    def __init__(self) -> None:
        # Models are loaded lazily on first access
        self._spacy = None
        self._classifier = None
        self._summarizer = None

    # ── Lazy loaders ────────────────────────────────────────

    @property
    def spacy(self):
        if self._spacy is None:
            self._spacy = load_spacy_model()
        return self._spacy

    @property
    def classifier(self):
        if self._classifier is None:
            self._classifier = load_zero_shot_classifier()
        return self._classifier

    @property
    def summarizer(self):
        if self._summarizer is None:
            self._summarizer = load_summarizer()
        return self._summarizer

    # ── Public API ──────────────────────────────────────────

    def process(self, text: str) -> dict[str, Any]:
        """Run the full NLP pipeline on *text* and return a result dict."""
        start = time.perf_counter()

        urdu = is_urdu(text)
        lang = "ur" if urdu else "en"

        if urdu:
            text = normalize_urdu(text)

        entities = self.extract_entities(text, lang=lang)
        classification = self.classify(text)
        summary = self.summarize(text)
        statistics = self.extract_statistics(text, lang=lang)
        severity = self.calculate_severity(
            classification=classification,
            statistics=statistics,
            diseases=entities.get("diseases", []),
        )

        elapsed_ms = int((time.perf_counter() - start) * 1000)

        return {
            "language": lang,
            "entities": entities,
            "classification": classification,
            "summary": summary,
            "statistics": statistics,
            "severity": severity,
            "processing_time_ms": elapsed_ms,
            "model_metadata": {
                "ner": "en_core_web_trf" if lang == "en" else "urdu_rules",
                "classifier": "facebook/bart-large-mnli",
                "summarizer": "facebook/bart-large-cnn",
            },
        }

    # ── Entity extraction ───────────────────────────────────

    def extract_entities(
        self, text: str, *, lang: str = "en"
    ) -> dict[str, list]:
        """Extract named entities from text.

        For English uses spaCy NER; for Urdu uses rule-based keyword
        matching.
        """
        if lang == "ur":
            urdu_ents = extract_urdu_entities(text)
            return {
                "diseases": urdu_ents.get("diseases", []),
                "symptoms": [],
                "locations": urdu_ents.get("locations", []),
                "organizations": [],
                "people": [],
                "dates": [],
            }

        doc = self.spacy(text)
        diseases: list[str] = []
        locations: list[str] = []
        organizations: list[str] = []
        people: list[str] = []
        dates: list[str] = []

        for ent in doc.ents:
            if ent.label_ in ("DISEASE", "HEALTH"):
                diseases.append(ent.text)
            elif ent.label_ == "GPE":
                locations.append(ent.text)
            elif ent.label_ == "ORG":
                organizations.append(ent.text)
            elif ent.label_ == "PERSON":
                people.append(ent.text)
            elif ent.label_ == "DATE":
                dates.append(ent.text)

        return {
            "diseases": list(set(diseases)),
            "symptoms": [],
            "locations": list(set(locations)),
            "organizations": list(set(organizations)),
            "people": list(set(people)),
            "dates": list(set(dates)),
        }

    # ── Classification ──────────────────────────────────────

    def classify(self, text: str) -> dict[str, Any]:
        """Zero-shot classify text into one of CANDIDATE_LABELS."""
        try:
            result = self.classifier(
                text[:1024],
                candidate_labels=CANDIDATE_LABELS,
            )
            return {
                "category": result["labels"][0],
                "score": round(result["scores"][0], 4),
                "all_scores": dict(zip(result["labels"], result["scores"])),
            }
        except Exception as exc:
            logger.error("classification_failed", error=str(exc))
            return {
                "category": "general health information",
                "score": 0.0,
                "all_scores": {},
            }

    # ── Summarisation ───────────────────────────────────────

    def summarize(self, text: str) -> str:
        """Generate a concise summary of *text*."""
        try:
            truncated = text[:1024]
            if len(truncated) < 50:
                return truncated
            result = self.summarizer(
                truncated,
                max_length=150,
                min_length=30,
                do_sample=False,
            )
            return result[0]["summary_text"]
        except Exception as exc:
            logger.error("summarization_failed", error=str(exc))
            return text[:300]

    # ── Statistics extraction ───────────────────────────────

    def extract_statistics(
        self, text: str, *, lang: str = "en"
    ) -> dict[str, Optional[int]]:
        """Extract numeric counts (affected, deaths, recovered)."""
        if lang == "ur":
            return extract_urdu_statistics(text)

        affected: Optional[int] = None
        deaths: Optional[int] = None
        recovered: Optional[int] = None

        match = _AFFECTED_PATTERN.search(text)
        if match:
            affected = int(match.group(1).replace(",", ""))

        match = _DEATH_PATTERN.search(text)
        if match:
            deaths = int(match.group(1).replace(",", ""))

        match = _RECOVERED_PATTERN.search(text)
        if match:
            recovered = int(match.group(1).replace(",", ""))

        return {
            "affected": affected,
            "deaths": deaths,
            "recovered": recovered,
        }

    # ── Severity calculation ────────────────────────────────

    def calculate_severity(
        self,
        *,
        classification: dict[str, Any],
        statistics: dict[str, Optional[int]],
        diseases: list[str],
    ) -> float:
        """Return a severity score between 0.0 and 1.0."""
        category = classification.get("category", "")
        score = classification.get("score", 0.0)

        # Base score from classification category
        category_weights = {
            "disease outbreak alert": 0.4,
            "health advisory warning": 0.3,
            "hospital capacity report": 0.25,
            "vaccination campaign": 0.1,
            "medical treatment news": 0.15,
            "general health information": 0.05,
        }
        severity = category_weights.get(category, 0.05) * score

        # Death count boost
        deaths = statistics.get("deaths")
        if deaths and deaths > 0:
            severity += min(deaths * 0.05, 0.3)

        # Affected count boost
        affected = statistics.get("affected")
        if affected and affected > 10:
            severity += min(affected * 0.01, 0.2)

        # Disease count boost
        if diseases:
            severity += min(len(diseases) * 0.05, 0.1)

        return round(min(severity, 1.0), 4)
