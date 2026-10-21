#!/bin/bash
# Creates the Play upload key for the JetPacker bundle, outside the source tree.
#
# Usage: create_upload_key.sh [KEYSTORE] [ALIAS]
#   KEYSTORE  Default: ~/keys/jetpacker-upload.jks
#   ALIAS     Default: upload
#
# Never write a keystore into a source tree or public repository. Play App Signing
# manages the app signing key; this key only authenticates your uploads. If you
# lose it, request an upload key reset on the App integrity page in Play Console.

set -euo pipefail

raw_keystore="${1:-${HOME}/keys/jetpacker-upload.jks}"
[[ "${raw_keystore}" == "~/"* ]] && raw_keystore="${HOME}/${raw_keystore#"~/"}"
keystore="$(realpath -m -- "${raw_keystore}")"
readonly keystore
readonly key_alias="${2:-upload}"

KEYTOOL_BIN=""
if [[ -n "${JDK_BIN:-}" && -x "${JDK_BIN}/keytool" ]]; then
  KEYTOOL_BIN="${JDK_BIN}/keytool"
elif command -v keytool >/dev/null 2>&1; then
  KEYTOOL_BIN="$(command -v keytool)"
elif [[ -x "/usr/local/buildtools/java/jdk/bin/keytool" ]]; then
  KEYTOOL_BIN="/usr/local/buildtools/java/jdk/bin/keytool"
else
  echo "Error: keytool binary not found. Set JDK_BIN or ensure keytool is in PATH." >&2
  exit 1
fi
readonly keytool="${KEYTOOL_BIN}"

git_root="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -n "${git_root}" && ( "${keystore}" == "${git_root}" || "${keystore}" == "${git_root}/"* ) ]]; then
  echo "Error: refusing to create a keystore inside a source tree:" \
    "${keystore}" >&2
  exit 1
fi

if [[ -e "${keystore}" ]]; then
  echo "Error: ${keystore} already exists. Pick another path or reuse it." >&2
  exit 1
fi

mkdir -p -m 700 -- "$(dirname -- "${keystore}")"

# keytool prompts for the password, so it never reaches shell history.
"${keytool}" -genkeypair \
  -keystore "${keystore}" \
  -storetype PKCS12 \
  -alias "${key_alias}" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=JetPacker upload key"
chmod 600 -- "${keystore}"

echo
echo "Created ${keystore} (alias: ${key_alias})."
echo "Back up the keystore file and its password somewhere private."
