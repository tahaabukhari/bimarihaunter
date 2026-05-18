"""
Facebook Graph API integration tests.

Run unit tests (no token needed):
    pytest tests/test_facebook_api.py -v -m "not facebook"

Run with real token:
    FACEBOOK_ACCESS_TOKEN=<token> pytest tests/test_facebook_api.py -v -s -m facebook
"""
from __future__ import annotations
import hashlib, os
import httpx, pytest, respx
from app.scraper.facebook_client import (
    DISEASE_KEYWORDS_EN, DISEASE_KEYWORDS_URDU,
    FacebookGraphClient, FacebookScraper,
)

FACEBOOK_TOKEN = os.getenv("FACEBOOK_ACCESS_TOKEN", "")
HAS_TOKEN = bool(FACEBOOK_TOKEN)

class TestHealthKeywordFiltering:
    @pytest.mark.parametrize("keyword", DISEASE_KEYWORDS_EN)
    def test_english_keywords(self, keyword):
        assert FacebookScraper.is_health_related(f"Breaking: {keyword} cases in Islamabad")

    @pytest.mark.parametrize("keyword", DISEASE_KEYWORDS_URDU)
    def test_urdu_keywords(self, keyword):
        assert FacebookScraper.is_health_related(f"اسلام آباد میں {keyword} کے کیسز")

    def test_non_health_rejected(self):
        assert not FacebookScraper.is_health_related("Cricket match today")
        assert not FacebookScraper.is_health_related("")

    def test_mixed_language(self):
        assert FacebookScraper.is_health_related("Lahore میں dengue کے 200 نئے کیسز")

class TestTextCleaning:
    def test_url_removal(self):
        cleaned = FacebookScraper._clean_text("Check https://example.com for dengue")
        assert "https://" not in cleaned and "dengue" in cleaned

    def test_whitespace_collapse(self):
        assert "  " not in FacebookScraper._clean_text("Dengue   outbreak    in   Karachi")

class TestMediaExtraction:
    def test_photo(self):
        post = {"attachments":{"data":[{"type":"photo","media":{"image":{"src":"https://cdn.fb.com/p.jpg"}}}]}}
        assert FacebookScraper._extract_media(post) == ["https://cdn.fb.com/p.jpg"]

    def test_no_attachments(self):
        assert FacebookScraper._extract_media({}) == []

    def test_subattachments(self):
        post = {"attachments":{"data":[{"type":"photo","media":{"image":{"src":"https://a.jpg"}},"subattachments":{"data":[{"media":{"image":{"src":"https://b.jpg"}}},{"media":{"image":{"src":"https://c.jpg"}}}]}}]}}
        assert len(FacebookScraper._extract_media(post)) == 3

class TestGraphClientMocked:
    @pytest.mark.asyncio
    @respx.mock
    async def test_get_page_posts(self):
        mock = {"data":[{"id":"111_222","message":"Dengue cases rising in Karachi","created_time":"2026-05-05T10:00:00+0000","permalink_url":"https://facebook.com/111/posts/222","likes":{"summary":{"total_count":42}},"comments":{"summary":{"total_count":7}},"shares":{"count":3}},{"id":"111_333","message":"Cricket update","created_time":"2026-05-05T09:00:00+0000"}]}
        respx.get("https://graph.facebook.com/v18.0/test_page/posts").mock(return_value=httpx.Response(200, json=mock))
        posts = await FacebookGraphClient("fake").get_page_posts("test_page", limit=10)
        assert len(posts) == 2
        assert posts[0]["message"] == "Dengue cases rising in Karachi"

    @pytest.mark.asyncio
    @respx.mock
    async def test_scraper_filters_health_posts(self):
        mock = {"data":[
            {"id":"p1","message":"50 dengue patients admitted to hospital","created_time":"2026-05-05T10:00:00+0000","permalink_url":"https://fb.com/p1","likes":{"summary":{"total_count":100}},"comments":{"summary":{"total_count":20}},"shares":{"count":15}},
            {"id":"p2","message":"Pakistan won the cricket match!","created_time":"2026-05-05T09:00:00+0000"},
            {"id":"p3","message":"ڈینگی بخار کے مریضوں کی تعداد بڑھ رہی ہے","created_time":"2026-05-05T08:00:00+0000","permalink_url":"https://fb.com/p3","likes":{"summary":{"total_count":50}},"comments":{"summary":{"total_count":10}}},
        ]}
        respx.get("https://graph.facebook.com/v18.0/tp/posts").mock(return_value=httpx.Response(200, json=mock))
        results = await FacebookScraper({"access_token":"fake","rate_limit":200}).scrape("tp")
        assert len(results) == 2
        assert results[0]["likes_count"] == 100
        assert len(results[0]["content_hash"]) == 64

    @pytest.mark.asyncio
    @respx.mock
    async def test_api_error(self):
        respx.get("https://graph.facebook.com/v18.0/bad/posts").mock(return_value=httpx.Response(400, json={"error":{"message":"Invalid"}}))
        with pytest.raises(httpx.HTTPStatusError):
            await FacebookGraphClient("fake").get_page_posts("bad")

    @pytest.mark.asyncio
    @respx.mock
    async def test_empty_response(self):
        respx.get("https://graph.facebook.com/v18.0/empty/posts").mock(return_value=httpx.Response(200, json={"data":[]}))
        assert await FacebookGraphClient("fake").get_page_posts("empty") == []

@pytest.mark.facebook
class TestLiveFacebookAPI:
    @pytest.mark.skipif(not HAS_TOKEN, reason="FACEBOOK_ACCESS_TOKEN not set")
    @pytest.mark.asyncio
    async def test_token_valid(self):
        async with httpx.AsyncClient(timeout=15) as http:
            resp = await http.get(f"https://graph.facebook.com/v18.0/me", params={"access_token": FACEBOOK_TOKEN})
        print(f"\n  Token check: HTTP {resp.status_code}")
        assert resp.status_code == 200

    @pytest.mark.skipif(not HAS_TOKEN, reason="FACEBOOK_ACCESS_TOKEN not set")
    @pytest.mark.asyncio
    async def test_fetch_page_posts(self):
        page_id = os.getenv("FACEBOOK_TEST_PAGE_ID", "")
        if not page_id: pytest.skip("Set FACEBOOK_TEST_PAGE_ID")
        posts = await FacebookGraphClient(FACEBOOK_TOKEN).get_page_posts(page_id, limit=5)
        assert isinstance(posts, list)
        print(f"\n  ✓ Fetched {len(posts)} posts from {page_id}")
