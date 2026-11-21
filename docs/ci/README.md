# GitHub Actions

Workflow ini belum ada di `.github/workflows/` karena GitHub App yang
digunakan untuk push tidak punya izin `workflows`.

Untuk mengaktifkan:

1. Beri izin **Workflows: Read and write** ke Moclaw GitHub App
   (GitHub → Settings → Applications → Moclaw → Permissions), atau
2. buat file `.github/workflows/build-client.yml` manual dan salin isi
   `build-client.yml` di folder ini.

`build-client.yml` menghasilkan artifact APK dari `client/build.sh`.
