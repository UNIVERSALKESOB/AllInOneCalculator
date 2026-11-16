#!/data/data/com.termux/files/usr/bin/bash
# Builds a signed, installable APK with the plain SDK tools (no Gradle).
set -e

cd "$(dirname "$0")"
PROJECT_DIR="$PWD"
ANDROID_JAR="${ANDROID_JAR:-$HOME/sdk/android-34-ext12/android.jar}"
APP_NAME="AllInOneCalculator"
VERSION_NAME="1.1"
VERSION_CODE="2"
MIN_SDK="21"
TARGET_SDK="34"

if [ ! -f "$ANDROID_JAR" ]; then
  echo "android.jar not found at $ANDROID_JAR" >&2
  exit 1
fi

echo "==> cleaning"
rm -rf build out
mkdir -p build/gen build/classes build/dex out

echo "==> resources"
aapt2 compile --dir res -o build/res.zip
aapt2 link -o build/base.apk \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -R build/res.zip \
  --java build/gen \
  --min-sdk-version "$MIN_SDK" \
  --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay

echo "==> javac"
find src build/gen -name '*.java' > build/sources.txt
javac -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" \
  -d build/classes \
  @build/sources.txt

if [ ! -f build/classes/com/allinone/calc/MainActivity.class ]; then
  echo "javac did not produce classes" >&2
  exit 1
fi

echo "==> dex"
jar cf build/classes.jar -C build/classes .
d8 --release --min-api "$MIN_SDK" --lib "$ANDROID_JAR" \
  --output build/dex build/classes.jar

echo "==> package"
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -j ../unsigned.apk classes.dex)

echo "==> align"
if command -v zipalign >/dev/null 2>&1; then
  zipalign -f -p 4 build/unsigned.apk build/aligned.apk
else
  python3 tools/zipalign.py build/unsigned.apk build/aligned.apk
fi

echo "==> sign"
KEY_ALIAS="${KEY_ALIAS:-calc}"
KEYSTORE="${KEYSTORE_PATH:-keys/release.jks}"
PASS_FILE="keys/password.txt"

if [ -n "${KEYSTORE_BASE64:-}" ]; then
  mkdir -p build
  printf '%s' "$KEYSTORE_BASE64" | base64 -d > build/ci-keystore.jks
  KEYSTORE="build/ci-keystore.jks"
fi

KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-}"
if [ -z "$KEYSTORE_PASSWORD" ] && [ -f "$PASS_FILE" ]; then
  KEYSTORE_PASSWORD="$(cat "$PASS_FILE")"
fi

if [ -z "$KEYSTORE_PASSWORD" ]; then
  if [ -f "$KEYSTORE" ]; then
    echo "Keystore exists but password is missing: set KEYSTORE_PASSWORD or create $PASS_FILE" >&2
    exit 1
  fi
  KEYSTORE_PASSWORD="$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)"
  mkdir -p keys
  printf '%s' "$KEYSTORE_PASSWORD" > "$PASS_FILE"
  echo "Generated a new signing password in $PASS_FILE"
fi

KEY_PASSWORD="${KEY_PASSWORD:-$KEYSTORE_PASSWORD}"

if [ ! -f "$KEYSTORE" ]; then
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair -keystore "$KEYSTORE" -alias "$KEY_ALIAS" \
    -storepass "$KEYSTORE_PASSWORD" -keypass "$KEY_PASSWORD" \
    -keyalg RSA -keysize 4096 -validity 10950 \
    -dname "CN=All-in-One Calculator, O=UNIVERSALKESOB, C=BD"
fi

apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$KEYSTORE_PASSWORD" \
  --key-pass "pass:$KEY_PASSWORD" \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "out/${APP_NAME}.apk" \
  build/aligned.apk

echo "==> verify"
apksigner verify --print-certs "out/${APP_NAME}.apk" | head -5
ls -lh "out/${APP_NAME}.apk"
echo "BUILD OK: $PROJECT_DIR/out/${APP_NAME}.apk"
