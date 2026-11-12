"""Nesine Spor Toto form automation using a private Chrome profile.

This module only selects and verifies form checkboxes. It deliberately never
clicks "Hemen Oyna", "Onayla" or another wager-submission control.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from playwright.sync_api import BrowserContext, Page, Playwright, sync_playwright

NESINE_URL = "https://www.nesine.com/sportoto"
MATCH_COUNT = 15
GROUP_COUNT = 4  # A, B, C, D on the official Spor Toto form
OUTCOMES = {"1": 0, "X": 1, "2": 2}


def normalize_columns(raw: list[str]) -> list[str]:
    """Validate exactly representable selections; 0 is a draw alias for X."""
    if not 1 <= len(raw) <= GROUP_COUNT:
        raise ValueError("Tek seferde 1–4 adet 15 maçlık kolon aktarılabilir.")
    normalized: list[str] = []
    for position, value in enumerate(raw, start=1):
        if not isinstance(value, str):
            raise ValueError(f"{position}. kolon metin olmalı.")
        column = value.strip().upper().replace("0", "X")
        if len(column) != MATCH_COUNT or any(c not in OUTCOMES for c in column):
            raise ValueError(f"{position}. kolon tam 15 adet 1, 0/X veya 2 içermeli.")
        normalized.append(column)
    return normalized


def is_official_program(url: str) -> bool:
    parsed = urlparse(url)
    return (
        parsed.scheme == "https"
        and parsed.hostname == "www.nesine.com"
        and parsed.path.rstrip("/") == "/sportoto"
    )


@dataclass(frozen=True)
class FillResult:
    filled_columns: int
    checked_outcomes: int
    verified: bool
    page_url: str

    def as_dict(self) -> dict[str, Any]:
        return {
            "filled_columns": self.filled_columns,
            "checked_outcomes": self.checked_outcomes,
            "verified": self.verified,
            "page_url": self.page_url,
            "wager_submitted": False,
        }


class NesineBrowser:
    """All methods must run on the SAME dedicated Playwright thread."""

    def __init__(
        self,
        profile_path: Path,
        *,
        headless: bool = True,
        chrome_channel: str = "chrome",
    ) -> None:
        self.profile_path = profile_path.expanduser().resolve()
        self.headless = headless
        self.chrome_channel = chrome_channel
        self._pw: Playwright | None = None
        self._context: BrowserContext | None = None
        self.page: Page | None = None
        self.last_result: FillResult | None = None

    def start(self) -> None:
        self.profile_path.mkdir(mode=0o700, parents=True, exist_ok=True)
        self._pw = sync_playwright().start()
        try:
            kwargs: dict[str, Any] = {
                "user_data_dir": str(self.profile_path),
                "headless": self.headless,
                "accept_downloads": False,
            }
            if self.chrome_channel != "chromium":
                kwargs["channel"] = self.chrome_channel
            self._context = self._pw.chromium.launch_persistent_context(**kwargs)
            self.page = self._context.pages[0] if self._context.pages else self._context.new_page()
            self.page.goto(NESINE_URL, wait_until="domcontentloaded", timeout=45000)
        except Exception:
            self.stop()
            raise

    def stop(self) -> None:
        if self._context is not None:
            self._context.close()
            self._context = None
        self.page = None
        if self._pw is not None:
            self._pw.stop()
            self._pw = None

    def _program_page(self) -> Page:
        if self.page is None:
            raise RuntimeError("Chrome henüz başlatılmadı.")
        if not is_official_program(self.page.url):
            self.page.goto(NESINE_URL, wait_until="domcontentloaded", timeout=45000)
        self.page.wait_for_selector("#m-c-14-3-2", timeout=30000)
        return self.page

    def fill(self, requested_columns: list[str]) -> FillResult:
        columns = normalize_columns(requested_columns)
        page = self._program_page()

        # Fail before changing anything if Nesine has changed its 15x4 form.
        count = page.locator('input[id^="m-c-"][type="checkbox"]').count()
        if count != MATCH_COUNT * GROUP_COUNT * len(OUTCOMES):
            raise RuntimeError(
                "Nesine seçim alanlarının yapısı değişmiş. Kupon aktarılmadı."
            )

        # Each A/B/C/D group represents ONE exact column. Selecting doubles
        # here would silently expand a filtered matrix, so never do that.
        for group in range(GROUP_COUNT):
            column = columns[group] if group < len(columns) else None
            for match in range(MATCH_COUNT):
                wanted = OUTCOMES[column[match]] if column is not None else None
                for outcome in range(3):
                    locator = page.locator(f"#m-c-{match}-{group}-{outcome}")
                    should_be_checked = outcome == wanted
                    if locator.is_checked() != should_be_checked:
                        if should_be_checked:
                            locator.check(timeout=10000)
                        else:
                            locator.uncheck(timeout=10000)

        checked_count = 0
        for group in range(GROUP_COUNT):
            column = columns[group] if group < len(columns) else None
            for match in range(MATCH_COUNT):
                wanted = OUTCOMES[column[match]] if column is not None else None
                for outcome in range(3):
                    actual = page.locator(f"#m-c-{match}-{group}-{outcome}").is_checked()
                    if actual != (outcome == wanted):
                        raise RuntimeError(
                            "Nesine seçimlerinin doğrulaması başarısız; kuponu onaylamayın."
                        )
                    checked_count += int(actual)

        self.last_result = FillResult(
            filled_columns=len(columns),
            checked_outcomes=checked_count,
            verified=True,
            page_url=page.url,
        )
        return self.last_result

    def preview_png(self) -> bytes:
        if self.page is None:
            raise RuntimeError("Chrome henüz başlatılmadı.")
        return self.page.screenshot(type="png", full_page=False)

    def status(self) -> dict[str, Any]:
        return {
            "browser_running": self.page is not None,
            "page_url": self.page.url if self.page else None,
            "last_fill": self.last_result.as_dict() if self.last_result else None,
            "account_login_verified": False,
            "wager_submitted": False,
        }
