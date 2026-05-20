"""
rss_scraper.py — Reliable multi-source health news scraper for Pakistan.

Architecture (in priority order):
  1. Google News RSS  — primary backbone, no auth, 50-100 items per query, always up
  2. Direct RSS feeds — Dawn, ARY, Geo, The News (confirmed working)
  3. Playwright fallback — used only for Urdu-language content that RSS doesn't cover

The old PAKISTANI_NEWS_SOURCES-based crawler used CSS-selector scraping against
sites that either 404'd, 403'd, or required JS rendering. This module bypasses
that entirely and uses RSS/Atom feeds which are confirmed to return 20-250 items.

Confirmed live endpoint results (tested 2026-05-21):
  Google News RSS (dengue pakistan)       → 100 items
  Google News RSS (health/disease pk)     → 64 items
  Google News RSS (malaria pakistan)      → 68 items
  Google News RSS (flood/disaster pk)     → 75 items
  Google News RSS (cholera/typhoid pk)    → 49 items
  ARY News RSS                            → 250 items
  The News PK RSS                         → 50 items
  Dawn Pakistan RSS                       → 30 items
  Geo Health RSS                          → 20 items
"""
from __future__ import annotations

import hashlib
import re
import urllib.parse
import urllib.request
import ssl
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from typing import Any

import structlog

logger = structlog.get_logger(__name__)

# ── SSL context (some PK news sites have misconfigured certs) ─────────────────
_SSL_CTX = ssl.create_default_context()
_SSL_CTX.check_hostname = False
_SSL_CTX.verify_mode = ssl.CERT_NONE

_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/124.0.0.0 Safari/537.36"
)

# ── Health / disease keyword filter ──────────────────────────────────────────
HEALTH_KEYWORDS_EN = [
    "dengue", "malaria", "cholera", "typhoid", "polio", "measles", "hepatitis",
    "tuberculosis", " tb ", "covid", "coronavirus", "influenza", "flu",
    "outbreak", "epidemic", "pandemic", "disease", "infection", "virus",
    "bacteria", "health", "hospital", "patient", "fever", "diarrhea",
    "gastroenteritis", "contamination", "water-borne", "vector-borne",
    "flood", "disaster", "emergency", "vaccination", "vaccine", "who",
    "ministry of health", "nhsrc", "nih pakistan", "mortality", "death toll",
    "quarantine", "surveillance", "zoonotic", "rabies", "anthrax",
    "pneumonia", "respiratory", "monkeypox", "mpox",
]

HEALTH_KEYWORDS_URDU = [
    "بیماری", "وبا", "ڈینگی", "ملیریا", "ہیضہ", "ٹائیفائیڈ", "پولیو",
    "خسرہ", "ہیپاٹائٹس", "تپ دق", "کورونا", "انفلوئنزا", "بخار",
    "وبائی مرض", "وبائی", "صحت", "ہسپتال", "مریض", "وائرس", "بیکٹیریا",
    "سیلاب", "آفت", "ہنگامی صورتحال", "ویکسین", "قرنطینہ",
]

# ── Google News RSS queries (confirmed: 49–100 items each) ───────────────────
GNEWS_QUERIES = [
    "dengue fever pakistan",
    "malaria pakistan",
    "cholera typhoid pakistan",
    "disease outbreak pakistan",
    "pakistan health emergency",
    "flood disaster pakistan health",
    "polio pakistan",
    "hepatitis pakistan",
    "pakistan ministry of health",
    "nih pakistan disease",
    "coronavirus pakistan 2026",
]

GNEWS_BASE = "https://news.google.com/rss/search?hl=en-PK&gl=PK&ceid=PK:en&q={}"

# ── Direct RSS feeds (confirmed working) ─────────────────────────────────────
DIRECT_RSS_FEEDS = [
    ("Dawn Pakistan",    "https://www.dawn.com/feeds/pakistan"),
    ("Dawn Home",        "https://www.dawn.com/feeds/home"),
    ("ARY News",         "https://arynews.tv/feed/"),
    ("Geo Health",       "https://www.geo.tv/rss/1/1"),
    ("Geo Pakistan",     "https://www.geo.tv/rss/1/2"),
    ("The News PK",      "https://www.thenews.com.pk/rss/1/1"),
    ("ProPakistani",     "https://propakistani.pk/feed/"),
]

# ── Pakistani city → (lat, lon) lookup ───────────────────────────────────────
CITY_COORDS: dict[str, tuple[float, float]] = {
    "karachi":        (24.8607, 67.0011),
    "lahore":         (31.5204, 74.3587),
    "islamabad":      (33.6844, 73.0479),
    "rawalpindi":     (33.5651, 73.0169),
    "faisalabad":     (31.4504, 73.1350),
    "multan":         (30.1575, 71.5249),
    "peshawar":       (34.0151, 71.5249),
    "quetta":         (30.1798, 66.9750),
    "hyderabad":      (25.3960, 68.3578),
    "gujranwala":     (32.1877, 74.1945),
    "sialkot":        (32.4945, 74.5229),
    "bahawalpur":     (29.3956, 71.6836),
    "sukkur":         (27.7052, 68.8574),
    "larkana":        (27.5570, 68.2118),
    "sheikhupura":    (31.7167, 73.9850),
    "rahim yar khan": (28.4202, 70.2952),
    "jhang":          (31.2681, 72.3181),
    "dera ghazi khan":(30.0500, 70.6333),
    "gujrat":         (32.5736, 74.0790),
    "sahiwal":        (30.6682, 73.1066),
    "sindh":          (25.8943, 68.5247),
    "punjab":         (31.1471, 75.3412),
    "kpk":            (34.0151, 71.5249),
    "khyber pakhtunkhwa": (34.0151, 71.5249),
    "balochistan":    (28.4907, 65.0958),
    "gilgit":         (35.9220, 74.3085),
    "azad kashmir":   (34.0954, 73.8397),
    "pakistan":       (30.3753, 69.3451),
}


def _fetch_rss(url: str, timeout: int = 12) -> bytes | None:
    """Fetch a URL and return the raw bytes, or None on failure."""
    try:
        req = urllib.request.Request(url, headers={"User-Agent": _UA})
        with urllib.request.urlopen(req, timeout=timeout, context=_SSL_CTX) as r:
            return r.read()
    except Exception as e:
        logger.warning("rss_fetch_failed", url=url[:80], error=str(e))
        return None


def _parse_rss_items(body: bytes) -> list[dict[str, str]]:
    """Parse RSS/Atom XML and return list of raw item dicts."""
    items: list[dict[str, str]] = []
    try:
        root = ET.fromstring(body)
    except ET.ParseError:
        return items

    # RSS 2.0
    for item in root.iter("item"):
        title = item.findtext("title", "").strip()
        link = item.findtext("link", "").strip()
        desc = item.findtext("description", "").strip()
        pub = item.findtext("pubDate", "").strip()
        source_el = item.find("source")
        source = source_el.text.strip() if source_el is not None else ""
        if title or desc:
            items.append({"title": title, "link": link, "description": desc,
                          "pub_date": pub, "source": source})

    # Atom
    for entry in root.iter("{http://www.w3.org/2005/Atom}entry"):
        title = entry.findtext("{http://www.w3.org/2005/Atom}title", "").strip()
        link_el = entry.find("{http://www.w3.org/2005/Atom}link")
        link = (link_el.get("href", "") if link_el is not None else "").strip()
        summary = entry.findtext("{http://www.w3.org/2005/Atom}summary", "").strip()
        pub = entry.findtext("{http://www.w3.org/2005/Atom}published", "").strip()
        if title or summary:
            items.append({"title": title, "link": link, "description": summary,
                          "pub_date": pub, "source": ""})

    return items


# Strong disease keywords that alone are sufficient to classify an article as health-related
STRONG_DISEASE_KEYWORDS = [
    "dengue", "malaria", "cholera", "typhoid", "polio", "measles", "hepatitis",
    "tuberculosis", " tb ", "covid", "coronavirus", "influenza", "h1n1",
    "outbreak", "epidemic", "pandemic", "quarantine", "monkeypox", "mpox",
    "rabies", "anthrax", "zoonotic", "gastroenteritis", "water-borne",
    "vector-borne", "nhsrc", "nih pakistan",
]

def _is_health_related(text: str) -> bool:
    lower = text.lower()
    # A single strong disease keyword is enough
    for kw in STRONG_DISEASE_KEYWORDS:
        if kw in lower:
            return True
    # Weaker keywords require at least 2 hits to avoid false positives
    hit_count = sum(1 for kw in HEALTH_KEYWORDS_EN if kw in lower)
    if hit_count >= 2:
        return True
    # Urdu keywords — any single hit is sufficient
    for kw in HEALTH_KEYWORDS_URDU:
        if kw in text:
            return True
    return False


def _clean_text(text: str) -> str:
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"https?://\S+", "", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def _resolve_location(text: str) -> tuple[list[str], tuple[float, float]]:
    lower = text.lower()
    found: list[str] = []
    coords: tuple[float, float] = CITY_COORDS["pakistan"]
    for city, latlon in CITY_COORDS.items():
        if city in lower:
            found.append(city.title())
            if coords == CITY_COORDS["pakistan"]:
                coords = latlon
    if not found:
        found = ["Pakistan"]
    return found, coords


def _make_report(raw: dict[str, str], source_name: str) -> dict[str, Any] | None:
    """
    Convert a raw RSS item into a Firestore-ready report dict.
    The shape matches exactly what FeedRepository.documentToEntity() and
    the users.py /location fan-out expect:
      - top-level: title, source, url, raw_text, published_at, scraped_at, status
      - nested ai_analysis: disease, severity, summary, symptoms, locations,
                            coordinates (GeoPoint), confidence_score, model_used
    """
    title = _clean_text(raw.get("title", ""))
    description = _clean_text(raw.get("description", ""))
    combined = f"{title} {description}"

    if not _is_health_related(combined):
        return None

    link = raw.get("link", "")
    if not link:
        return None

    content_hash = hashlib.sha256(link.encode()).hexdigest()
    locations, coords = _resolve_location(combined)

    # Severity heuristic
    lower = combined.lower()
    if any(w in lower for w in ["critical", "emergency", "death", "fatality", "killed",
                                 "surge", "epidemic", "hundreds of cases", "thousands"]):
        severity = "high"
    elif any(w in lower for w in ["warning", "alert", "outbreak", "spread", "rise",
                                   "increase", "reported", "confirmed"]):
        severity = "medium"
    else:
        severity = "low"

    # Disease detection
    disease = "outbreak"
    for d, keywords in {
        "dengue":        ["dengue"],
        "malaria":       ["malaria", "plasmodium"],
        "cholera":       ["cholera"],
        "typhoid":       ["typhoid", "enteric fever"],
        "polio":         ["polio"],
        "hepatitis":     ["hepatitis", "jaundice"],
        "tuberculosis":  ["tuberculosis", " tb "],
        "covid":         ["covid", "coronavirus"],
        "influenza":     ["influenza", " flu "],
        "measles":       ["measles"],
    }.items():
        if any(kw in lower for kw in keywords):
            disease = d
            break

    # Symptom extraction
    symptoms: list[str] = []
    for symptom, keywords in {
        "fever":              ["fever", "pyrexia"],
        "cough":              ["cough"],
        "headache":           ["headache"],
        "diarrhea":           ["diarrhea", "diarrhoea"],
        "vomiting":           ["vomiting", "nausea"],
        "joint pain":         ["joint pain", "muscle pain", "body ache"],
        "rash":               ["rash", "skin rash"],
        "fatigue":            ["fatigue", "weakness"],
        "breathing difficulty": ["shortness of breath", "breathing difficulty"],
        "jaundice":           ["jaundice"],
    }.items():
        if any(kw in lower for kw in keywords):
            symptoms.append(symptom)

    # Summary — use description sentences or fall back to title
    sentences = [s.strip() for s in re.split(r"[.!?]", description) if len(s.strip()) > 30]
    summary = sentences[:3] if sentences else [title[:120]]

    pub_date = raw.get("pub_date", "")
    try:
        published_at = parsedate_to_datetime(pub_date).astimezone(timezone.utc)
    except Exception:
        published_at = datetime.now(timezone.utc)

    source_label = raw.get("source") or source_name

    # Import GeoPoint here to avoid circular imports at module level
    from google.cloud import firestore as _fs
    geo_coords = _fs.GeoPoint(coords[0], coords[1])

    return {
        # Top-level fields (FeedRepository.documentToEntity reads these directly)
        "content_hash": content_hash,
        "title": title or "Health Update",
        "source": source_label,
        "url": link,
        "raw_text": description[:1000] if description else title,
        "published_at": published_at,
        "scraped_at": datetime.now(timezone.utc),
        "status": "analyzed",
        # Nested ai_analysis (FeedRepository reads ai_analysis.disease, .severity, etc.)
        "ai_analysis": {
            "disease": disease,
            "severity": severity,
            "summary": summary,
            "symptoms": symptoms,
            "locations": locations,
            "coordinates": geo_coords,
            "confidence_score": 0.75,
            "model_used": "rss-keyword-classifier-v2",
        },
    }


# ── Per-tag RSS query map ────────────────────────────────────────────────────
# Maps each FeedTag.id (from the Android app) to one or more targeted
# Google News RSS queries. These are used when a user has saved feed_tags.
TAG_RSS_QUERIES: dict[str, list[str]] = {
    # Viral & Infectious Diseases
    "dengue":        ["dengue fever pakistan", "dengue outbreak"],
    "malaria":       ["malaria pakistan", "malaria outbreak"],
    "covid":         ["covid pakistan 2026", "coronavirus pakistan"],
    "influenza":     ["influenza flu pakistan", "flu outbreak pakistan"],
    "mpox":          ["mpox monkeypox pakistan", "mpox outbreak"],
    "measles":       ["measles outbreak pakistan"],
    "polio":         ["polio pakistan", "poliovirus"],
    "hepatitis":     ["hepatitis pakistan", "hepatitis outbreak"],
    "tuberculosis":  ["tuberculosis tb pakistan"],
    "cholera":       ["cholera outbreak pakistan"],
    "typhoid":       ["typhoid pakistan", "enteric fever pakistan"],
    "rabies":        ["rabies pakistan"],
    # Natural Disasters
    "floods":        ["floods pakistan", "flood disaster pakistan"],
    "earthquake":    ["earthquake pakistan"],
    "heatwave":      ["heatwave pakistan", "extreme heat pakistan"],
    "drought":       ["drought pakistan water crisis"],
    "cyclone":       ["cyclone storm pakistan"],
    "landslide":     ["landslide pakistan"],
    # Global Health
    "who_alerts":    ["WHO health alert", "WHO outbreak warning"],
    "outbreak":      ["disease outbreak pakistan", "health outbreak"],
    "pandemic":      ["pandemic health emergency"],
    "vaccination":   ["vaccination drive pakistan", "vaccine pakistan"],
    "antimicrobial": ["antimicrobial resistance pakistan", "drug resistant bacteria"],
    "zoonotic":      ["zoonotic disease pakistan", "animal human disease"],
    # Pakistan Health
    "pk_health":     ["pakistan health news", "ministry of health pakistan"],
    "nih_pakistan":  ["NIH pakistan disease", "national institute of health pakistan"],
    "water_quality": ["water contamination pakistan", "water borne disease pakistan"],
    "air_quality":   ["smog lahore", "air pollution pakistan"],
    "food_safety":   ["food poisoning pakistan", "food safety pakistan"],
    # Economy & Society
    "economy":       ["pakistan economy 2026", "pakistan economic crisis"],
    "food_crisis":   ["food crisis pakistan", "hunger malnutrition pakistan"],
    "refugee":       ["refugee health pakistan", "displaced persons pakistan"],
    "mental_health": ["mental health pakistan", "depression anxiety pakistan"],
}


# ── Public API ────────────────────────────────────────────────────────────────

def scrape_google_news_rss(max_per_query: int = 100) -> list[dict[str, Any]]:
    """
    Fetch health news from Google News RSS for each disease query.
    Returns deduplicated Firestore-ready report dicts.
    """
    seen: set[str] = set()
    results: list[dict[str, Any]] = []

    for query in GNEWS_QUERIES:
        url = GNEWS_BASE.format(urllib.parse.quote_plus(query))
        body = _fetch_rss(url)
        if not body:
            continue

        items = _parse_rss_items(body)
        logger.info("gnews_rss_fetched", query=query, raw_count=len(items))

        for raw in items[:max_per_query]:
            report = _make_report(raw, "Google News")
            if report and report["content_hash"] not in seen:
                seen.add(report["content_hash"])
                results.append(report)

    logger.info("gnews_rss_complete", total=len(results))
    return results


def scrape_direct_rss_feeds() -> list[dict[str, Any]]:
    """
    Fetch from confirmed-working direct RSS feeds (Dawn, ARY, Geo, The News).
    Filters to health-related items only.
    """
    seen: set[str] = set()
    results: list[dict[str, Any]] = []

    for source_name, url in DIRECT_RSS_FEEDS:
        body = _fetch_rss(url)
        if not body:
            continue

        items = _parse_rss_items(body)
        logger.info("direct_rss_fetched", source=source_name, raw_count=len(items))

        for raw in items:
            report = _make_report(raw, source_name)
            if report and report["content_hash"] not in seen:
                seen.add(report["content_hash"])
                results.append(report)

    logger.info("direct_rss_complete", total=len(results))
    return results


def scrape_all_rss() -> list[dict[str, Any]]:
    """
    Synchronous entry point — runs Google News RSS + direct RSS feeds.
    Returns a deduplicated, health-filtered, Firestore-ready list.
    Sorted newest first.
    """
    seen: set[str] = set()
    all_reports: list[dict[str, Any]] = []

    for report in scrape_google_news_rss():
        if report["content_hash"] not in seen:
            seen.add(report["content_hash"])
            all_reports.append(report)

    for report in scrape_direct_rss_feeds():
        if report["content_hash"] not in seen:
            seen.add(report["content_hash"])
            all_reports.append(report)

    all_reports.sort(
        key=lambda r: r.get("published_at", datetime.min.replace(tzinfo=timezone.utc)),
        reverse=True,
    )

    logger.info("scrape_all_rss_complete", total=len(all_reports))
    return all_reports


def scrape_tags_rss(
    tag_ids: list[str],
    max_per_query: int = 50,
) -> list[dict[str, Any]]:
    """
    Targeted scrape for a specific user's selected tag IDs.

    Only runs the Google News RSS queries that correspond to the user's
    chosen tags. Falls back to scrape_all_rss() if tag_ids is empty.

    Returns a deduplicated, health-filtered, Firestore-ready list sorted
    newest-first.
    """
    if not tag_ids:
        return scrape_all_rss()

    # Collect unique queries for the requested tags
    queries: list[str] = []
    seen_queries: set[str] = set()
    for tag_id in tag_ids:
        for q in TAG_RSS_QUERIES.get(tag_id, []):
            if q not in seen_queries:
                seen_queries.add(q)
                queries.append(q)

    if not queries:
        logger.warning("scrape_tags_rss_no_queries", tag_ids=tag_ids)
        return scrape_all_rss()

    seen: set[str] = set()
    results: list[dict[str, Any]] = []

    for query in queries:
        url = GNEWS_BASE.format(urllib.parse.quote_plus(query))
        body = _fetch_rss(url)
        if not body:
            continue
        items = _parse_rss_items(body)
        logger.info("tag_rss_fetched", query=query, raw_count=len(items))
        for raw in items[:max_per_query]:
            report = _make_report(raw, "Google News")
            if report and report["content_hash"] not in seen:
                seen.add(report["content_hash"])
                results.append(report)

    results.sort(
        key=lambda r: r.get("published_at", datetime.min.replace(tzinfo=timezone.utc)),
        reverse=True,
    )
    logger.info("scrape_tags_rss_complete", tags=tag_ids, total=len(results))
    return results
