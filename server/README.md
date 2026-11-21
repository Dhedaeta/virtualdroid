# Server kit

```bash
unzip virtualdroid-cloud-server.zip -d vdcloud
cd vdcloud
sudo bash check-binder.sh
sudo bash bootstrap-oracle.sh
```

Buka TCP 80 di firewall cloud. Script mencetak URL + kredensial.

File:

- `docker-compose.yml` — redroid Android 16, ws-scrcpy, API, Caddy
- `bootstrap-oracle.sh` — install Docker, binder, build, start
- `check-binder.sh` — diagnosa `/dev/binder`
- `web/` — ws-scrcpy (Node)
- `api/` — provisioning Flask
- `Caddyfile` — basic auth + reverse proxy

Jangan expose port 5555/8000/8081.
