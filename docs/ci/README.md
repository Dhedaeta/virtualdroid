# GitHub Actions

`build-client.yml` lama memakai `android-actions/setup-android@v3` dan gagal di
runner (step setup SDK). Gunakan **`build-client-manual.yml`** yang mengunduh
SDK langsung, dan build `allinone/` (satu APK).

Workflow belum bisa di-push oleh Zentor GitHub App karena izin `workflows`
belum aktif. Tambahkan manual:

1. Buka repo → Add file → Create new file
2. Path: `.github/workflows/build-allinone.yml`
3. Salin isi `docs/ci/build-client-manual.yml`
4. Commit. Actions akan build `allinone/dist/VirtualDroid-AllInOne-v0.6.apk`.
