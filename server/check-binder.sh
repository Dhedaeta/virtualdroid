#!/usr/bin/env bash
# Cek apakah kernel menyediakan binder untuk redroid.
set -u
echo "kernel : $(uname -r)"
echo "arch   : $(uname -m)"
echo

if [ -e /dev/binder ]; then
  echo "OK: /dev/binder ada"
else
  echo "!! /dev/binder belum ada"
fi

if lsmod 2>/dev/null | grep -q binder; then
  lsmod | grep binder
else
  echo "modul binder belum terlihat di lsmod"
fi

echo
echo "Coba muat modul (butuh root):"
if command -v modprobe >/dev/null 2>&1; then
  sudo modprobe binder_linux devices="binder,hwbinder,vndbinder" 2>&1 || true
  sudo modprobe ashmem_linux 2>&1 || true
else
  echo "modprobe tidak ada"
fi

if [ ! -e /dev/binder ]; then
  sudo mkdir -p /dev/binderfs 2>/dev/null || true
  sudo mount -t binder binder /dev/binderfs 2>/dev/null || true
  for n in binder hwbinder vndbinder; do
    [ -e "/dev/binderfs/$n" ] && sudo ln -sf "/dev/binderfs/$n" "/dev/$n"
  done
fi

echo
ls -l /dev/binder /dev/hwbinder /dev/vndbinder 2>&1
