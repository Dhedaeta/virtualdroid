# Arsitektur

```
┌────────────── HP ──────────────┐
│ VirtualDroid Cloud APK         │
│  MainActivity  (setup/status)  │
│  CloudActivity (WebView)       │
└───────────────┬────────────────┘
                │ HTTP + WebSocket (Basic auth)
┌───────────────▼────────────────┐
│ Caddy :80                      │
│  /api/*  → API :8081           │
│  /       → ws-scrcpy :8000     │
└───────┬───────────────┬────────┘
        │               │
┌───────▼──────┐  ┌─────▼──────────────┐
│ API Flask    │  │ ws-scrcpy (Node)    │
│ docker.sock  │  │ adb → android:5555  │
└───────┬──────┘  └─────┬──────────────┘
        │               │
┌───────▼───────────────▼──────────┐
│ redroid Android 16 (arm64)        │
│ privileged, /dev/binder, /data    │
└───────────────────────────────────┘
```

## Alur provisioning

1. App `POST /api/provision`.
2. API `docker compose pull android` (image Android 16, ±700 MB).
3. API `docker compose up -d android web`.
4. API polling container; status `starting` → `ready`.
5. App `GET /api/status` tiap 3 detik.
6. App membuka `http://server/` di WebView.
7. ws-scrcpy mendeteksi device lewat `adb connect android:5555`.
8. Video (H.264/WebCodecs) + input sentuh dikirim lewat WebSocket.

## Port

| Port | Akses | Fungsi |
|---|---|---|
| 80 | publik | Caddy (auth + reverse proxy) |
| 8000 | internal | ws-scrcpy |
| 8081 | internal | API provisioning |
| 5555 | 127.0.0.1 | adb redroid |

Jangan buka 5555/8000/8081 ke publik.

## Data

- `server/data/` → `/data` redroid (user apps, akun).
- `docker volume caddy_data` → sertifikat (jika HTTPS).
- `.env` → kredensial.

## Auth

Caddy `basic_auth` melindungi semua route. App mengirim header Basic untuk
API dan mengisi dialog auth WebView untuk halaman/stream.

Untuk produksi: domain + HTTPS (Caddy automatic), atau Tailscale/WireGuard.
