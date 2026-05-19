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
        print("Navigating to Yahoo Search...")
        url = "https://search.yahoo.com/search?p=site:x.com+dengue+Karachi"
        await page.goto(url, wait_until="domcontentloaded")
        content = await page.content()
        await browser.close()
        
        soup = BeautifulSoup(content, "html.parser")
        results = soup.select("div.algo")
        print(f"Total result elements found on Yahoo: {len(results)}")
        
        if len(results) == 0:
            safe_text = soup.text.strip().encode("ascii", "ignore").decode("ascii")
            print("\nPage text length:", len(safe_text))
            print("First 500 chars of page text:\n", safe_text[:500])
        else:
            for idx, res in enumerate(results[:3]):
                link = res.select_one("h3 a")
                snippet = res.select_one("span.compText") or res.select_one("div.compText")
                print(f"\n--- Result {idx + 1} ---")
                print("Link:", link.get("href") if link else "None")
                safe_snippet = snippet.text.strip().encode("ascii", "ignore").decode("ascii") if snippet else "None"
                print("Snippet:", safe_snippet)

asyncio.run(debug())
