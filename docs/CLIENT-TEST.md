# Uji client (AVD API 30)

## Lingkungan

- Emulator API 30 ATD x86, TCG tanpa KVM, 1 GB/2 core.
- Mock server di host (`10.0.2.2:8000`) menyajikan:
  - `/api/status` → `{"status":"ready",...}`
  - `/` → halaman "ANDROID 16 / MOCK STREAM"

## Hasil

1. APK install.
2. Isi server `http://10.0.2.2:8000`.
3. **Cek server**:

```
Android siap · Mock server siap
```

4. **Buka Layar Android** → WebView fullscreen:

```
ANDROID 16
MOCK STREAM
Halaman uji WebView
```

5. Tombol × menutup layar.

## Catatan

- Di TCG, WebView sempat memicu ANR 5 detik saat start; pilih "Wait" dan
  halaman termuat. Di HP nyata WebView init jauh lebih cepat.
- Bug yang diperbaiki: `getWindow().getInsetsController()` null jika dipanggil
  sebelum `setContentView`; sekarang immersive dipanggil setelah content.
- Backend nyata (redroid/ws-scrcpy) tidak bisa diuji di sandbox ini karena
  tidak ada Docker/binder. Client diuji terhadap mock.
