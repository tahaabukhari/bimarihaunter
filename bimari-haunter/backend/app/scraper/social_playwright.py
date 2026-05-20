"""
Playwright-based Social Media Scraper for X (Twitter) and Facebook.

Bypasses standard API and login limitations by dynamically using:
1. Yahoo Search Indexing Bypass (extracting site:x.com / site:facebook.com indexed posts)
   - Completely CAPTCHA-free, highly reliable, lightweight, and fast.
2. DuckDuckGo HTML Search Indexing Bypass (secondary fallback)
3. Public Nitter mirror instances (tertiary fallback)
4. Facebook Mobile Basic search (mbasic.facebook.com)

Provides highly accurate, anonymous, location-aware social media mining.
"""

from __future__ import annotations

import asyncio
import hashlib
import re
import urllib.parse
from datetime import datetime, timezone
from typing import Any, Optional
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright

import structlog

logger = structlog.get_logger(__name__)

# Geo-coordinate resolution mapping for Pakistan regions/cities
COORDINATES_MAP = {
    "karachi": (24.8607, 67.0011),
    "lahore": (31.5204, 74.3587),
    "islamabad": (33.6844, 73.0479),
    "rawalpindi": (33.5984, 73.0441),
    "peshawar": (34.0151, 71.5249),
    "quetta": (30.1798, 66.9750),
    "multan": (30.1575, 71.5249),
    "faisalabad": (31.4504, 73.1350),
    "sialkot": (32.4925, 74.5310),
    "gujranwala": (32.1877, 74.1945),
    "sindh": (25.8920, 68.5247),
    "punjab": (31.1704, 72.7097),
    "kpk": (34.9526, 72.3311),
    "balochistan": (28.4907, 65.0958),
}

NITTER_INSTANCES = [
    "https://nitter.poast.org",
    "https://nitter.cz",
    "https://nitter.net",
]

DISEASE_KEYWORDS_URDU = [
    "ڈینگی", "ملیریا", "کووڈ", "وائرس", "بخار",
    "وبا", "ہسپتال", "مریض", "ویکسین", "علاج",
]

DISEASE_KEYWORDS_EN = [
    "dengue", "malaria", "covid", "virus", "fever",
    "outbreak", "hospital", "patient", "vaccine",
    "treatment", "disease", "cholera", "typhoid",
]


class PlaywrightSocialScraper:
    """Handles browser-driven public social media extraction using Playwright."""

    def __init__(self, use_headless: bool = True) -> None:
        self.use_headless = use_headless

    @staticmethod
    def _clean_text(text: str) -> str:
        """Removes duplicate spaces, newlines, and URLs."""
        text = re.sub(r"https?://\S+", "", text)
        text = re.sub(r"\s+", " ", text)
        return text.strip()

    @staticmethod
    def _resolve_location(text: str) -> tuple[list[str], Optional[tuple[float, float]]]:
        """Scans post text for major locations and returns resolved coordinates."""
        lower_text = text.lower()
        found_locations: list[str] = []
        coordinates: Optional[tuple[float, float]] = None

        for city, coords in COORDINATES_MAP.items():
            if city in lower_text:
                found_locations.append(city.capitalize())
                if not coordinates:
                    coordinates = coords

        return found_locations, coordinates

    # ── X (Twitter) Scraper (Yahoo Bypass + DDG Bypass + Nitter Fallback) ────

    async def scrape_x(self, query: str, limit: int = 15) -> list[dict[str, Any]]:
        """Scrapes public health tweets matching *query*."""
        # 1. Attempt Yahoo Search Indexing Bypass first (super stable, 100% CAPTCHA-free)
        posts = await self._scrape_via_yahoo_search("site:x.com", query, limit)
        if posts:
            logger.info("x_yahoo_search_bypass_succeeded", count=len(posts))
            return posts

        # 2. Attempt DuckDuckGo HTML Search Indexing Bypass as secondary fallback
        posts = await self._scrape_via_ddg_search("site:x.com", query, limit)
        if posts:
            logger.info("x_ddg_search_bypass_succeeded", count=len(posts))
            return posts

        # 3. Fall back to Nitter pool
        logger.info("all_search_bypasses_returned_empty_falling_back_to_nitter")
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=self.use_headless)
            context = await browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            page = await context.new_page()

            success = False
            html_content = ""
            
            for instance in NITTER_INSTANCES:
                search_url = f"{instance}/search?f=tweets&q={query}"
                try:
                    await page.goto(search_url, timeout=12000, wait_until="domcontentloaded")
                    timeline_el = await page.query_selector(".timeline")
                    if timeline_el:
                        html_content = await page.content()
                        success = True
                        break
                except Exception as exc:
                    logger.warning("nitter_instance_failed", instance=instance, error=str(exc))
                    continue

            if not success or not html_content:
                await browser.close()
                return []

            soup = BeautifulSoup(html_content, "html.parser")
            tweet_items = soup.select(".timeline-item")
            
            for item in tweet_items[:limit]:
                try:
                    if "show-more" in item.get("class", []):
                        continue

                    author_el = item.select_one(".username")
                    author_name = author_el.text.strip() if author_el else "anonymous"

                    link_el = item.select_one(".tweet-link")
                    tweet_path = link_el["href"] if link_el else ""
                    permalink = f"https://x.com{tweet_path}" if tweet_path else ""

                    text_el = item.select_one(".tweet-content")
                    if not text_el:
                        continue
                    raw_text = text_el.text.strip()
                    cleaned_text = self._clean_text(raw_text)

                    date_el = item.select_one(".tweet-date a")
                    pub_date = datetime.now(timezone.utc)
                    if date_el and date_el.get("title"):
                        date_str = date_el["title"].replace(" UTC", "")
                        try:
                            pub_date = datetime.strptime(date_str, "%b %d, %Y · %I:%M %p").replace(tzinfo=timezone.utc)
                        except Exception:
                            pass

                    locations, coords = self._resolve_location(cleaned_text)
                    content_hash = hashlib.sha256((permalink + cleaned_text[:100]).encode()).hexdigest()

                    posts.append({
                        "platform": "x",
                        "author": author_name,
                        "permalink": permalink,
                        "raw_text": raw_text,
                        "cleaned_text": cleaned_text,
                        "published_at": pub_date,
                        "locations": locations,
                        "coordinates": coords,
                        "content_hash": content_hash
                    })
                except Exception:
                    pass

            await browser.close()

        return posts

    # ── Facebook Scraper (Yahoo Bypass + DDG Bypass + Mobile Basic Fallback) ────

    async def scrape_facebook(self, query: str, limit: int = 15) -> list[dict[str, Any]]:
        """Scrapes public health Facebook updates matching *query*."""
        # 1. Attempt Yahoo Search Indexing Bypass first (super stable)
        posts = await self._scrape_via_yahoo_search("site:facebook.com", query, limit)
        if posts:
            logger.info("facebook_yahoo_search_bypass_succeeded", count=len(posts))
            return posts

        # 2. Attempt DuckDuckGo HTML Search Indexing Bypass
        posts = await self._scrape_via_ddg_search("site:facebook.com", query, limit)
        if posts:
            logger.info("facebook_ddg_search_bypass_succeeded", count=len(posts))
            return posts

        # 3. Fall back to mobile basic site search
        logger.info("all_search_bypasses_returned_empty_falling_back_to_mbasic")
        posts = []
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=self.use_headless)
            context = await browser.new_context(
                user_agent="Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            page = await context.new_page()

            search_url = f"https://mbasic.facebook.com/search/posts/?q={query}"
            try:
                await page.goto(search_url, timeout=15000, wait_until="domcontentloaded")
                html_content = await page.content()
            except Exception as exc:
                logger.error("facebook_mbasic_load_failed", error=str(exc))
                await browser.close()
                return []

            soup = BeautifulSoup(html_content, "html.parser")
            post_containers = soup.find_all("div", role="article") or soup.find_all("table", class_="bj")
            
            for container in post_containers[:limit]:
                try:
                    text_block = container.find("div")
                    if not text_block:
                        continue
                    
                    raw_text = text_block.text.strip()
                    cleaned_text = self._clean_text(raw_text)

                    has_kw = any(kw in cleaned_text.lower() for kw in DISEASE_KEYWORDS_EN) or \
                             any(kw in cleaned_text for kw in DISEASE_KEYWORDS_URDU)
                    if not has_kw:
                        continue

                    author_name = "Facebook Public User"
                    author_link = container.find("a", class_="bk") or container.find("a")
                    if author_link:
                        author_name = author_link.text.strip()

                    permalink = "https://facebook.com/public"
                    all_links = container.find_all("a")
                    for link in all_links:
                        href = link.get("href", "")
                        if "story.php" in href or "/posts/" in href:
                            permalink = f"https://m.facebook.com{href}"
                            break

                    locations, coords = self._resolve_location(cleaned_text)
                    content_hash = hashlib.sha256((permalink + cleaned_text[:100]).encode()).hexdigest()

                    posts.append({
                        "platform": "facebook",
                        "author": author_name,
                        "permalink": permalink,
                        "raw_text": raw_text,
                        "cleaned_text": cleaned_text,
                        "published_at": datetime.now(timezone.utc),
                        "locations": locations,
                        "coordinates": coords,
                        "content_hash": content_hash
                    })
                except Exception:
                    pass

            await browser.close()

        return posts

    # ── Yahoo Search Indexing Bypass Helper (Premium, highly reliable) ──

    async def _scrape_via_yahoo_search(self, site: str, query: str, limit: int) -> list[dict[str, Any]]:
        """Queries Yahoo Search for site-specific indexed pages, extracting live snippets anonymously."""
        posts: list[dict[str, Any]] = []
        search_query = f"{site} {query}"
        encoded_query = urllib.parse.quote_plus(search_query)
        yahoo_url = f"https://search.yahoo.com/search?p={encoded_query}&n={limit}"

        async with async_playwright() as p:
            try:
                browser = await p.chromium.launch(headless=self.use_headless)
                context = await browser.new_context(
                    user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                page = await context.new_page()
                
                await page.goto(yahoo_url, timeout=15000, wait_until="domcontentloaded")
                html_content = await page.content()
                await browser.close()

                # Parse Yahoo results
                soup = BeautifulSoup(html_content, "html.parser")
                results = soup.select("div.algo")
                platform = "x" if "x.com" in site else "facebook"

                for res in results[:limit]:
                    # Extract Link
                    permalink = ""
                    all_links = res.find_all("a")
                    for a in all_links:
                        href = a.get("href", "")
                        # Unpack Yahoo redirect parameters if present
                        if "r.search.yahoo.com" in href:
                            parsed_url = urllib.parse.urlparse(href)
                            # The real URL is often in the path or redirected, but we can verify it contains x.com/facebook.com
                            if platform == "x" and ("x.com" in href or "twitter.com" in href):
                                permalink = href
                                break
                            if platform == "facebook" and "facebook.com" in href:
                                permalink = href
                                break
                        elif (platform == "x" and ("x.com" in href or "twitter.com" in href)) or \
                             (platform == "facebook" and "facebook.com" in href):
                            permalink = href
                            break
                    
                    if not permalink:
                        # Fallback default if not resolved
                        permalink = f"https://{platform}.com/public"

                    # Extract Snippet (actual post body text)
                    snippet_el = res.select_one("span.compText") or res.select_one("div.compText") or res.select_one("p")
                    if not snippet_el:
                        continue
                    raw_text = snippet_el.text.strip()
                    cleaned_text = self._clean_text(raw_text)
                    if not cleaned_text:
                        continue

                    # Extract Author from Title
                    author_name = f"{platform.capitalize()} Public User"
                    title_el = res.select_one("h3 a") or res.select_one("h3")
                    if title_el:
                        title_text = title_el.text.strip()
                        title_text = re.sub(r"\s+on\s+X\s*:?.*$", "", title_text, flags=re.IGNORECASE)
                        title_text = re.sub(r"\s*\|\s*Facebook\s*$", "", title_text, flags=re.IGNORECASE)
                        author_name = title_text

                    # Location resolution
                    locations, coords = self._resolve_location(cleaned_text)

                    # Deduplication hash
                    content_hash = hashlib.sha256((permalink + cleaned_text[:100]).encode()).hexdigest()

                    posts.append({
                        "platform": platform,
                        "author": author_name,
                        "permalink": permalink,
                        "raw_text": raw_text,
                        "cleaned_text": cleaned_text,
                        "published_at": datetime.now(timezone.utc),
                        "locations": locations,
                        "coordinates": coords,
                        "content_hash": content_hash
                    })

            except Exception as e:
                logger.warning("yahoo_search_bypass_failed", error=str(e))
                
        return posts

    # ── DuckDuckGo Search Indexing Bypass Helper ───────────────────────

    async def _scrape_via_ddg_search(self, site: str, query: str, limit: int) -> list[dict[str, Any]]:
        """Queries DuckDuckGo HTML Search for site-specific indexed pages, extracting live snippets anonymously."""
        posts: list[dict[str, Any]] = []
        search_query = f"{site} {query}"
        encoded_query = urllib.parse.quote_plus(search_query)
        ddg_url = f"https://html.duckduckgo.com/html/?q={encoded_query}"

        async with async_playwright() as p:
            try:
                browser = await p.chromium.launch(headless=self.use_headless)
                context = await browser.new_context(
                    user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                page = await context.new_page()
                
                await page.goto(ddg_url, timeout=15000, wait_until="domcontentloaded")
                html_content = await page.content()
                await browser.close()

                soup = BeautifulSoup(html_content, "html.parser")
                results = soup.select("div.result")
                platform = "x" if "x.com" in site else "facebook"

                for res in results[:limit]:
                    link_el = res.select_one("a.result__url")
                    if not link_el or not link_el.get("href"):
                        continue
                    permalink = link_el["href"]
                    
                    if "duckduckgo.com/l/?kh=-1&uddg=" in permalink:
                        parsed = urllib.parse.urlparse(permalink)
                        params = urllib.parse.parse_qs(parsed.query)
                        if "uddg" in params:
                            permalink = params["uddg"][0]

                    if platform == "x" and "x.com" not in permalink and "twitter.com" not in permalink:
                        continue
                    if platform == "facebook" and "facebook.com" not in permalink:
                        continue

                    snippet_el = res.select_one("a.result__snippet")
                    if not snippet_el:
                        continue
                    raw_text = snippet_el.text.strip()
                    cleaned_text = self._clean_text(raw_text)
                    if not cleaned_text:
                        continue

                    author_name = f"{platform.capitalize()} Public User"
                    title_el = res.select_one("a.result__title")
                    if title_el:
                        title_text = title_el.text.strip()
                        title_text = re.sub(r"\s+on\s+X\s*:?.*$", "", title_text, flags=re.IGNORECASE)
                        title_text = re.sub(r"\s*\|\s*Facebook\s*$", "", title_text, flags=re.IGNORECASE)
                        author_name = title_text

                    locations, coords = self._resolve_location(cleaned_text)
                    content_hash = hashlib.sha256((permalink + cleaned_text[:100]).encode()).hexdigest()

                    posts.append({
                        "platform": platform,
                        "author": author_name,
                        "permalink": permalink,
                        "raw_text": raw_text,
                        "cleaned_text": cleaned_text,
                        "published_at": datetime.now(timezone.utc),
                        "locations": locations,
                        "coordinates": coords,
                        "content_hash": content_hash
                    })

            except Exception as e:
                logger.warning("ddg_search_bypass_failed", error=str(e))
                
        return posts
