#!/usr/bin/env bash
# Toto Nesine bridge installer for Ubuntu/Debian. Run as a normal Linux user.
set -euo pipefail
BridgeRoot="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BridgePython="$BridgeRoot/.venv/bin/python"

if [ "$(id -u)" -eq 0 ]; then
  echo "Run as your normal SSH user (not root). sudo will be requested for apt."
  exit 1
fi
if ! command -v apt-get >/dev/null 2>&1; then
  echo "This installer targets Ubuntu/Debian. Use manual steps for other Linux."
  exit 1
fi
echo "Installing Python and one-time private Chrome login tools..."
sudo apt-get update
sudo apt-get install -y python3 python3-venv python3-pip xvfb x11vnc novnc websockify

if ! python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 11) else 1)'; then
  echo "Python 3.11+ required; install a newer Python and rerun."
  exit 1
fi
if [ ! -x "$BridgePython" ]; then
  python3 -m venv "$BridgeRoot/.venv"
fi
"$BridgePython" -m pip install -r "$BridgeRoot/nesine_bridge/requirements.txt"
"$BridgePython" -m playwright install --with-deps chrome
cd "$BridgeRoot"
"$BridgePython" -m unittest discover -s tests -p test_nesine_bridge.py
echo
echo "Setup complete. On a headless SSH server, run: bash linux/02-giris-ssh.sh"

