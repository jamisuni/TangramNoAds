#!/usr/bin/env python3
"""
Convert preview SVGs to PNG with Chromium (Playwright).

    python tools/svg2png.py Tangrams/previews/*.svg

Needs: pip install playwright (Chromium must be available to Playwright).
"""
import re
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright


def main(paths):
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(device_scale_factor=1)
        for f in (Path(a) for a in paths):
            if f.suffix.lower() != ".svg":
                continue
            svg = f.read_text(encoding="utf-8")
            m = re.search(r'width="(\d+)" height="(\d+)"', svg)
            w, h = int(m.group(1)), int(m.group(2))
            page.set_viewport_size({"width": w, "height": h})
            page.set_content(f'<html><body style="margin:0">{svg}</body></html>')
            page.wait_for_timeout(150)
            out = f.with_suffix(".png")
            page.screenshot(path=str(out), clip={"x": 0, "y": 0, "width": w, "height": h})
            print(out)
        browser.close()


if __name__ == "__main__":
    main(sys.argv[1:])
