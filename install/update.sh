#!/usr/bin/env bash

set -Eeuo pipefail

APP_NAME="org.agty.drive"
APP_DESCRIPTION="AGTY/Drive Service"
SERVICE_NAME="${APP_NAME}.service"
DEFAULT_INSTALL_DIR="/opt/${APP_NAME}"
DEFAULT_GITHUB_REPO="agtymc/org.agty.drive"
DEFAULT_INSTALL_SCRIPTS_RAW_BASE_URL="https://raw.githubusercontent.com/agtymc/org.agty.drive/master/install"
SERVICE_PATH="/etc/systemd/system/${SERVICE_NAME}"

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

warn() {
    printf 'WARNING: %s\n' "$*" >&2
}

trim_value() {
    local value="$1"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    printf '%s' "$value"
}

require_command() {
    local command_name="$1"
    local package_hint="${2:-}"
    if ! command -v "$command_name" >/dev/null 2>&1; then
        if [[ -n "$package_hint" ]]; then
            fail "Command '$command_name' is required. Install package: $package_hint"
        fi
        fail "Command '$command_name' is required."
    fi
}

prompt_value() {
    local prompt_text="$1"
    local default_value="${2:-}"
    local answer=""
    if [[ -n "$default_value" ]]; then
        if [[ -r /dev/tty && -w /dev/tty ]]; then
            read -r -e -i "$default_value" -p "${prompt_text}: " answer </dev/tty
        else
            read -r -p "${prompt_text} [${default_value}]: " answer
            if [[ -z "$answer" ]]; then
                answer="$default_value"
            fi
        fi
    else
        if [[ -r /dev/tty && -w /dev/tty ]]; then
            read -r -e -p "${prompt_text}: " answer </dev/tty
        else
            read -r -p "${prompt_text}: " answer
        fi
    fi
    printf '%s' "$(trim_value "$answer")"
}

confirm() {
    local prompt_text="$1"
    local answer=""
    local normalized=""
    while true; do
        if [[ -r /dev/tty && -w /dev/tty ]]; then
            read -r -p "${prompt_text} [y/N]: " answer </dev/tty
        else
            read -r -p "${prompt_text} [y/N]: " answer
        fi
        normalized="$(printf '%s' "$answer" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z')"
        if [[ "$normalized" == y || "$normalized" == yes ]]; then
            return 0
        fi
        if [[ -z "$normalized" || "$normalized" == n || "$normalized" == no ]]; then
            return 1
        fi
        printf 'Please answer y or n.\n'
    done
}

extract_latest_asset_url() {
    local release_json="$1"
    local pattern="$2"
    printf '%s\n' "$release_json" \
        | sed -n 's/.*"browser_download_url"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' \
        | grep -E "$pattern" \
        | head -n1
}

extract_preferred_config_asset_url() {
    local release_json="$1"
    local asset_url=""
    asset_url="$(extract_latest_asset_url "$release_json" '/config\.ini-sample$')"
    if [[ -n "$asset_url" ]]; then
        printf '%s' "$asset_url"
        return 0
    fi
    asset_url="$(extract_latest_asset_url "$release_json" '/config\.ini\.sample$')"
    if [[ -n "$asset_url" ]]; then
        printf '%s' "$asset_url"
        return 0
    fi
    asset_url="$(extract_latest_asset_url "$release_json" '/config\.ini$')"
    printf '%s' "$asset_url"
}

download_file() {
    local url="$1"
    local destination="$2"
    curl -fsSL --retry 3 --connect-timeout 10 "$url" -o "$destination"
}

print_file_with_prefix() {
    local file_path="$1"
    local prefix="$2"
    [[ -f "$file_path" ]] || return 0
    while IFS= read -r line; do
        printf '%s%s\n' "$prefix" "$line"
    done <"$file_path"
}

extract_service_user() {
    local service_file="$1"
    awk -F'=' '
        /^[[:space:]]*User[[:space:]]*=/ {
            value = $2
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
            print value
            exit
        }
    ' "$service_file"
}

merge_config_with_sample() {
    local current_config="$1"
    local sample_config="$2"
    local output_config="$3"
    awk '
        function trim(value) {
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
            return value
        }
        FNR == NR {
            line = $0
            trimmed = trim(line)
            if (trimmed ~ /^\[/ && trimmed ~ /\]$/) {
                section = trimmed
                sub(/^\[/, "", section)
                sub(/\]$/, "", section)
                section = trim(section)
                next
            }
            if (trimmed == "" || trimmed ~ /^[#;]/) {
                next
            }
            separator = index(line, "=")
            if (separator < 1) {
                next
            }
            key = trim(substr(line, 1, separator - 1))
            value = trim(substr(line, separator + 1))
            scoped_key = section == "" ? key : section "." key
            values[scoped_key] = value
            next
        }
        {
            line = $0
            trimmed = trim(line)
            if (trimmed ~ /^\[/ && trimmed ~ /\]$/) {
                section = trimmed
                sub(/^\[/, "", section)
                sub(/\]$/, "", section)
                section = trim(section)
                print line
                next
            }
            if (trimmed == "" || trimmed ~ /^[#;]/) {
                print line
                next
            }
            separator = index(line, "=")
            if (separator < 1) {
                print line
                next
            }
            key = trim(substr(line, 1, separator - 1))
            scoped_key = section == "" ? key : section "." key
            if (scoped_key in values) {
                print key " = " values[scoped_key]
            } else {
                print line
            }
        }
    ' "$current_config" "$sample_config" >"$output_config"
}

list_deprecated_config_keys() {
    local current_config="$1"
    local sample_config="$2"
    local output_file="$3"
    awk '
        function trim(value) {
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
            return value
        }
        function emit_key(section_name, key_name) {
            if (key_name == "") {
                return
            }
            print (section_name == "" ? key_name : section_name "." key_name)
        }
        function collect_keys(file_name, prefix) {
            section = ""
            while ((getline line < file_name) > 0) {
                trimmed = trim(line)
                if (trimmed ~ /^\[/ && trimmed ~ /\]$/) {
                    section = trimmed
                    sub(/^\[/, "", section)
                    sub(/\]$/, "", section)
                    section = trim(section)
                    continue
                }
                if (trimmed == "" || trimmed ~ /^[#;]/) {
                    continue
                }
                separator = index(line, "=")
                if (separator < 1) {
                    continue
                }
                key = trim(substr(line, 1, separator - 1))
                scoped = section == "" ? key : section "." key
                keys[prefix, scoped] = 1
            }
            close(file_name)
        }
        BEGIN {
            collect_keys(ARGV[1], "current")
            collect_keys(ARGV[2], "sample")
            for (key in keys) {
                split(key, parts, SUBSEP)
                if (parts[1] == "current" && !(("sample" SUBSEP parts[2]) in keys)) {
                    print parts[2]
                }
            }
            exit
        }
    ' "$current_config" "$sample_config" | sort >"$output_file"
}

if [[ "${EUID}" -ne 0 ]]; then
    fail "Run this updater as root. Example: sudo bash install/update.sh"
fi

require_command curl curl
require_command systemctl systemd
require_command install coreutils
require_command chmod coreutils
require_command chown coreutils
require_command stat coreutils

INSTALL_DIR="$(prompt_value "Install directory" "$DEFAULT_INSTALL_DIR")"
[[ -n "$INSTALL_DIR" ]] || fail "Install directory cannot be empty."

BIN_DIR="${INSTALL_DIR}/bin"
INSTALL_SUPPORT_DIR="${INSTALL_DIR}/install"
CONFIG_PATH="${INSTALL_DIR}/config.ini"
CONFIG_SAMPLE_PATH="${INSTALL_DIR}/config.ini-sample"
CONFIG_MERGED_PATH="${INSTALL_DIR}/config.ini.merged"
CONFIG_DEPRECATED_KEYS_PATH="${INSTALL_DIR}/config.deprecated-keys.txt"
JAR_PATH="${BIN_DIR}/${APP_NAME}.jar"
GITHUB_REPO="$DEFAULT_GITHUB_REPO"
INSTALL_SCRIPTS_RAW_BASE_URL="$DEFAULT_INSTALL_SCRIPTS_RAW_BASE_URL"

[[ -d "$INSTALL_DIR" ]] || fail "Install directory not found: $INSTALL_DIR"
[[ -d "$BIN_DIR" ]] || fail "Binary directory not found: $BIN_DIR"
[[ -f "$SERVICE_PATH" ]] || fail "Service file not found: $SERVICE_PATH"

SERVICE_USER="$(extract_service_user "$SERVICE_PATH" || true)"
if [[ -z "$SERVICE_USER" ]]; then
    warn "Could not detect service user from ${SERVICE_PATH}. Ownership will be preserved from ${INSTALL_DIR}."
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

printf 'Requesting latest GitHub release metadata...\n'
RELEASE_API_URL="https://api.github.com/repos/${GITHUB_REPO}/releases/latest"
if ! RELEASE_JSON="$(curl -fsSL --connect-timeout 10 "$RELEASE_API_URL")"; then
    fail "Failed to load latest GitHub release metadata from ${RELEASE_API_URL}."
fi

JAR_DOWNLOAD_URL="$(extract_latest_asset_url "$RELEASE_JSON" '/org-agty-drive-[^/]+\.jar$')"
CONFIG_DOWNLOAD_URL="$(extract_preferred_config_asset_url "$RELEASE_JSON")"

[[ -n "$JAR_DOWNLOAD_URL" ]] || fail "Latest GitHub release does not contain the expected jar asset."
[[ -n "$CONFIG_DOWNLOAD_URL" ]] || fail "Latest GitHub release does not contain config.ini, config.ini.sample, or config.ini-sample."

printf 'Downloading jar asset...\n'
download_file "$JAR_DOWNLOAD_URL" "${TMP_DIR}/app.jar"

printf 'Downloading config sample asset...\n'
download_file "$CONFIG_DOWNLOAD_URL" "${TMP_DIR}/config.ini-sample"

printf 'Refreshing maintenance scripts...\n'
install -d -m 0755 "$INSTALL_SUPPORT_DIR"
download_file "${INSTALL_SCRIPTS_RAW_BASE_URL}/uninstall.sh" "${TMP_DIR}/uninstall.sh"
download_file "${INSTALL_SCRIPTS_RAW_BASE_URL}/update.sh" "${TMP_DIR}/update.sh"

printf 'Installing updated files...\n'
install -m 0644 "${TMP_DIR}/app.jar" "$JAR_PATH"
install -m 0644 "${TMP_DIR}/config.ini-sample" "$CONFIG_SAMPLE_PATH"
install -m 0755 "${TMP_DIR}/uninstall.sh" "${INSTALL_SUPPORT_DIR}/uninstall.sh"
install -m 0755 "${TMP_DIR}/update.sh" "${INSTALL_SUPPORT_DIR}/update.sh"

if [[ -f "$CONFIG_PATH" ]]; then
    printf 'Building merged config preview from current config and new sample...\n'
    merge_config_with_sample "$CONFIG_PATH" "$CONFIG_SAMPLE_PATH" "$CONFIG_MERGED_PATH"
    list_deprecated_config_keys "$CONFIG_PATH" "$CONFIG_SAMPLE_PATH" "$CONFIG_DEPRECATED_KEYS_PATH"
    printf '\nConfiguration update report:\n'
    printf '  Current config kept unchanged: %s\n' "$CONFIG_PATH"
    printf '  New sample downloaded to: %s\n' "$CONFIG_SAMPLE_PATH"
    printf '  Merged config prepared at: %s\n' "$CONFIG_MERGED_PATH"
    if [[ -s "$CONFIG_DEPRECATED_KEYS_PATH" ]]; then
        printf '  Keys to review because they are missing in the new sample:\n'
        print_file_with_prefix "$CONFIG_DEPRECATED_KEYS_PATH" '    - '
        printf '  These keys may be deprecated, removed, or renamed in the new version.\n'
    else
        printf '  Deprecated or renamed keys were not detected.\n'
    fi

    if ! cmp -s "$CONFIG_PATH" "$CONFIG_MERGED_PATH"; then
        printf '  The merged config differs from the current working config.\n'
        if confirm "Replace working config.ini with config.ini.merged and create a backup"; then
            CONFIG_BACKUP_PATH="${INSTALL_DIR}/config.ini.bak.$(date +%Y%m%d-%H%M%S)"
            install -m 0600 "$CONFIG_PATH" "$CONFIG_BACKUP_PATH"
            install -m 0600 "$CONFIG_MERGED_PATH" "$CONFIG_PATH"
            printf '  Backup created: %s\n' "$CONFIG_BACKUP_PATH"
            printf '  Working config updated from merged config.\n'
        else
            printf '  Working config was not changed. Review %s manually.\n' "$CONFIG_MERGED_PATH"
        fi
    else
        printf '  Merged config is identical to the current working config.\n'
    fi
fi

if [[ -n "$SERVICE_USER" ]]; then
    chown -R "${SERVICE_USER}:${SERVICE_USER}" "$INSTALL_DIR"
else
    INSTALL_OWNER_GROUP="$(stat -c '%U:%G' "$INSTALL_DIR")"
    chown -R "$INSTALL_OWNER_GROUP" "$INSTALL_DIR"
fi

printf 'Restarting service...\n'
systemctl daemon-reload
systemctl restart "$SERVICE_NAME"

printf '\nUpdate complete.\n'
printf 'Application directory: %s\n' "$INSTALL_DIR"
printf 'Jar: %s\n' "$JAR_PATH"
if [[ -f "$CONFIG_PATH" ]]; then
    printf 'Working config preserved: %s\n' "$CONFIG_PATH"
    printf 'Merged config preview: %s\n' "$CONFIG_MERGED_PATH"
    if [[ -s "$CONFIG_DEPRECATED_KEYS_PATH" ]]; then
        printf 'Deprecated or renamed keys to review: %s\n' "$CONFIG_DEPRECATED_KEYS_PATH"
    else
        printf 'Deprecated or renamed keys to review: none\n'
    fi
fi
printf 'Sample config: %s\n' "$CONFIG_SAMPLE_PATH"
printf 'Maintenance scripts refreshed in: %s\n' "$INSTALL_SUPPORT_DIR"
printf '\nUseful commands:\n'
printf '  sudo systemctl status %s\n' "$SERVICE_NAME"
printf '  sudo systemctl start %s\n' "$SERVICE_NAME"
printf '  sudo systemctl stop %s\n' "$SERVICE_NAME"
printf '  sudo systemctl restart %s\n' "$SERVICE_NAME"
printf '  sudo journalctl -u %s -f\n' "$SERVICE_NAME"
printf '  sudo bash %s/update.sh\n' "${INSTALL_SUPPORT_DIR}"
printf '  sudo bash %s/uninstall.sh\n' "${INSTALL_SUPPORT_DIR}"
