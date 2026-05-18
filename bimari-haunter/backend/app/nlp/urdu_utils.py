"""
Urdu text utilities.

Provides normalisation via urduhack, language detection based on
Unicode code-point ranges, and rule-based keyword lists for Urdu
entity extraction.
"""

from __future__ import annotations

import re
from typing import Optional

import structlog

logger = structlog.get_logger(__name__)

# ── Urdu disease keywords ──────────────────────────────────

DISEASE_KEYWORDS_URDU: list[str] = [
    "ڈینگی",
    "ملیریا",
    "کووڈ",
    "کورونا",
    "وائرس",
    "ٹی بی",
    "خسرہ",
    "ہیپاٹائٹس",
    "پولیو",
    "ایچ آئی وی",
    "ایڈز",
    "فلو",
]

# ── Urdu location keywords ─────────────────────────────────

LOCATION_KEYWORDS_URDU: list[str] = [
    "کراچی",
    "لاہور",
    "اسلام آباد",
    "پشاور",
    "کوئٹہ",
    "ملتان",
    "فیصل آباد",
    "راولپنڈی",
    "حیدرآباد",
    "سندھ",
    "پنجاب",
    "خیبر پختونخوا",
    "بلوچستان",
]

# ── Urdu statistics keywords ───────────────────────────────

AFFECTED_KEYWORDS_URDU: list[str] = ["مریض", "افراد", "کیسز", "متاثر"]
DEATH_KEYWORDS_URDU: list[str] = ["ہلاک", "موت", "فوت"]
RECOVERED_KEYWORDS_URDU: list[str] = ["صحت", "ٹھیک", "رخصت"]

# ── Urdu Unicode range ─────────────────────────────────────

_URDU_RANGE = re.compile(r"[\u0600-\u06FF]")


def is_urdu(text: str, threshold: float = 0.3) -> bool:
    """Return True if ≥ *threshold* fraction of characters are Urdu."""
    if not text:
        return False
    urdu_chars = len(_URDU_RANGE.findall(text))
    return (urdu_chars / len(text)) >= threshold


def normalize_urdu(text: str) -> str:
    """Normalise Urdu text via urduhack (character + punctuation normalisation)."""
    try:
        import urduhack

        text = urduhack.normalize(text)
    except Exception as exc:
        logger.warning("urduhack_normalize_failed", error=str(exc))
    return text


def extract_urdu_entities(text: str) -> dict:
    """Rule-based extraction of diseases and locations from Urdu text."""
    diseases: list[str] = []
    locations: list[str] = []

    for keyword in DISEASE_KEYWORDS_URDU:
        if keyword in text:
            diseases.append(keyword)

    for keyword in LOCATION_KEYWORDS_URDU:
        if keyword in text:
            locations.append(keyword)

    return {
        "diseases": list(set(diseases)),
        "locations": list(set(locations)),
    }


def extract_urdu_statistics(text: str) -> dict:
    """Extract numeric counts adjacent to Urdu keywords."""
    number_pattern = re.compile(r"(\d+)")
    affected: Optional[int] = None
    deaths: Optional[int] = None
    recovered: Optional[int] = None

    for keyword in AFFECTED_KEYWORDS_URDU:
        if keyword in text:
            idx = text.index(keyword)
            window = text[max(0, idx - 30) : idx + len(keyword) + 30]
            nums = number_pattern.findall(window)
            if nums:
                affected = max(int(n) for n in nums)

    for keyword in DEATH_KEYWORDS_URDU:
        if keyword in text:
            idx = text.index(keyword)
            window = text[max(0, idx - 30) : idx + len(keyword) + 30]
            nums = number_pattern.findall(window)
            if nums:
                deaths = max(int(n) for n in nums)

    for keyword in RECOVERED_KEYWORDS_URDU:
        if keyword in text:
            idx = text.index(keyword)
            window = text[max(0, idx - 30) : idx + len(keyword) + 30]
            nums = number_pattern.findall(window)
            if nums:
                recovered = max(int(n) for n in nums)

    return {
        "affected": affected,
        "deaths": deaths,
        "recovered": recovered,
    }
