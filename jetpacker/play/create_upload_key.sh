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

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

normalize_path() {
  local path="$1"
  if [[ "${path}" == "~" ]]; then
    path="${HOME}"
  elif [[ "${path}" == "~/"* ]]; then
    path="${HOME}/${path#"~/"}"
  fi

  if [[ "${path}" != /* ]]; then
    path="$(pwd)/${path}"
  fi

  local cur="${path}" suffix=""
  while [[ ! -d "${cur}" && "${cur}" != "/" && "${cur}" != "." ]]; do
    suffix="/$(basename -- "${cur}")${suffix}"
    cur="$(dirname -- "${cur}")"
  done

  local resolved_dir
  resolved_dir="$(cd -P -- "${cur}" 2>/dev/null && pwd || echo "${cur}")"
  local full="${resolved_dir}${suffix}"

  local -a parts=()
  IFS='/' read -r -a parts <<< "${full}"

  local -a stack=()
  local part
  for part in "${parts[@]}"; do
    if [[ -z "${part}" || "${part}" == "." ]]; then
      continue
    elif [[ "${part}" == ".." ]]; then
      if ((${#stack[@]} > 0)); then
        unset 'stack[${#stack[@]}-1]'
      fi
    else
      stack+=("${part}")
    fi
  done

  local result=""
  for part in "${stack[@]}"; do
    result+="/${part}"
  done
  echo "${result:-/}"
}

raw_keystore="${1:-${HOME}/keys/jetpacker-upload.jks}"
keystore="$(normalize_path "${raw_keystore}")"
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

is_inside_tree() {
  local file_path="$1" root_path="$2"
  [[ -n "${root_path}" && ( "${file_path}" == "${root_path}" || "${file_path}" == "${root_path}/"* ) ]]
}

script_git_root="$(git -C "${script_dir}" rev-parse --show-toplevel 2>/dev/null || true)"
pwd_git_root="$(git rev-parse --show-toplevel 2>/dev/null || true)"
target_ancestor="$(dirname -- "${keystore}")"
while [[ ! -d "${target_ancestor}" && "${target_ancestor}" != "/" ]]; do
  target_ancestor="$(dirname -- "${target_ancestor}")"
done
target_git_root="$(git -C "${target_ancestor}" rev-parse --show-toplevel 2>/dev/null || true)"

for root in "${script_git_root}" "${pwd_git_root}" "${target_git_root}"; do
  if is_inside_tree "${keystore}" "${root}"; then
    echo "Error: refusing to create a keystore inside a source tree:" \
      "${keystore}" >&2
    exit 1
  fi
done

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
