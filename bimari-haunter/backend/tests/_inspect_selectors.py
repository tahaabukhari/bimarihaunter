"""Find content divs with actual text > 200 chars."""
import asyncio, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
import httpx
from bs4 import BeautifulSoup

HEADERS = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0'}

async def dump_content_divs(name, url):
    async with httpx.AsyncClient(headers=HEADERS, follow_redirects=True, timeout=20) as c:
        r = await c.get(url)
    soup = BeautifulSoup(r.text, 'html.parser')
    print(f'\n=== {name} ===')
    for div in soup.find_all(['div','section','article','main'], class_=True):
        cls = ' '.join(div.get('class', []))
        text = div.get_text(strip=True)
        if len(text) > 200:
            for kw in ['content','story','article','detail','body','post','desc','news','text']:
                if kw in cls.lower():
                    print(f'  <{div.name} class="{cls[:60]}"> text_len={len(text)}')
                    break

async def main():
    await dump_content_divs('PakistanToday',
        'https://www.pakistantoday.com.pk/2026/05/06/pakistan-army-cadet-wins-top-foreign-honour-at-australian-military-college')
    await dump_content_divs('BolNews',
        'https://www.bolnews.com/entertainment/olivia-rodrigo-drops-massive-unraveled-world-tour-announcement')
    await dump_content_divs('DailyPakistan',
        'https://en.dailypakistan.com.pk/06-May-2026/gold-prices-increase-by-rs11100-per-tola-in-pakistan')

asyncio.run(main())
