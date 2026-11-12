"""Use python -m nesine_bridge login or python -m nesine_bridge serve."""

from __future__ import annotations

import argparse
import os
from pathlib import Path

from .browser import NESINE_URL, NesineBrowser


def main() -> None:
    parser = argparse.ArgumentParser(description="Toto Nesine Chrome köprüsü")
    parser.add_argument("command", choices=["login", "serve"])
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()

    if args.command == "login":
        profile = Path(os.environ.get("TOTO_NESINE_CHROME_PROFILE", "~/.toto-nesine-chrome"))
        chrome = NesineBrowser(
            profile,
            headless=False,
            chrome_channel=os.environ.get("TOTO_NESINE_CHROME_CHANNEL", "chrome"),
        )
        chrome.start()
        try:
            print(f"Resmî Nesine sitesi açık: {NESINE_URL}")
            print("Girişinizi Chrome penceresinde kendiniz yapın.")
            print("Güvenlik kodu ve iki aşamalı doğrulamayı normal şekilde tamamlayın.")
            input("Giriş tamamlandıktan sonra burada Enter'a basın ve Chrome'u kapatın...")
        finally:
            chrome.stop()
        print("Chrome profilindeki oturum sonraki çalıştırmada yeniden kullanılacak.")
    else:
        if args.host not in {"127.0.0.1", "::1", "localhost"}:
            print(
                "DİKKAT: İnternete açık bir sunucu için TLS/HTTPS ters vekili "
                "ve ağ erişim kısıtlaması kullanın."
            )
        import uvicorn

        uvicorn.run(
            "nesine_bridge.api:app",
            host=args.host,
            port=args.port,
            workers=1,
            access_log=False,
        )


if __name__ == "__main__":
    main()

