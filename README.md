# VirtualDroid Cloud

Repo: https://github.com/Dhedaeta/virtualdroid

HP Android 16 **asli** yang berjalan di server, tampil penuh di HP Anda.
Bukan guest Activity, bukan UI palsu: container Android 16 (redroid) + stream
layar lewat ws-scrcpy.

```
HP (APK client)  ──HTTP/WebSocket──►  Caddy (auth)
                                        ├── ws-scrcpy  ← adb ── redroid Android 16
                                        └── API provisioning (pull image, start)
```

Server: Oracle Always Free Ampere A1 (ARM64, 4 OCPU/24 GB) atau VPS Linux
dengan kernel binder. Client: APK Android 8+.

## Yang real

- Android 16 (redroid `16.0.0_64only`, arm64) berjalan di container privileged.
- Layar + sentuhan asli lewat ws-scrcpy di WebView.
- Root container (`adb root`), bukan emulasi.
- Image/ROM diunduh **di server** saat tombol "Siapkan Android 16".
- Satu server bisa jadi beberapa instance (tambah container, lihat docs).

## Yang tidak

- Tanpa GApps: Play Store tidak ada; pakai Aurora Store. Play Integrity gagal.
- GPU software; game 3D berat tidak realistis.
- Butuh VPS/cloud. Tidak ada cloud Android selalu-aktif gratis tanpa kartu.
- Basic auth HTTP; gunakan domain+HTTPS atau VPN untuk produksi.

## Mulai

### 1. Server

Ikuti `docs/DEPLOY-ORACLE.md`. Ringkas:

```bash
# di instance Ubuntu ARM64
unzip virtualdroid-cloud-server.zip -d vdcloud
cd vdcloud
sudo bash bootstrap-oracle.sh
```

Buka port 80 di Oracle Security List. Script mencetak URL + user/password.

### 2. Client

Install `VirtualDroid-Cloud-v0.1.apk`, isi:

- Server: `http://IP-VPS`
- User / password dari script

Tekan **Cek server** → **Siapkan Android 16** (unduh image ±700 MB, boot 2–5
menit) → **Buka Layar Android**.

## Uji client

Client diuji di AVD API 30 dengan mock server:

```
Android siap · Mock server siap
WebView: ANDROID 16 / MOCK STREAM
```

Lihat `docs/CLIENT-TEST.md`. Backend tidak bisa diuji di sandbox ini karena
tidak ada Docker/binder; lihat `docs/LIMITS.md`.

## Struktur

```
client/   APK client (Java, tanpa AndroidX)
server/   docker-compose redroid + ws-scrcpy + Caddy + API
docs/     deploy Oracle, arsitektur, batas, hasil uji
```

## Biaya

Oracle Always Free: 4 OCPU Ampere + 24 GB RAM + 200 GB disk, gratis dalam
kuota (kartu diperlukan untuk verifikasi). Kapasitas ARM sering penuh;
coba beberapa availability domain.
