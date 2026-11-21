# All-in-One APK

Satu APK berisi:

1. **Cloud** — client Android 16 dari server (redroid + ws-scrcpy).
2. **Mode Lokal** — guest engine v0.5 (StubActivity + resource proxy) dengan
   `guest.apk` tertanam di assets.

Tidak perlu install APK kedua. Server tetap terpisah karena Android 16
berjalan di VPS, bukan di dalam APK.

Build: `ANDROID_HOME=... ./build.sh`
