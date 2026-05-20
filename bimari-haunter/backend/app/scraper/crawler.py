"""
Web crawler for Pakistani news websites.

Uses httpx for fast async HTTP and BeautifulSoup for HTML parsing.
Optionally falls back to Playwright for JS-rendered pages.
"""

from __future__ import annotations

import re
from typing import Any, Optional
from urllib.parse import urljoin

import httpx
import structlog
from bs4 import BeautifulSoup, Comment

logger = structlog.get_logger(__name__)

# Tags removed during text cleaning
_NOISE_TAGS = {"script", "style", "nav", "footer", "header", "aside", "noscript"}


class NewsCrawler:
    """Stateless async news-article crawler."""

    def __init__(
        self,
        source_config: dict[str, Any],
        *,
        timeout: float = 30.0,
        headers: dict[str, str] | None = None,
    ) -> None:
        self.config = source_config
        self.timeout = timeout
        self.headers = headers or {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
            "Accept": "text/html,application/xhtml+xml",
            "Accept-Language": "en-US,en;q=0.9,ur;q=0.8",
        }

    # ── Article discovery ───────────────────────────────────

    async def discover_articles(self) -> list[str]:
        """Fetch the list page and return up to 50 article URLs."""
        list_url: str = self.config.get("list_url", "")
        link_selector: str = self.config.get("link_selector", "a")
        use_playwright: bool = self.config.get("use_playwright", False)

        if use_playwright:
            html = await self._fetch_with_playwright(list_url)
        else:
            html = await self._fetch(list_url)

        if not html:
            return []

        soup = BeautifulSoup(html, "html.parser")
        links: list[str] = []
        seen: set[str] = set()

        for tag in soup.select(link_selector):
            href = tag.get("href")
            if not href:
                continue
            url = urljoin(list_url, href)
            if url not in seen:
                seen.add(url)
                links.append(url)

        logger.info(
            "articles_discovered",
            list_url=list_url,
            count=len(links),
        )
        return links[:50]

    # ── Single-article fetch ────────────────────────────────

    async def fetch_article(self, url: str) -> dict[str, Any]:
        """Fetch and parse a single article page."""
        use_playwright: bool = self.config.get("use_playwright", False)

        if use_playwright:
            html = await self._fetch_with_playwright(url)
        else:
            html = await self._fetch(url)

        if not html:
            return {"url": url, "title": None, "raw_html": None, "text": None}

        soup = BeautifulSoup(html, "html.parser")

        title = self._extract_text(soup, self.config.get("title_selector"))
        content_html = soup.select_one(self.config.get("content_selector", "body"))
        text = self._clean_text(content_html) if content_html else None
        published_at = self._extract_text(soup, self.config.get("date_selector"))
        media_urls = self._extract_media(soup)

        return {
            "url": url,
            "title": title,
            "raw_html": str(content_html) if content_html else None,
            "text": text,
            "published_at": published_at,
            "media_urls": media_urls,
        }

    # ── Private helpers ─────────────────────────────────────

    async def _fetch(self, url: str) -> Optional[str]:
        """Plain HTTP fetch via httpx."""
        try:
            async with httpx.AsyncClient(
                headers=self.headers,
                timeout=self.timeout,
                follow_redirects=True,
            ) as client:
                resp = await client.get(url)
                resp.raise_for_status()
                return resp.text
        except Exception as exc:
            logger.error("fetch_failed", url=url, error=str(exc))
            return None

    async def _fetch_with_playwright(self, url: str) -> Optional[str]:
        """JS-rendered fetch via Playwright Chromium."""
        try:
            from playwright.async_api import async_playwright

            async with async_playwright() as pw:
                browser = await pw.chromium.launch(headless=True)
                page = await browser.new_page()
                await page.goto(url, timeout=int(self.timeout * 1000))
                await page.wait_for_load_state("networkidle")
                html = await page.content()
                await browser.close()
                return html
        except Exception as exc:
            logger.error("playwright_fetch_failed", url=url, error=str(exc))
            return None

    @staticmethod
    def _extract_text(soup: BeautifulSoup, selector: Optional[str]) -> Optional[str]:
        if not selector:
            return None
        el = soup.select_one(selector)
        return el.get_text(strip=True) if el else None

    @staticmethod
    def _clean_text(tag) -> str:
        """Remove noise tags and return visible text."""
        for noise in tag.find_all(_NOISE_TAGS):
            noise.decompose()
        for comment in tag.find_all(string=lambda t: isinstance(t, Comment)):
            comment.extract()
        text = tag.get_text(separator="\n", strip=True)
        # Collapse whitespace
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text.strip()

    @staticmethod
    def _extract_media(soup: BeautifulSoup) -> list[str]:
        urls: list[str] = []
        for img in soup.find_all("img", src=True):
            urls.append(img["src"])
        for video in soup.find_all("video", src=True):
            urls.append(video["src"])
        return urls
