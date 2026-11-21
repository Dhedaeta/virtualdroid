# Batas & risiko

## Tidak ada GApps

redroid tidak menyertakan Play Services/Play Store.

- Pakai **Aurora Store** (sideload APK) untuk mengambil aplikasi publik.
- Aplikasi yang butuh Play Services bisa crash.
- **Play Integrity** (`DEVICE`/`STRONG`) gagal. Aplikasi bank, Google Pay,
  dan game anti-cheat tertentu menolak.

## GPU

Rendering software (`redroid_gpu_mode=guest`). UI ringan jalan; game 3D berat
tidak realistis. Oracle Ampere tidak punya GPU.

## Android 16

Image `redroid/redroid:16.0.0_64only-latest` (arm64). Ini Android 16 AOSP,
bukan Pixel ROM. Beberapa fitur vendor tidak ada.

## Root

Container berjalan privileged; `adb root` tersedia. Ini root container,
bukan Magisk di boot image. Modul Magisk/Zygisk tidak tersedia secara default.

## Satu instance

Compose default satu container Android. Multi instance = tambah service
redroid dengan port/volume berbeda; RAM 24 GB cukup untuk 3–4 instance
ringan, tetapi CPU software rendering membatasi.

## Keamanan

- Basic auth HTTP tanpa TLS: password dan stream bisa disadap.
- Gunakan domain + HTTPS atau VPN.
- API memegang `docker.sock` (root-equivalent). Jangan expose API langsung.
- Jangan buka adb 5555 ke internet.

## Biaya

Oracle Always Free gratis dalam kuota, tetapi:
- kartu diperlukan untuk verifikasi;
- kapasitas Ampere sering penuh;
- egress 10 TB/bulan (cukup untuk streaming ringan).

## Kenapa bukan APK lokal

VMOS/VPhoneGaga menjalankan Android penuh dengan container/hypervisor khusus.
APK biasa di HP non-root tidak boleh membuat VM/KVM atau memuat kernel tamu.
QEMU lokal bisa, tetapi di HP 4 GB hanya 1–5 FPS. Cloud memberi performa
real dengan biaya server.
