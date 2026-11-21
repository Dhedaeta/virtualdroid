# Client APK

Install `VirtualDroid-Cloud-v0.1.apk`, isi server URL + user/password dari
`bootstrap-oracle.sh`.

- **Cek server** → `GET /api/status`
- **Siapkan Android 16** → `POST /api/provision` (server pull image + start)
- **Buka Layar Android** → WebView ws-scrcpy fullscreen

Build:

```bash
ANDROID_HOME=/path/sdk ./build.sh
```
