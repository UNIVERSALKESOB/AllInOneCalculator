# Shared build configuration, sourced by build.sh and build_aab.sh.

ANDROID_JAR="${ANDROID_JAR:-$HOME/sdk/android-36/android.jar}"
BUNDLETOOL_JAR="${BUNDLETOOL_JAR:-$HOME/tools/bundletool.jar}"

APP_NAME="AllInOneCalculator"
VERSION_NAME="1.3"
VERSION_CODE="4"
MIN_SDK="21"
TARGET_SDK="36"

KEY_ALIAS="${KEY_ALIAS:-calc}"
KEYSTORE="${KEYSTORE_PATH:-keys/release.jks}"
PASS_FILE="keys/password.txt"

# Resolves the signing keystore + passwords and creates a key on first use.
prepare_signing() {
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
}
