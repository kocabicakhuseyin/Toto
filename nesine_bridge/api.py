"""Token-protected API between the Toto Android app and user's own computer.

Run behind HTTPS when accessed from a telephone. Chrome profile stays on the
computer; no Nesine username/password is accepted by this service.
"""

from __future__ import annotations

import asyncio
import hmac
import os
from concurrent.futures import ThreadPoolExecutor
from contextlib import asynccontextmanager
from functools import partial
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException, Response
from pydantic import BaseModel, Field

from .browser import NesineBrowser, normalize_columns


class FillRequest(BaseModel):
    columns: list[str] = Field(min_length=1, max_length=4)
    same_bulletin_confirmed: bool = False


def create_app(browser: NesineBrowser | None = None, token: str | None = None) -> FastAPI:
    access_token = token if token is not None else os.environ.get("TOTO_NESINE_BRIDGE_TOKEN", "")
    profile = Path(
        os.environ.get("TOTO_NESINE_CHROME_PROFILE", "~/.toto-nesine-chrome")
    )
    chrome = browser or NesineBrowser(
        profile,
        headless=os.environ.get("TOTO_NESINE_HEADLESS", "1") != "0",
        chrome_channel=os.environ.get("TOTO_NESINE_CHROME_CHANNEL", "chrome"),
    )
    # Playwright sync objects must never move between FastAPI worker threads.
    worker = ThreadPoolExecutor(max_workers=1, thread_name_prefix="nesine-chrome")

    async def on_browser_thread(func, *args):
        loop = asyncio.get_running_loop()
        return await loop.run_in_executor(worker, partial(func, *args))

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        if len(access_token) < 32:
            worker.shutdown(wait=False)
            raise RuntimeError(
                "TOTO_NESINE_BRIDGE_TOKEN en az 32 karakter olmalı. "
                "Önce güvenli bir erişim anahtarı tanımlayın."
            )
        await on_browser_thread(chrome.start)
        try:
            yield
        finally:
            await on_browser_thread(chrome.stop)
            worker.shutdown(wait=True)

    app = FastAPI(
        title="Toto - Nesine Chrome Köprüsü",
        description="Yalnızca kullanıcıya ait Chrome oturumunda kupon alanlarını doldurur.",
        version="0.1.0",
        docs_url=None,
        redoc_url=None,
        lifespan=lifespan,
    )

    @app.middleware("http")
    async def no_cache(request, call_next):
        response = await call_next(request)
        response.headers["Cache-Control"] = "no-store"
        response.headers["X-Content-Type-Options"] = "nosniff"
        return response

    def require_token(authorization: str | None = Header(default=None)) -> None:
        given = (authorization or "").removeprefix("Bearer ").strip()
        if not access_token or not hmac.compare_digest(given, access_token):
            raise HTTPException(status_code=401, detail="Erişim anahtarı geçersiz.")

    @app.get("/v1/nesine/status", dependencies=[Depends(require_token)])
    async def status():
        return await on_browser_thread(chrome.status)

    @app.post("/v1/nesine/fill", dependencies=[Depends(require_token)])
    async def fill(request: FillRequest):
        if not request.same_bulletin_confirmed:
            raise HTTPException(
                status_code=400,
                detail="Toto ve Nesine maç programının aynı hafta olduğunu önce onaylayın.",
            )
        try:
            columns = normalize_columns(request.columns)
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        try:
            result = await on_browser_thread(chrome.fill, columns)
        except Exception as exc:
            # Never echo URLs containing possible private parameters or passwords.
            raise HTTPException(
                status_code=503,
                detail="Chrome kupon alanlarını dolduramadı/doğrulayamadı. "
                "Nesine sayfasını ve açık Chrome oturumunu kontrol edin.",
            ) from exc
        return result.as_dict()

    @app.get("/v1/nesine/preview.png", dependencies=[Depends(require_token)])
    async def preview():
        try:
            data = await on_browser_thread(chrome.preview_png)
        except Exception as exc:
            raise HTTPException(status_code=503, detail="Chrome önizlemesi alınamadı.") from exc
        return Response(
            content=data,
            media_type="image/png",
            headers={"Content-Disposition": "inline; filename=nesine-preview.png"},
        )

    return app


app = create_app()

