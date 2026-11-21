#!/usr/bin/env bash
#
# Build APK client cloud phone tanpa Gradle.
#   ANDROID_HOME=/path/sdk ./build.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
BT="${ANDROID_BUILD_TOOLS:-$SDK/build-tools/36.0.0}"
PLATFORM="$SDK/platforms/android-36/android.jar"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
KEYTOOL="${JAVA_HOME:+$JAVA_HOME/bin/}keytool"

die() { echo "ERROR: $*" >&2; exit 1; }
[ -n "$SDK" ] || die "Set ANDROID_HOME"
[ -x "$BT/aapt2" ] || die "build-tools tidak ada di $BT"
[ -f "$PLATFORM" ] || die "android.jar tidak ada"
[ -f "$ROOT/app/AndroidManifest.xml" ] || die "manifest tidak ada"

B="$ROOT/build"
D="$ROOT/dist"
KS="$ROOT/.keys/debug.keystore"
rm -rf "$B" "$D"
mkdir -p "$B" "$D" "$ROOT/.keys"

if [ ! -f "$KS" ]; then
  "$KEYTOOL" -genkeypair -keystore "$KS" -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi

mkdir -p "$B/classes" "$B/dex" "$B/gen"
"$BT/aapt2" compile --dir "$ROOT/app/res" -o "$B/res.zip"
"$BT/aapt2" link -o "$B/base.apk" -I "$PLATFORM" \
  --manifest "$ROOT/app/AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 36 \
  --auto-add-overlay -R "$B/res.zip" --java "$B/gen"

find "$ROOT/app/src" "$B/gen" -name '*.java' | sort > "$B/sources.txt"
"$JAVAC" -encoding UTF-8 --release 8 -classpath "$PLATFORM" \
  -d "$B/classes" @"$B/sources.txt"

find "$B/classes" -name '*.class' | sort > "$B/classes.txt"
# shellcheck disable=SC2046
"$BT/d8" --min-api 26 --lib "$PLATFORM" --output "$B/dex" $(cat "$B/classes.txt")

( cd "$B/dex" && zip -q -X "$B/base.apk" classes.dex )
"$BT/zipalign" -f 4 "$B/base.apk" "$B/aligned.apk"
OUT="$D/VirtualDroid-Cloud-v0.1.apk"
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey --out "$OUT" "$B/aligned.apk"
"$BT/apksigner" verify --print-certs "$OUT" | head -2

{
  echo "VirtualDroid Cloud client 0.1.0"
  echo "built: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  sha256sum "$OUT"
} | tee "$D/BUILD-INFO.txt"

echo "OK -> $OUT"
