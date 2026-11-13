#!/usr/bin/env bash
# HTTPS access ONLY inside your private Tailscale network. Never public Funnel.
set -euo pipefail
if ! command -v tailscale >/dev/null 2>&1; then
  echo "Install Tailscale using https://tailscale.com/docs/install/linux"
  echo "Then run: sudo tailscale up"
  exit 1
fi
if ! sudo tailscale status >/dev/null 2>&1; then
  echo "Tailscale is not connected. Run: sudo tailscale up"
  exit 1
fi
sudo tailscale serve --bg 8765
echo
sudo tailscale serve status
echo
echo "Install Tailscale on Android and sign in to the SAME tailnet."
echo "In Toto > Nesine Chrome Server, use the HTTPS ts.net address above."
echo "Enter the secret access token from linux/03-systemd-kur.sh."
echo "This is private Tailscale Serve, NOT public Funnel."

