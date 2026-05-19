"""
Test script for the Playwright Social Media Scraper.
Runs an immediate test query for X and Facebook and logs results.
"""

import asyncio
from app.scraper.social_playwright import PlaywrightSocialScraper

async def test_scrape():
    print("--- Initializing Playwright Social Scraper test ---")
    scraper = PlaywrightSocialScraper(use_headless=True)
    
    query = "dengue Karachi"
    
    print(f"\n[X Scraper] Scraping X (Twitter) via Nitter for query: '{query}'...")
    try:
        x_posts = await scraper.scrape_x(query, limit=3)
        print(f"SUCCESS: X Scrape completed! Found {len(x_posts)} posts.")
        for idx, post in enumerate(x_posts):
            print(f"\n--- Tweet {idx + 1} ---")
            print(f"Author: {post['author']}")
            print(f"URL: {post['permalink']}")
            print(f"Text: {post['cleaned_text'][:120]}...")
            print(f"Locations: {post['locations']}")
            print(f"Coordinates: {post['coordinates']}")
    except Exception as e:
        print(f"FAILED: X Scrape failed: {str(e)}")

    print(f"\n[Facebook Scraper] Scraping Facebook via mbasic for query: '{query}'...")
    try:
        fb_posts = await scraper.scrape_facebook(query, limit=3)
        print(f"SUCCESS: Facebook Scrape completed! Found {len(fb_posts)} posts.")
        for idx, post in enumerate(fb_posts):
            print(f"\n--- Facebook Post {idx + 1} ---")
            print(f"Author: {post['author']}")
            print(f"URL: {post['permalink']}")
            print(f"Text: {post['cleaned_text'][:120]}...")
            print(f"Locations: {post['locations']}")
            print(f"Coordinates: {post['coordinates']}")
    except Exception as e:
        print(f"FAILED: Facebook Scrape failed: {str(e)}")

if __name__ == "__main__":
    asyncio.run(test_scrape())
