#!/bin/sh
set -e
adb start-server
( while true; do adb connect "${ADB_HOST:-android}:5555" >/dev/null 2>&1 || true; sleep 5; done ) &
exec npm start
