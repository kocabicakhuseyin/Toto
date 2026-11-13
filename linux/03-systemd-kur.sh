#!/usr/bin/env bash
# Install a per-user background service; the token is never in the repository.
set -euo pipefail
BridgeRoot="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BridgePython="$BridgeRoot/.venv/bin/python"
BridgeUserName="$(id -un)"
BridgeUserHome="$(getent passwd "$BridgeUserName" | cut -d: -f6)"
BridgeConfigDir="${XDG_CONFIG_HOME:-$BridgeUserHome/.config}/toto-nesine-bridge"
BridgeUnitDir="$BridgeUserHome/.config/systemd/user"
BridgeEnvFile="$BridgeConfigDir/bridge.env"
BridgeUnitFile="$BridgeUnitDir/toto-nesine-bridge.service"

if [ "$(id -u)" -eq 0 ]; then
  echo "Run as the SAME regular Linux user used for the Chrome login."
  exit 1
fi
if [ ! -x "$BridgePython" ]; then
  echo "First run: bash linux/01-kur-ubuntu.sh"
  exit 1
fi
if ! command -v systemctl >/dev/null 2>&1; then
  echo "systemd required for this installer."
  exit 1
fi

install -d -m 700 "$BridgeConfigDir"
install -d -m 700 "$BridgeUnitDir"
if [ ! -f "$BridgeEnvFile" ]; then
  BridgeToken="$("$BridgePython" -c 'import secrets; print(secrets.token_urlsafe(36))')"
  (umask 077; printf 'TOTO_NESINE_BRIDGE_TOKEN=%s\n' "$BridgeToken" > "$BridgeEnvFile")
fi
chmod 600 "$BridgeEnvFile"
BridgeToken="$(sed -n 's/^TOTO_NESINE_BRIDGE_TOKEN=//p' "$BridgeEnvFile" | head -1)"
if [ "${#BridgeToken}" -lt 32 ]; then
  echo "The server token must be at least 32 characters long."
  exit 1
fi

cat > "$BridgeUnitFile" <<EOF
[Unit]
Description=Toto Nesine user-owned Chrome bridge
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=$BridgeRoot
ExecStart="$BridgePython" -m nesine_bridge serve --host 127.0.0.1 --port 8765
EnvironmentFile=$BridgeEnvFile
Environment=TOTO_NESINE_HEADLESS=1
Restart=on-failure
RestartSec=6
NoNewPrivileges=true
PrivateTmp=true
UMask=0077

[Install]
WantedBy=default.target
EOF
chmod 600 "$BridgeUnitFile"
systemctl --user daemon-reload
systemctl --user enable --now toto-nesine-bridge.service

echo
echo "Service state: $(systemctl --user is-active toto-nesine-bridge.service)"
echo "SECRET ACCESS TOKEN: enter ONLY in the Toto Android app."
printf '%s\n' "$BridgeToken"
echo
echo "For boot auto-start (optional): sudo loginctl enable-linger $BridgeUserName"
echo "To inspect errors: journalctl --user -u toto-nesine-bridge.service -n 50"
echo "Next: bash linux/04-tailnet-https.sh"

