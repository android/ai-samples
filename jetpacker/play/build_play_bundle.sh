#!/bin/bash
# Builds, signs, and checks the JetPacker app bundle for Play Console upload.
# Run with --help for options. Settings come from command-line flags first,
# then the demo profile named by --profile.

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

# Find android root with gradlew
android_dir=""
if [[ -f "${script_dir}/../android/gradlew" ]]; then
  android_dir="$(cd -- "${script_dir}/../android" && pwd)"
elif [[ -f "${script_dir}/../../gradlew" ]]; then
  android_dir="$(cd -- "${script_dir}/../.." && pwd)"
elif [[ -f "${script_dir}/gradlew" ]]; then
  android_dir="${script_dir}"
else
  android_dir="$(pwd)"
fi

readonly ANDROID_DIR="${android_dir}"
readonly GRADLEW="${ANDROID_DIR}/gradlew"

# Locate JDK tools (jarsigner, keytool, java)
JDK_BIN_DIR="${JDK_BIN:-}"
if [[ -z "${JDK_BIN_DIR}" ]]; then
  if command -v jarsigner >/dev/null 2>&1; then
    JDK_BIN_DIR="$(dirname -- "$(command -v jarsigner)")"
  elif [[ -d "/usr/local/buildtools/java/jdk/bin" ]]; then
    JDK_BIN_DIR="/usr/local/buildtools/java/jdk/bin"
  fi
fi
readonly JDK_BIN="${JDK_BIN_DIR}"

readonly JARSIGNER="${JDK_BIN:+${JDK_BIN}/}jarsigner"
readonly KEYTOOL="${JDK_BIN:+${JDK_BIN}/}keytool"
readonly JAVA="${JDK_BIN:+${JDK_BIN}/}java"
readonly SIGALG="${JARSIGNER_SIGALG:-SHA256withRSA}"

readonly APPFUNCTIONS_ASSET="base/assets/jetpacker_app_function_service.xml"
readonly BUNDLETOOL_JAR="${BUNDLETOOL_JAR:-}"

# Keys a profile can set. README.md describes them.
readonly -a PROFILE_KEYS=(
  APPLICATION_ID KEYSTORE UNIVERSAL_APK PLAY_CONSOLE DEVELOPER_ID
)

usage() {
  cat <<EOF
Usage: $(basename -- "$0") [--profile NAME] [--keystore PATH] [options]

Builds :app:bundleRelease with Gradle, signs it with your upload key,
and checks it before you upload it to Play Console.

Options:
  --profile NAME|PATH   Profile with your package name, keystore, and
                        Play Console account. NAME reads profiles/NAME.conf
                        next to this script; a value with a slash is a file
                        path. Flags override profile values. See DEMO.md.
  --keystore PATH       Upload keystore. Create one with create_upload_key.sh.
                        Required unless the profile sets KEYSTORE.
  --application-id ID   Package name override. Play ties it to the first developer
                        account that uploads it.
  --version-code N      Version code. Play rejects one it has already seen.
  --alias NAME          Key alias. Default: upload.
  --out PATH            Signed bundle path.
                        Default: ~/jetpacker-play-v<versionCode>.aab.
  --abis LIST           Comma-separated native ABIs. Default: arm64-v8a.
  --universal-apk PATH  Also write a universal APK signed with the upload key,
                        to install on test devices.
  -h, --help            Show this help.

Environment:
  JDK_BIN               JDK bin directory containing jarsigner, keytool, and java.
  BUNDLETOOL_JAR        Optional path to bundletool.jar to generate universal APKs.
  JARSIGNER_SIGALG      Signature algorithm. Default: SHA256withRSA.
EOF
}

die() {
  echo "Error: $*" >&2
  exit 1
}

profile=""
keystore=""
expected_package=""
expected_version=""
key_alias="upload"
out=""
abis="arm64-v8a"
universal_apk=""

while (($# > 0)); do
  case "$1" in
    -h | --help)
      usage
      exit 0
      ;;
    --profile | --keystore | --application-id | --version-code | --alias | \
      --out | --abis | --universal-apk)
      (($# >= 2)) || die "$1 needs a value."
      case "$1" in
        --profile) profile="$2" ;;
        --keystore) keystore="$2" ;;
        --application-id) expected_package="$2" ;;
        --version-code) expected_version="$2" ;;
        --alias) key_alias="$2" ;;
        --out) out="$2" ;;
        --abis) abis="$2" ;;
        --universal-apk) universal_apk="$2" ;;
      esac
      shift 2
      ;;
    *)
      usage >&2
      die "Unknown argument: $1"
      ;;
  esac
done

profile_names() {
  local file names=""
  for file in "${script_dir}"/profiles/*.conf "${script_dir}"/profiles/*.example; do
    [[ -e "${file}" ]] || continue
    file="${file##*/}"
    names+="${names:+, }${file%.*}"
  done
  echo "${names:-none}"
}

declare -A profile_values=()
load_profile() {
  local file="$1" line key value line_number=0
  local skip_re='^[[:space:]]*(#|$)'
  local entry_re='^[[:space:]]*([A-Z_]+)[[:space:]]*=(.*)$'
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line_number=$((line_number + 1))
    line="${line%$'\r'}"
    [[ "${line}" =~ ${skip_re} ]] && continue
    [[ "${line}" =~ ${entry_re} ]] ||
      die "${file}:${line_number}: expected KEY=VALUE."
    key="${BASH_REMATCH[1]}"
    value="${BASH_REMATCH[2]}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    [[ " ${PROFILE_KEYS[*]} " == *" ${key} "* ]] ||
      die "${file}:${line_number}: unknown key ${key}. Valid keys: ${PROFILE_KEYS[*]}."
    case "${key}" in
      KEYSTORE | UNIVERSAL_APK)
        [[ "${value}" != "~/"* ]] || value="${HOME}/${value#"~/"}"
        [[ -z "${value}" || "${value}" == /* ]] ||
          die "${file}:${line_number}: ${key} must start with / or ~/."
        ;;
    esac
    profile_values["${key}"]="${value}"
  done <"${file}"
}

expand_tilde() {
  local p="$1"
  [[ "${p}" == "~/"* ]] && p="${HOME}/${p#"~/"}"
  echo "${p}"
}

profile_file=""
if [[ -n "${profile}" ]]; then
  if [[ "${profile}" == */* ]]; then
    profile_file="$(realpath -m -- "$(expand_tilde "${profile}")")"
  elif [[ -f "${script_dir}/profiles/${profile}.conf" ]]; then
    profile_file="${script_dir}/profiles/${profile}.conf"
  elif [[ -f "${script_dir}/profiles/${profile}.conf.example" ]]; then
    profile_file="${script_dir}/profiles/${profile}.conf.example"
  else
    profile_file="${script_dir}/profiles/${profile}.conf"
  fi
  [[ -r "${profile_file}" ]] ||
    die "Can't read profile ${profile_file}. Available profiles: $(profile_names)."
  load_profile "${profile_file}"
fi

keystore="${keystore:-${profile_values[KEYSTORE]:-}}"
expected_package="${expected_package:-${profile_values[APPLICATION_ID]:-com.example.jetpacker}}"
universal_apk="${universal_apk:-${profile_values[UNIVERSAL_APK]:-}}"
play_console="${profile_values[PLAY_CONSOLE]:-https://play.google.com/console}"
play_console="${play_console%/}"
developer_id="${profile_values[DEVELOPER_ID]:-}"

url_re='^https://[^[:space:]]+$'
[[ -z "${play_console}" || "${play_console}" =~ ${url_re} ]] ||
  die "PLAY_CONSOLE must be an https:// URL, not '${play_console}'."
[[ -z "${developer_id}" || "${developer_id}" =~ ^[0-9]+$ ]] ||
  die "DEVELOPER_ID must contain only digits, not '${developer_id}'."

[[ -n "${keystore}" ]] ||
  die "Pass --keystore, or a --profile that sets KEYSTORE. Create a keystore with create_upload_key.sh."

keystore="$(expand_tilde "${keystore}")"
keystore="$(realpath -m -- "${keystore}")"
[[ -r "${keystore}" ]] ||
  die "Can't read ${keystore}. Create it with create_upload_key.sh."

if [[ -n "${out}" ]]; then
  out="$(expand_tilde "${out}")"
  out="$(realpath -m -- "${out}")"
fi
if [[ -n "${universal_apk}" ]]; then
  universal_apk="$(expand_tilde "${universal_apk}")"
  universal_apk="$(realpath -m -- "${universal_apk}")"
fi

[[ -x "${GRADLEW}" ]] ||
  die "Cannot find gradlew executable at ${GRADLEW}."

# Check password and alias before building
if [[ -z "${KS_PASS:-}" ]]; then
  read -r -s -p "Password for ${keystore}: " ks_pass
  echo >&2
else
  ks_pass="${KS_PASS}"
fi

KS_PASS="${ks_pass}" "${KEYTOOL}" -list -keystore "${keystore}" \
  -alias "${key_alias}" -storepass:env KS_PASS >/dev/null 2>&1 ||
  die "Wrong password, or alias '${key_alias}' isn't in ${keystore}."

echo "Building :app:bundleRelease with Gradle in ${ANDROID_DIR}..." >&2

gradle_args=(":app:bundleRelease")
if [[ -n "${expected_version}" ]]; then
  gradle_args+=("-PversionCode=${expected_version}")
fi
if [[ -n "${expected_package}" && "${expected_package}" != "com.example.jetpacker" ]]; then
  gradle_args+=("-PapplicationId=${expected_package}")
fi

(cd "${ANDROID_DIR}" && "${GRADLEW}" "${gradle_args[@]}")

# Locate built bundle
built_aab="${ANDROID_DIR}/app/build/outputs/bundle/release/app-release.aab"
if [[ ! -f "${built_aab}" ]]; then
  built_aab="${ANDROID_DIR}/app/build/outputs/bundle/release/app-release-unsigned.aab"
fi
[[ -f "${built_aab}" ]] ||
  die "Gradle build succeeded, but no AAB bundle found in ${ANDROID_DIR}/app/build/outputs/bundle/release/."

work="$(mktemp -d)"
trap 'rm -rf -- "${work}"' EXIT
signed="${work}/signed.aab"
cp -- "${built_aab}" "${signed}"
chmod u+w -- "${signed}"

echo "Signing bundle with upload key..." >&2
KS_PASS="${ks_pass}" "${JARSIGNER}" -keystore "${keystore}" \
  -storepass:env KS_PASS -digestalg SHA-256 -sigalg "${SIGALG}" \
  "${signed}" "${key_alias}" >/dev/null

verify_output="$("${JARSIGNER}" -verify "${signed}")"
[[ "${verify_output}" == *"jar verified."* ]] ||
  die "jarsigner cannot verify the signed bundle."

# Verify contents
contents="$(unzip -Z1 "${signed}")"
if [[ "${contents}" != *"${APPFUNCTIONS_ASSET}"* ]]; then
  echo "Warning: bundle does not contain ${APPFUNCTIONS_ASSET}." >&2
fi

out="${out:-${HOME}/jetpacker-play-release.aab}"
mkdir -p -- "$(dirname -- "${out}")"
cp -- "${signed}" "${out}"

# If bundletool is provided and universal apk is requested, generate it
if [[ -n "${universal_apk}" ]]; then
  if [[ -n "${BUNDLETOOL_JAR}" && -f "${BUNDLETOOL_JAR}" ]]; then
    (umask 077 && printf '%s' "${ks_pass}" >"${work}/ks_pass")
    "${JAVA}" -jar "${BUNDLETOOL_JAR}" build-apks --mode=universal \
      --bundle="${signed}" --output="${work}/universal.apks" \
      --ks="${keystore}" --ks-key-alias="${key_alias}" \
      --ks-pass="file:${work}/ks_pass"
    mkdir -p -- "$(dirname -- "${universal_apk}")"
    unzip -p "${work}/universal.apks" universal.apk >"${universal_apk}"
    echo "Universal APK generated at: ${universal_apk}"
  else
    echo "Notice: universal APK requested, but BUNDLETOOL_JAR is not set. Skipped universal APK generation." >&2
  fi
fi

cert_sha256="$(KS_PASS="${ks_pass}" "${KEYTOOL}" -list \
  -keystore "${keystore}" -alias "${key_alias}" -storepass:env KS_PASS |
  sed -n 's/^Certificate fingerprint (SHA-256): //p')"

cat <<EOF

Signed bundle created successfully:
  Location:    ${out}
  Upload cert: SHA-256 ${cert_sha256}
EOF

if [[ -n "${universal_apk}" && -f "${universal_apk}" ]]; then
  echo "  Universal APK: ${universal_apk}"
fi
if [[ -n "${play_console}" ]]; then
  console_url="${play_console}"
  [[ -z "${developer_id}" ]] ||
    console_url+="/u/0/developers/${developer_id}/app-list"
  echo "  Play Console:  ${console_url}"
fi

cat <<EOF

Next: in Google Play Console, navigate to Internal testing > Create new release,
upload the bundle, and complete release submission.
EOF
