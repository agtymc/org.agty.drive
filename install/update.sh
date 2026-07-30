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
        read -r -p "${prompt_text} [${default_value}]: " answer
        if [[ -z "$answer" ]]; then
            answer="$default_value"
        fi
    else
        read -r -p "${prompt_text}: " answer
    fi
    printf '%s' "$answer"
}

extract_latest_asset_url() {
    local release_json="$1"
    local pattern="$2"
    printf '%s\n' "$release_json" \
        | sed -n 's/.*"browser_download_url"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' \
        | grep -E "$pattern" \
        | head -n1
}

download_file() {
    local url="$1"
    local destination="$2"
    curl -fsSL --retry 3 --connect-timeout 10 "$url" -o "$destination"
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
CONFIG_SAMPLE_PATH="${INSTALL_DIR}/config.ini-sample"
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
CONFIG_DOWNLOAD_URL="$(extract_latest_asset_url "$RELEASE_JSON" '/config\.ini([.-]sample)?$')"

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
printf 'Sample config: %s\n' "$CONFIG_SAMPLE_PATH"
printf 'Maintenance scripts refreshed in: %s\n' "$INSTALL_SUPPORT_DIR"
printf '\nUseful commands:\n'
printf '  systemctl status %s\n' "$SERVICE_NAME"
printf '  journalctl -u %s -f\n' "$SERVICE_NAME"
printf '  sudo bash %s/update.sh\n' "${INSTALL_SUPPORT_DIR}"
printf '  sudo bash %s/uninstall.sh\n' "${INSTALL_SUPPORT_DIR}"
