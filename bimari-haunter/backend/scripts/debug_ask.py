import asyncio
from playwright.async_api import async_playwright
from bs4 import BeautifulSoup

async def debug():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        page = await context.new_page()
        print("Navigating to Ask.com...")
        url = "https://www.ask.com/web?q=site:x.com+dengue+Karachi"
        await page.goto(url, wait_until="domcontentloaded")
        content = await page.content()
        await browser.close()
        
        soup = BeautifulSoup(content, "html.parser")
        results = soup.select("div.PartialSearchResults-item")
        print(f"Total result elements found on Ask: {len(results)}")
        
        if len(results) == 0:
            print("\nPage text length:", len(soup.text.strip()))
            print("First 500 chars of page text:\n", soup.text.strip()[:500])
        else:
            for idx, res in enumerate(results[:3]):
                link = res.select_one("a.PartialSearchResults-item-title-link")
                snippet = res.select_one("p.PartialSearchResults-item-abstract")
                print(f"\n--- Result {idx + 1} ---")
                print("Link:", link.get("href") if link else "None")
                print("Snippet:", snippet.text.strip() if snippet else "None")

asyncio.run(debug())
