import os
import subprocess
import threading
import time

from flask import Flask, jsonify

app = Flask(__name__)

COMPOSE = os.environ.get("COMPOSE_FILE", "/opt/vdroid/docker-compose.yml")
LOCK = threading.Lock()

state = {
    "status": "idle",
    "message": "Belum disiapkan",
    "updated": 0,
}


def set_state(status, message):
    with LOCK:
        state["status"] = status
        state["message"] = message
        state["updated"] = int(time.time())


def compose(*args, timeout=1800):
    cmd = ["docker-compose", "-f", COMPOSE] + list(args)
    return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)


def provision_worker():
    try:
        set_state("pulling", "Mengunduh image Android 16 (sekitar 700 MB)...")
        p = compose("pull", "android")
        if p.returncode != 0:
            set_state("error", (p.stderr or p.stdout)[-800:])
            return

        set_state("starting", "Menjalankan Android...")
        p = compose("up", "-d", "android", "web")
        if p.returncode != 0:
            set_state("error", (p.stderr or p.stdout)[-800:])
            return

        set_state("starting", "Menunggu Android boot...")
        for _ in range(90):
            time.sleep(4)
            p = compose("ps", "-q", "android")
            if p.returncode == 0 and p.stdout.strip():
                set_state("ready", "Android siap. Buka layar.")
                return
        set_state("error", "Container jalan tapi Android belum siap. Cek logs.")
    except Exception as e:
        set_state("error", str(e)[-800:])


def refresh():
    if state["status"] in ("pulling", "starting"):
        return
    try:
        p = compose("ps", "-q", "android", timeout=30)
        if p.returncode == 0 and p.stdout.strip():
            if state["status"] != "ready":
                set_state("ready", "Android siap. Buka layar.")
        elif state["status"] == "ready":
            set_state("idle", "Android berhenti")
    except Exception:
        pass


@app.get("/api/health")
def health():
    return jsonify({"ok": True})


@app.get("/api/status")
def status():
    refresh()
    with LOCK:
        return jsonify(dict(state))


@app.post("/api/provision")
def provision():
    if state["status"] in ("pulling", "starting"):
        with LOCK:
            return jsonify(dict(state))
    threading.Thread(target=provision_worker, daemon=True).start()
    set_state("pulling", "Menyiapkan...")
    return jsonify({"status": "pulling"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8081)
