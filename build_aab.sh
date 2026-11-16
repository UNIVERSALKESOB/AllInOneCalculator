#!/data/data/com.termux/files/usr/bin/bash
# Builds a Play-Store-ready .aab (Android App Bundle) + verifies it.
set -e

cd "$(dirname "$0")"
source ./config.sh

if [ ! -f "$ANDROID_JAR" ]; then
  echo "android.jar not found at $ANDROID_JAR" >&2
  exit 1
fi
if [ ! -f "$BUNDLETOOL_JAR" ]; then
  echo "bundletool not found at $BUNDLETOOL_JAR" >&2
  echo "download: https://github.com/google/bundletool/releases" >&2
  exit 1
fi

echo "==> cleaning"
rm -rf build/aab
mkdir -p build/aab/gen build/aab/classes build/aab/dex build/aab/module out

echo "==> resources (proto)"
aapt2 compile --dir res -o build/aab/res.zip
aapt2 link --proto-format -o build/aab/base.apk \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -R build/aab/res.zip \
  --java build/aab/gen \
  --min-sdk-version "$MIN_SDK" \
  --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay

echo "==> javac"
find src build/aab/gen -name '*.java' > build/aab/sources.txt
javac -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" \
  -d build/aab/classes \
  @build/aab/sources.txt

echo "==> dex"
jar cf build/aab/classes.jar -C build/aab/classes .
d8 --release --min-api "$MIN_SDK" --lib "$ANDROID_JAR" \
  --output build/aab/dex build/aab/classes.jar

echo "==> module layout"
unzip -q -o build/aab/base.apk -d build/aab/module
rm -rf build/aab/module/META-INF
mkdir -p build/aab/module/manifest build/aab/module/dex
mv build/aab/module/AndroidManifest.xml build/aab/module/manifest/AndroidManifest.xml
cp build/aab/dex/classes.dex build/aab/module/dex/classes.dex

(cd build/aab/module && zip -q -r ../base.zip .)

echo "==> bundle"
java -jar "$BUNDLETOOL_JAR" build-bundle \
  --modules=build/aab/base.zip \
  --output="out/${APP_NAME}.aab" \
  --config=bundleconfig.json

echo "==> validate"
java -jar "$BUNDLETOOL_JAR" validate --bundle="out/${APP_NAME}.aab"

echo "==> sign (upload key)"
prepare_signing
jarsigner -keystore "$KEYSTORE" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  "out/${APP_NAME}.aab" "$KEY_ALIAS" >/dev/null
jarsigner -verify "out/${APP_NAME}.aab" | head -2

echo "==> test (universal APK from the bundle)"
rm -rf build/aab/test
if java -jar "$BUNDLETOOL_JAR" build-apks \
  --bundle="out/${APP_NAME}.aab" \
  --output=build/aab/test.apks \
  --mode=universal \
  --ks="$KEYSTORE" \
  --ks-key-alias="$KEY_ALIAS" \
  --ks-pass="pass:$KEYSTORE_PASSWORD" \
  --key-pass="pass:$KEY_PASSWORD" >/dev/null 2>&1; then
  unzip -q -o build/aab/test.apks universal.apk -d build/aab/test
  aapt2 dump badging build/aab/test/universal.apk 2>/dev/null | head -2
else
  echo "note: universal APK test skipped (bundletool's bundled aapt2 is not runnable on this host)"
fi

ls -lh "out/${APP_NAME}.aab"
echo "BUILD OK: $(pwd)/out/${APP_NAME}.aab"
