#!/usr/bin/env bash
#
# Deploy VirtualDroid Cloud di Oracle Always Free (Ampere ARM64, Ubuntu).
# Jalankan sebagai root: sudo bash bootstrap-oracle.sh
#
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

[ "$(id -u)" = "0" ] || { echo "Jalankan dengan sudo." >&2; exit 1; }

echo "== 1/6 paket dasar =="
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y curl ca-certificates openssl iptables

echo "== 2/6 docker =="
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

echo "== 3/6 binder =="
apt-get install -y "linux-modules-extra-$(uname -r)" 2>/dev/null || true
modprobe binder_linux devices="binder,hwbinder,vndbinder" 2>/dev/null || true
modprobe ashmem_linux 2>/dev/null || true

if [ ! -e /dev/binder ]; then
  mkdir -p /dev/binderfs
  mount -t binder binder /dev/binderfs 2>/dev/null || true
  for n in binder hwbinder vndbinder; do
    [ -e "/dev/binderfs/$n" ] && ln -sf "/dev/binderfs/$n" "/dev/$n"
  done
fi

if [ ! -e /dev/binder ]; then
  cat >&2 <<'MSG'
✗ Kernel ini tidak menyediakan /dev/binder.
  Redroid tidak bisa jalan tanpa binder.
  Coba:
    sudo apt install linux-modules-extra-$(uname -r)
    sudo modprobe binder_linux devices="binder,hwbinder,vndbinder"
  Kalau tetap gagal, pakai kernel Ubuntu generic (bukan kernel Oracle)
  atau jalur GCE dengan nested KVM + Android emulator.
MSG
  exit 2
fi
echo "binder OK: $(ls -l /dev/binder)"

echo "== 4/6 kredensial =="
if [ ! -f .env ]; then
  PASS="$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 16)"
  HASH="$(docker run --rm caddy:2.8 caddy hash-password --plaintext "$PASS" | tail -1)"
  cat > .env <<ENV
REDROID_IMAGE=redroid/redroid:16.0.0_64only-latest
VDROID_USER=admin
VDROID_PASS=$PASS
VDROID_HASH='$HASH'
ENV
  chmod 600 .env
fi
# shellcheck disable=SC1091
. ./.env

echo "== 5/6 build + start =="
docker compose pull android caddy
docker compose up -d --build

echo "== 6/6 tunggu API =="
for i in $(seq 1 60); do
  if curl -fsS -u "$VDROID_USER:$VDROID_PASS" http://127.0.0.1/api/health >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

# Oracle images sering memblokir port 80 di iptables
iptables -I INPUT -p tcp --dport 80 -j ACCEPT 2>/dev/null || true
iptables -I INPUT -p tcp --dport 443 -j ACCEPT 2>/dev/null || true
if command -v netfilter-persistent >/dev/null 2>&1; then
  netfilter-persistent save 2>/dev/null || true
fi

IP="$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || hostname -I | awk '{print $1}')"
cat <<MSG

========================================
 VirtualDroid Cloud siap
========================================
 URL      : http://$IP/
 User     : $VDROID_USER
 Password : $VDROID_PASS

 Buka port 80 di Oracle Security List
 (VCN → Security List → Ingress 0.0.0.0/0 TCP 80).

 Di aplikasi:
   Server : http://$IP
   User   : $VDROID_USER
   Pass   : $VDROID_PASS
   → "Siapkan Android 16" → "Buka Layar"

 Catatan:
 - Image Android 16 diunduh saat provisioning (±700 MB).
 - Boot pertama 2–5 menit.
 - Tanpa GApps; pakai Aurora Store. Play Integrity tidak lulus.
========================================
MSG
