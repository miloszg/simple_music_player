#!/usr/bin/env bash
# Creates the Play upload key and the gitignored properties file Gradle reads.
#
#   ./tools/release/make-keystore.sh
#
# Run this ONCE. Then back the .jks up somewhere you will still have in five
# years. If you lose it you cannot ship an update to the same listing — you can
# ask Google to reset the upload key, but only because Play App Signing holds
# the real app-signing key. Losing both ends the app.
set -euo pipefail

KS="${1:-$HOME/.android-keys/flow-upload.jks}"
ALIAS=upload
PROPS="$(cd "$(dirname "$0")/../.." && pwd)/app/keystore.properties"

if [ -f "$KS" ]; then echo "refusing to overwrite $KS"; exit 1; fi
mkdir -p "$(dirname "$KS")"

read -rsp "Choose a keystore password: " PW; echo
read -rsp "Repeat it: " PW2; echo
[ "$PW" = "$PW2" ] || { echo "passwords differ"; exit 1; }
[ ${#PW} -ge 12 ] || { echo "use at least 12 characters"; exit 1; }

"${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}/bin/keytool" -genkeypair \
  -keystore "$KS" -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PW" -keypass "$PW" \
  -dname "CN=Flow, OU=Flow, O=Flow, L=, S=, C=PL"

umask 077
cat > "$PROPS" <<PROPS_EOF
storeFile=$KS
storePassword=$PW
keyAlias=$ALIAS
keyPassword=$PW
PROPS_EOF

echo
echo "key:   $KS"
echo "props: $PROPS  (gitignored)"
echo
echo "Back the .jks up now. Then: ./gradlew bundleRelease"
