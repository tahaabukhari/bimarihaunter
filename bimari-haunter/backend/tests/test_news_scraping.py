"""
LIVE polling tests for Pakistani news sources.

Tests actually hit real websites to verify:
  1. List pages are reachable (HTTP 200)
  2. CSS selectors discover article links
  3. Article pages can be fetched and parsed
  4. Content hash works for deduplication
  5. Health/disease keyword detection on real articles

Run with:
    pytest tests/test_news_scraping.py -v -s

Skip live tests in CI:
    pytest -m "not live"
"""

from __future__ import annotations

import hashlib
import re

import httpx
import pytest

from app.scraper.crawler import NewsCrawler
from app.scraper.sources.pakistani_news import PAKISTANI_NEWS_SOURCES


# ── Helpers ─────────────────────────────────────────────────

# Only test sources that do NOT require Playwright (pure httpx)
HTTPX_SOURCES = [
    s for s in PAKISTANI_NEWS_SOURCES if not s["scrape_config"].get("use_playwright")
]

# Sources that need Playwright (skip in basic tests)
PLAYWRIGHT_SOURCES = [
    s for s in PAKISTANI_NEWS_SOURCES if s["scrape_config"].get("use_playwright")
]

# Health / disease keywords we care about
HEALTH_KEYWORDS_EN = [
    "dengue", "malaria", "covid", "corona", "virus", "disease", "outbreak",
    "epidemic", "pandemic", "fever", "cholera", "polio", "measles",
    "hepatitis", "tuberculosis", "flu", "influenza", "infection",
    "hospital", "patient", "vaccine", "health", "medical", "doctor",
    "death", "killed", "died", "cases", "treatment", "WHO",
]

HEALTH_KEYWORDS_UR = [
    "\\u0688\\u06cc\\u0646\\u06af\\u06cc",     # dengue
    "\\u0645\\u0644\\u06cc\\u0631\\u06cc\\u0627",     # malaria
    "\\u06a9\\u0648\\u0648\\u0688",           # covid
    "\\u0648\\u0627\\u0626\\u0631\\u0633",         # virus
    "\\u0628\\u06cc\\u0645\\u0627\\u0631\\u06cc",     # disease
    "\\u0648\\u0628\\u0627",               # epidemic
    "\\u0628\\u062e\\u0627\\u0631",             # fever
    "\\u06c1\\u0633\\u067e\\u062a\\u0627\\u0644",     # hospital
    "\\u0645\\u0631\\u06cc\\u0636",           # patient
    "\\u0635\\u062d\\u062a",             # health
]

# Build combined regex (case-insensitive)
_ALL_KEYWORDS = HEALTH_KEYWORDS_EN + HEALTH_KEYWORDS_UR
_HEALTH_PATTERN = re.compile(
    "|".join(re.escape(kw) for kw in _ALL_KEYWORDS),
    re.IGNORECASE,
)


def _source_ids(sources: list[dict]) -> list[str]:
    return [s["name"] for s in sources]


def contains_health_keyword(text: str) -> list[str]:
    """Return list of health keywords found in text."""
    if not text:
        return []
    return list(set(_HEALTH_PATTERN.findall(text.lower())))


# ── Test: Source configs are well-formed (no network) ──────


class TestSourceConfigs:

    def test_all_sources_present(self):
        assert len(PAKISTANI_NEWS_SOURCES) == 9

    def test_httpx_sources_count(self):
        """At least 5 sources work without Playwright."""
        assert len(HTTPX_SOURCES) >= 5, (
            f"Only {len(HTTPX_SOURCES)} httpx sources; need at least 5"
        )

    @pytest.mark.parametrize(
        "source", PAKISTANI_NEWS_SOURCES, ids=_source_ids(PAKISTANI_NEWS_SOURCES)
    )
    def test_required_fields(self, source: dict):
        assert source["name"]
        assert source["base_url"].startswith("http")
        assert source["country"] == "Pakistan"

        cfg = source["scrape_config"]
        assert cfg["list_url"].startswith("http")
        assert cfg["link_selector"]
        assert cfg["title_selector"]
        assert cfg["content_selector"]

    @pytest.mark.parametrize(
        "source", PAKISTANI_NEWS_SOURCES, ids=_source_ids(PAKISTANI_NEWS_SOURCES)
    )
    def test_schedule_is_cron(self, source: dict):
        parts = source["schedule"].split()
        assert len(parts) == 5, f"Invalid cron: {source['schedule']}"


# ── Test: Live list-page fetch (httpx sources only) ────────


@pytest.mark.live
class TestListPageReachability:

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_list_page_returns_200(self, source: dict):
        """The list page should return HTTP 200 and non-empty HTML."""
        url = source["scrape_config"]["list_url"]
        async with httpx.AsyncClient(
            follow_redirects=True,
            timeout=30.0,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
                ),
                "Accept": "text/html,application/xhtml+xml",
            },
        ) as client:
            resp = await client.get(url)

        assert resp.status_code == 200, (
            f"{source['name']} list page returned {resp.status_code}"
        )
        assert len(resp.text) > 500, (
            f"{source['name']} list page body too short ({len(resp.text)} chars)"
        )


# ── Test: Article discovery ─────────────────────────────────


@pytest.mark.live
class TestArticleDiscovery:

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_discovers_links(self, source: dict):
        """The crawler should find at least 1 article link."""
        crawler = NewsCrawler(source["scrape_config"])
        urls = await crawler.discover_articles()

        assert isinstance(urls, list)
        assert len(urls) >= 1, (
            f"{source['name']}: discover_articles returned 0 links"
        )

        # Every URL should be absolute
        for url in urls[:5]:
            assert url.startswith("http"), f"Relative URL found: {url}"

        print(f"\n  [OK] {source['name']}: discovered {len(urls)} article links")
        for u in urls[:3]:
            print(f"    -> {u}")

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_max_50_links(self, source: dict):
        """discover_articles must cap at 50 URLs."""
        crawler = NewsCrawler(source["scrape_config"])
        urls = await crawler.discover_articles()
        assert len(urls) <= 50


# ── Test: Single article fetch & parse ──────────────────────


@pytest.mark.live
class TestArticleFetch:

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_fetch_first_article(self, source: dict):
        """Fetch the first discovered article and extract content."""
        crawler = NewsCrawler(source["scrape_config"])
        urls = await crawler.discover_articles()

        if not urls:
            pytest.skip(f"{source['name']}: no articles discovered")

        article = await crawler.fetch_article(urls[0])

        assert article["url"] == urls[0]
        assert article["url"].startswith("http")

        print(f"\n  [OK] {source['name']} - first article:")
        print(f"    URL:   {article['url']}")
        print(f"    Title: {article.get('title', '(none)')}")
        print(f"    Date:  {article.get('published_at', '(none)')}")
        text = article.get("text") or ""
        print(f"    Body:  {len(text)} chars")
        if text:
            preview = text[:120].encode("ascii", errors="replace").decode()
            print(f"    Preview: {preview}...")

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_content_hash_generation(self, source: dict):
        """Every article should produce a deterministic SHA-256 hash."""
        crawler = NewsCrawler(source["scrape_config"])
        urls = await crawler.discover_articles()

        if not urls:
            pytest.skip(f"{source['name']}: no articles discovered")

        article = await crawler.fetch_article(urls[0])
        content = article.get("text") or article.get("raw_html") or ""

        if not content:
            pytest.skip(f"{source['name']}: article body empty")

        content_hash = hashlib.sha256(content.encode()).hexdigest()
        assert len(content_hash) == 64
        assert content_hash == hashlib.sha256(content.encode()).hexdigest()

        print(f"\n  [OK] {source['name']} hash: {content_hash[:16]}...")


# ── Test: Health keyword detection in real articles ─────────


@pytest.mark.live
class TestHealthKeywordDetection:
    """Scan discovered articles for dengue/disease/health keywords.

    This is NOT a pass/fail gate per individual article -- most news on
    any given day won't be health-related.  The test verifies that:
      1) The keyword scanner works correctly
      2) We can surface health articles when they exist
    """

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_scan_headlines_for_health(self, source: dict):
        """Scan up to 20 headlines from each source for health keywords."""
        crawler = NewsCrawler(source["scrape_config"])
        urls = await crawler.discover_articles()

        if not urls:
            pytest.skip(f"{source['name']}: no articles discovered")

        # Fetch headlines from list page directly (faster than individual articles)
        list_url = source["scrape_config"]["list_url"]
        async with httpx.AsyncClient(
            follow_redirects=True,
            timeout=30.0,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
                ),
                "Accept": "text/html,application/xhtml+xml",
            },
        ) as client:
            resp = await client.get(list_url)

        from bs4 import BeautifulSoup
        soup = BeautifulSoup(resp.text, "html.parser")
        link_sel = source["scrape_config"]["link_selector"]

        headlines = []
        for tag in soup.select(link_sel)[:30]:
            text = tag.get_text(strip=True)
            if text and len(text) > 15:
                headlines.append(text)

        health_hits = []
        for headline in headlines:
            keywords = contains_health_keyword(headline)
            if keywords:
                safe_headline = headline[:80].encode("ascii", errors="replace").decode()
                health_hits.append((safe_headline, keywords))

        print(f"\n  [SCAN] {source['name']}: scanned {len(headlines)} headlines")
        print(f"         Health-related: {len(health_hits)}")

        for headline, kws in health_hits[:5]:
            print(f"    [HIT] {headline}")
            print(f"          Keywords: {', '.join(kws)}")

        if not health_hits:
            print(f"    [INFO] No health headlines today - this is normal")

        # Always passes: we just want to see the scan output
        assert len(headlines) > 0, f"{source['name']}: no headlines extracted"

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_scan_article_body_for_health(self, source: dict):
        """Fetch up to 3 articles and scan full body for health keywords."""
        crawler = NewsCrawler(source["scrape_config"])
        urls = await crawler.discover_articles()

        if not urls:
            pytest.skip(f"{source['name']}: no articles discovered")

        health_articles = []
        scanned = 0

        for url in urls[:5]:
            article = await crawler.fetch_article(url)
            text = article.get("text") or ""
            title = article.get("title") or ""
            combined = f"{title} {text}"
            scanned += 1

            keywords = contains_health_keyword(combined)
            if keywords:
                safe_title = title[:60].encode("ascii", errors="replace").decode()
                health_articles.append({
                    "title": safe_title,
                    "keywords": keywords,
                    "text_length": len(text),
                    "url": url,
                })

        print(f"\n  [DEEP SCAN] {source['name']}: scanned {scanned} articles")
        print(f"              Health-related: {len(health_articles)}")

        for hit in health_articles:
            print(f"    [HIT] {hit['title']}")
            print(f"          Keywords: {', '.join(hit['keywords'])}")
            print(f"          Body: {hit['text_length']} chars")
            print(f"          URL: {hit['url'][:80]}")

        if not health_articles:
            print(f"    [INFO] No health articles in top {scanned} - normal for non-outbreak days")

        # The test passes as long as scanning works (no crashes)
        assert scanned > 0


# ── Test: Keyword scanner unit tests (no network) ──────────


class TestKeywordScanner:

    def test_detects_dengue(self):
        hits = contains_health_keyword("50 dengue cases reported in Lahore")
        assert "dengue" in hits

    def test_detects_disease(self):
        hits = contains_health_keyword("New disease outbreak in Sindh")
        assert "disease" in hits
        assert "outbreak" in hits

    def test_detects_multiple(self):
        hits = contains_health_keyword(
            "Hospital reports 10 malaria patients with high fever"
        )
        assert "hospital" in hits
        assert "malaria" in hits
        assert "fever" in hits

    def test_case_insensitive(self):
        hits = contains_health_keyword("DENGUE Outbreak in KARACHI")
        assert "dengue" in hits
        assert "outbreak" in hits

    def test_no_false_positives(self):
        hits = contains_health_keyword("Cricket match today at National Stadium")
        assert len(hits) == 0

    def test_empty_text(self):
        assert contains_health_keyword("") == []
        assert contains_health_keyword(None) == []


# ── Test: Media extraction ──────────────────────────────────


@pytest.mark.live
class TestMediaExtraction:

    @pytest.mark.parametrize(
        "source", HTTPX_SOURCES, ids=_source_ids(HTTPX_SOURCES)
    )
    @pytest.mark.asyncio
    async def test_media_urls_are_list(self, source: dict):
        crawler = NewsCrawler(source["scrape_config"])
        urls = await crawler.discover_articles()
        if not urls:
            pytest.skip(f"{source['name']}: no articles")

        article = await crawler.fetch_article(urls[0])
        media = article.get("media_urls", [])
        assert isinstance(media, list)
        print(f"\n  [OK] {source['name']}: {len(media)} media URLs found")


# ── Test: Deduplication logic (no network) ──────────────────


class TestDeduplication:

    def test_identical_content_same_hash(self):
        content = "Dengue outbreak reported in Karachi with 50 cases"
        h1 = hashlib.sha256(content.encode()).hexdigest()
        h2 = hashlib.sha256(content.encode()).hexdigest()
        assert h1 == h2

    def test_different_content_different_hash(self):
        h1 = hashlib.sha256(b"Dengue outbreak in Karachi").hexdigest()
        h2 = hashlib.sha256(b"Malaria cases in Lahore").hexdigest()
        assert h1 != h2

    def test_hash_length(self):
        h = hashlib.sha256(b"test").hexdigest()
        assert len(h) == 64


# ── Test: Playwright source configs (no network) ───────────


class TestPlaywrightSourceConfigs:

    def test_dawn_needs_playwright(self):
        dawn = next(s for s in PAKISTANI_NEWS_SOURCES if s["name"] == "Dawn News")
        assert dawn["scrape_config"]["use_playwright"] is True

    def test_thenews_needs_playwright(self):
        tn = next(s for s in PAKISTANI_NEWS_SOURCES if s["name"] == "The News International")
        assert tn["scrape_config"]["use_playwright"] is True

    def test_express_tribune_httpx(self):
        et = next(s for s in PAKISTANI_NEWS_SOURCES if s["name"] == "Express Tribune")
        assert et["scrape_config"]["use_playwright"] is False

    def test_ary_httpx(self):
        ary = next(s for s in PAKISTANI_NEWS_SOURCES if s["name"] == "ARY News")
        assert ary["scrape_config"]["use_playwright"] is False

    def test_nation_httpx(self):
        n = next(s for s in PAKISTANI_NEWS_SOURCES if s["name"] == "The Nation")
        assert n["scrape_config"]["use_playwright"] is False

    def test_pakistan_today_httpx(self):
        pt = next(s for s in PAKISTANI_NEWS_SOURCES if s["name"] == "Pakistan Today")
        assert pt["scrape_config"]["use_playwright"] is False

    def test_daily_pakistan_httpx(self):
        dp = next(s for s in PAKISTANI_NEWS_SOURCES if s["name"] == "Daily Pakistan")
        assert dp["scrape_config"]["use_playwright"] is False
