#!/data/data/com.termux/files/usr/bin/bash
# Builds a signed, installable APK with the plain SDK tools (no Gradle).
set -e

cd "$(dirname "$0")"
PROJECT_DIR="$PWD"
ANDROID_JAR="${ANDROID_JAR:-$HOME/sdk/android-34-ext12/android.jar}"
APP_NAME="AllInOneCalculator"
VERSION_NAME="1.0"
VERSION_CODE="1"
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
javac -source 8 -target 8 -nowarn -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" \
  -d build/classes \
  @build/sources.txt 2>&1 | grep -v 'warning:' || true

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
if [ ! -f build/release.keystore ]; then
  keytool -genkeypair -keystore build/release.keystore -alias calc \
    -storepass calculator -keypass calculator \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -dname "CN=All-in-One Calculator, O=AntiGem, C=BD" >/dev/null 2>&1
fi

apksigner sign \
  --ks build/release.keystore \
  --ks-key-alias calc \
  --ks-pass pass:calculator \
  --key-pass pass:calculator \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "out/${APP_NAME}.apk" \
  build/aligned.apk

echo "==> verify"
apksigner verify --print-certs "out/${APP_NAME}.apk" | head -5
ls -lh "out/${APP_NAME}.apk"
echo "BUILD OK: $PROJECT_DIR/out/${APP_NAME}.apk"
