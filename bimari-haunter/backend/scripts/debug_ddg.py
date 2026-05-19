import asyncio
from playwright.async_api import async_playwright
from bs4 import BeautifulSoup

async def debug():
    async with async_playwright() as p:
        # Launch browser with advanced anti-detection flags
        browser = await p.chromium.launch(
            headless=True,
            args=[
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-infobars",
                "--window-position=0,0",
                "--ignore-certificate-errors",
            ]
        )
        
        # Create context masking user agent and platform
        context = await browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            viewport={"width": 1920, "height": 1080},
            locale="en-US",
            timezone_id="Asia/Karachi",
        )
        
        # Legendary anti-detection: Remove navigator.webdriver
        await context.add_init_script(
            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        )
        
        page = await context.new_page()
        print("Navigating to DuckDuckGo HTML search with stealth bypass...")
        
        url = "https://html.duckduckgo.com/html/?q=site:x.com+dengue+Karachi"
        await page.goto(url, wait_until="domcontentloaded")
        content = await page.content()
        await browser.close()
        
        soup = BeautifulSoup(content, "html.parser")
        results = soup.select(".result")
        print(f"Total result elements found: {len(results)}")
        
        if len(results) == 0:
            print("\nPage text length:", len(soup.text.strip()))
            print("First 500 chars of page text:\n", soup.text.strip()[:500])
        else:
            for idx, res in enumerate(results[:3]):
                link = res.select_one("a.result__url")
                snippet = res.select_one("a.result__snippet")
                print(f"\n--- Result {idx + 1} ---")
                print("Link:", link.get("href") if link else "None")
                print("Snippet:", snippet.text.strip() if snippet else "None")

asyncio.run(debug())
