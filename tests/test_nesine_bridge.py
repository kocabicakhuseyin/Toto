"""Offline tests; no Nesine account, payment or live bet is needed."""

import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient

from nesine_bridge.api import create_app
from nesine_bridge.browser import (
    GROUP_COUNT,
    MATCH_COUNT,
    NESINE_URL,
    NesineBrowser,
    is_official_program,
    normalize_columns,
)


class FakeLocator:
    def __init__(self, page, selector):
        self.page = page
        self.selector = selector

    def count(self):
        return MATCH_COUNT * GROUP_COUNT * 3

    def is_checked(self):
        return self.page.checked[self.selector]

    def check(self, timeout=0):
        self.page.checked[self.selector] = True

    def uncheck(self, timeout=0):
        self.page.checked[self.selector] = False


class FakePage:
    def __init__(self):
        self.url = NESINE_URL
        self.checked = {
            f"#m-c-{m}-{g}-{o}": False
            for m in range(MATCH_COUNT)
            for g in range(GROUP_COUNT)
            for o in range(3)
        }

    def locator(self, selector):
        return FakeLocator(self, selector)

    def wait_for_selector(self, selector, timeout=0):
        if selector not in self.checked:
            raise RuntimeError("Missing selector")


class BrowserTests(unittest.TestCase):
    def test_draw_alias_and_validation(self):
        self.assertEqual(normalize_columns(["1" * 14 + "0"]), ["1" * 14 + "X"])
        with self.assertRaises(ValueError):
            normalize_columns(["1" * 14])
        with self.assertRaises(ValueError):
            normalize_columns(["1" * 15] * 5)
        with self.assertRaises(ValueError):
            normalize_columns(["1" * 14 + "A"])

    def test_domain_cannot_be_spoofed(self):
        self.assertTrue(is_official_program(NESINE_URL))
        self.assertFalse(is_official_program("http://www.nesine.com/sportoto"))
        self.assertFalse(is_official_program("https://www.nesine.com.evil.test/sportoto"))
        self.assertFalse(is_official_program("https://www.nesine.com/iddaa"))

    def test_four_exact_columns_and_batch_replacement(self):
        with tempfile.TemporaryDirectory() as directory:
            browser = NesineBrowser(Path(directory))
            browser.page = FakePage()
            columns = ["1" * 15, "X" * 15, "2" * 15, "1X2" * 5]
            result = browser.fill(columns)
            self.assertEqual(result.filled_columns, 4)
            self.assertEqual(result.checked_outcomes, 60)
            self.assertTrue(result.verified)
            self.assertFalse(result.as_dict()["wager_submitted"])

            # Following batch must remove all stale B/C/D selections.
            next_result = browser.fill(["2" * 15])
            self.assertEqual(next_result.filled_columns, 1)
            self.assertEqual(next_result.checked_outcomes, 15)
            for match in range(15):
                for group in (1, 2, 3):
                    for outcome in range(3):
                        self.assertFalse(browser.page.checked[f"#m-c-{match}-{group}-{outcome}"])


class FakeBrowserService:
    def __init__(self):
        self.columns = None

    def start(self):
        pass

    def stop(self):
        pass

    def status(self):
        return {"browser_running": True, "wager_submitted": False}

    def fill(self, columns):
        self.columns = columns

        class Result:
            def as_dict(self):
                return {"filled_columns": len(columns), "verified": True, "wager_submitted": False}

        return Result()

    def preview_png(self):
        return b"\x89PNG\r\n\x1a\n"


class ApiTests(unittest.TestCase):
    def test_auth_confirmation_and_exact_data(self):
        fake = FakeBrowserService()
        app = create_app(browser=fake, token="t" * 40)
        with TestClient(app) as client:
            response = client.get("/v1/nesine/status")
            self.assertEqual(response.status_code, 401)
            headers = {"Authorization": "Bearer " + "t" * 40}
            response = client.post(
                "/v1/nesine/fill",
                json={"columns": ["1" * 15]},
                headers=headers,
            )
            self.assertEqual(response.status_code, 400)
            self.assertIsNone(fake.columns)

            response = client.post(
                "/v1/nesine/fill",
                json={"columns": ["1" * 14], "same_bulletin_confirmed": True},
                headers=headers,
            )
            self.assertEqual(response.status_code, 422)

            response = client.post(
                "/v1/nesine/fill",
                json={"columns": ["1" * 14 + "0"], "same_bulletin_confirmed": True},
                headers=headers,
            )
            self.assertEqual(response.status_code, 200)
            self.assertEqual(fake.columns, ["1" * 14 + "X"])
            self.assertTrue(response.json()["verified"])
            self.assertFalse(response.json()["wager_submitted"])

            image = client.get("/v1/nesine/preview.png", headers=headers)
            self.assertEqual(image.status_code, 200)
            self.assertEqual(image.headers["content-type"], "image/png")
            self.assertEqual(image.headers["cache-control"], "no-store")


if __name__ == "__main__":
    unittest.main()
