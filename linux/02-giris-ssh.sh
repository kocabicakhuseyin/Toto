#!/usr/bin/env bash
# ONE-TIME official Nesine login on a headless SSH server.
# VNC/noVNC listen ONLY on 127.0.0.1; use SSH port forwarding.
# Never expose TCP 5901 or 6080 publicly.
set -euo pipefail
BridgeRoot="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BridgePython="$BridgeRoot/.venv/bin/python"
BridgeDisplay=":98"
BridgeX11Socket="/tmp/.X11-unix/X98"
BridgeVncPid=""
BridgeWebPid=""
BridgeXvfbPid=""
BridgeVncPassFile=""

cleanup() {
  for BridgePid in "$BridgeWebPid" "$BridgeVncPid" "$BridgeXvfbPid"; do
    if [ -n "$BridgePid" ]; then
      kill "$BridgePid" 2>/dev/null || true
      wait "$BridgePid" 2>/dev/null || true
    fi
  done
  if [ -n "$BridgeVncPassFile" ]; then
    rm -f -- "$BridgeVncPassFile"
  fi
}
trap cleanup EXIT INT TERM

if [ ! -x "$BridgePython" ]; then
  echo "First run: bash linux/01-kur-ubuntu.sh"
  exit 1
fi
if [ -e "$BridgeX11Socket" ]; then
  echo "Virtual display :98 is already in use; stop the other session first."
  exit 1
fi
for BridgeTool in Xvfb x11vnc websockify; do
  if ! command -v "$BridgeTool" >/dev/null 2>&1; then
    echo "Missing $BridgeTool. Run linux/01-kur-ubuntu.sh."
    exit 1
  fi
done

Xvfb "$BridgeDisplay" -screen 0 1366x900x24 -nolisten tcp > /dev/null 2>&1 &
BridgeXvfbPid=$!
sleep 2
if ! kill -0 "$BridgeXvfbPid" 2>/dev/null; then
  echo "Could not start the virtual display."
  exit 1
fi
BridgeVncPassFile="$(mktemp)"
chmod 600 "$BridgeVncPassFile"
BridgeVncPassword="$("$BridgePython" -c 'import secrets, string; alphabet=string.ascii_letters+string.digits; print("".join(secrets.choice(alphabet) for _ in range(8)))')"
printf '%s\n' "$BridgeVncPassword" > "$BridgeVncPassFile"
x11vnc -display "$BridgeDisplay" -localhost -rfbport 5901 \
  -passwdfile "$BridgeVncPassFile" \
  -forever -shared -quiet > /dev/null 2>&1 &
BridgeVncPid=$!
websockify --web /usr/share/novnc 127.0.0.1:6080 127.0.0.1:5901 \
  > /dev/null 2>&1 &
BridgeWebPid=$!
sleep 2
if ! kill -0 "$BridgeVncPid" 2>/dev/null || ! kill -0 "$BridgeWebPid" 2>/dev/null; then
  echo "Private VNC login screen failed to start. Check local port 5901/6080."
  exit 1
fi

echo
echo "On YOUR PC open a SECOND terminal and establish an SSH tunnel:"
echo "  ssh -N -L 6080:127.0.0.1:6080 USER@YOUR_SERVER"
echo "Then open http://127.0.0.1:6080/vnc.html on your PC."
echo "One-time private VNC password: $BridgeVncPassword"
echo "Use this private browser screen to sign in ONLY at official Nesine."
echo "After login, press Enter in the server SSH terminal to close the session."
echo
cd "$BridgeRoot"
DISPLAY="$BridgeDisplay" "$BridgePython" -m nesine_bridge login
echo "Chrome profile saved under the current Linux user; private VNC is closing."

