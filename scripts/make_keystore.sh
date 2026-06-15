#!/usr/bin/env bash
# Generates the upload keystore + keystore.properties for release signing.
# Usage: ./scripts/make_keystore.sh [password]
# Both outputs are gitignored. BACK UP THE .jks FILE — losing it complicates app updates.
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p keystore

KS="keystore/tapcard-release.jks"
ALIAS="tapcard"
PASS="${1:-}"

if [ -f "$KS" ]; then
  echo "Keystore already exists at $KS — refusing to overwrite. Delete it first if you really mean to."
  exit 1
fi

if [ -z "$PASS" ]; then
  read -r -s -p "Enter a keystore password (min 6 chars): " PASS; echo
  read -r -s -p "Confirm password: " PASS2; echo
  [ "$PASS" = "$PASS2" ] || { echo "Passwords do not match."; exit 1; }
fi

# Locate keytool (Android Studio's bundled JDK works well on macOS).
KEYTOOL="keytool"
if ! command -v keytool >/dev/null 2>&1; then
  JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
  [ -x "$JBR" ] && KEYTOOL="$JBR"
fi

"$KEYTOOL" -genkeypair -v \
  -keystore "$KS" -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=Tertiary Infotech Academy Pte. Ltd., OU=Mobile, O=Tertiary Infotech, L=Singapore, C=SG"

cat > keystore/keystore.properties <<EOF
storePassword=$PASS
keyPassword=$PASS
keyAlias=$ALIAS
EOF

echo
echo "Created $KS and keystore/keystore.properties (both gitignored)."
echo "Upload-key fingerprints:"
"$KEYTOOL" -list -v -keystore "$KS" -alias "$ALIAS" -storepass "$PASS" | grep -E "SHA1|SHA256" || true
echo
echo "BACK UP keystore/tapcard-release.jks somewhere safe."
