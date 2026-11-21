# Deploy di Oracle Always Free (ARM64)

## 1. Akun & instance

1. Daftar Oracle Cloud Free Tier. Kartu diperlukan untuk verifikasi; tidak
   ditagih jika tetap dalam kuota Always Free.
2. Region dengan Ampere A1 yang masih punya kapasitas.
3. **Create Instance**:
   - Shape: `VM.Standard.A1.Flex`
   - OCPU: 4, Memory: 24 GB (batas Always Free)
   - Image: Ubuntu 24.04 (atau 22.04) aarch64
   - Boot volume: 100 GB
   - Public IPv4: assign
4. Setelah jalan, catat **public IP**.

## 2. Buka port 80

VCN → Security Lists → Default Security List → Add Ingress Rule:

- Source: `0.0.0.0/0`
- Protocol: TCP
- Destination port: `80`

Untuk HTTPS nanti tambahkan `443`.

## 3. Masuk ke server

Dari HP: Oracle Cloud Shell (browser) atau Termius/JuiceSSH.

```bash
ssh ubuntu@PUBLIC_IP
```

Upload `virtualdroid-cloud-server.zip`:

- Oracle Cloud Shell punya tombol **Upload**; upload zip, lalu
  `scp` ke instance, atau
- Termius SFTP.

```bash
unzip virtualdroid-cloud-server.zip -d vdcloud
cd vdcloud
sudo bash check-binder.sh
```

`check-binder.sh` harus menunjukkan `/dev/binder` ada. Kalau tidak, lihat
bagian Troubleshooting.

## 4. Bootstrap

```bash
sudo bash bootstrap-oracle.sh
```

Script:

- install Docker,
- memuat `binder_linux`,
- membuat user/password + bcrypt,
- build ws-scrcpy + API,
- menarik image redroid Android 16,
- menjalankan Caddy di port 80,
- mencetak URL + kredensial.

Catat password yang dicetak.

## 5. Uji server

```bash
curl -u admin:PASSWORD http://127.0.0.1/api/health
curl -u admin:PASSWORD http://127.0.0.1/api/status
```

Dari luar:

```bash
curl -u admin:PASSWORD http://PUBLIC_IP/api/health
```

## 6. Aplikasi

1. Install `VirtualDroid-Cloud-v0.1.apk`.
2. Server: `http://PUBLIC_IP`
3. User/password dari script.
4. **Cek server** → **Siapkan Android 16**.
5. Tunggu status `Android siap`.
6. **Buka Layar Android** → pilih device di ws-scrcpy.

Boot pertama redroid 2–5 menit. Image ±700 MB diunduh sekali.

## Troubleshooting

### `/dev/binder` tidak ada

```bash
sudo apt update
sudo apt install "linux-modules-extra-$(uname -r)"
sudo modprobe binder_linux devices="binder,hwbinder,vndbinder"
```

Kalau paket tidak ada (kernel Oracle), pasang kernel generic Ubuntu:

```bash
sudo apt install linux-generic
sudo reboot
```

Setelah reboot, cek `uname -r` (generic) dan ulangi `check-binder.sh`.
Jika masih gagal, opsi lain: VPS dengan kernel binder, atau GCE dengan
nested KVM + Android emulator.

### Container android restart

```bash
docker logs vdroid-android --tail 100
docker logs vdroid-web --tail 100
```

Biasanya binder belum ada atau image belum selesai pull.

### Layar putih / device tidak muncul

```bash
docker exec vdroid-web adb devices
docker restart vdroid-web
```

### Port 80 tidak bisa diakses

- Security List sudah TCP 80?
- `sudo iptables -L INPUT -n | grep 80`
- `docker ps` — caddy jalan?

## Update

```bash
cd vdcloud
docker compose pull
docker compose up -d --build
```

Data Android ada di `server/data/` (bind mount). Backup folder itu.
