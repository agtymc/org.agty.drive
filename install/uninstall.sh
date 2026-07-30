#!/usr/bin/env bash

set -Eeuo pipefail

APP_NAME="org.agty.drive"
SERVICE_NAME="${APP_NAME}.service"
DEFAULT_INSTALL_DIR="/opt/${APP_NAME}"
SERVICE_PATH="/etc/systemd/system/${SERVICE_NAME}"

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

warn() {
    printf 'WARNING: %s\n' "$*" >&2
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

confirm() {
    local prompt_text="$1"
    local answer=""
    local normalized=""
    while true; do
        read -r -p "${prompt_text} [y/N]: " answer
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

require_root() {
    if [[ "${EUID}" -ne 0 ]]; then
        fail "Run this uninstaller as root. Example: sudo bash install/uninstall.sh"
    fi
}

extract_config_value() {
    local file_path="$1"
    local section_name="$2"
    local key_name="$3"
    awk -F'=' -v section="$section_name" -v key="$key_name" '
        /^[[:space:]]*\[/ {
            current = $0
            gsub(/^[[:space:]]*\[/, "", current)
            gsub(/\][[:space:]]*$/, "", current)
            next
        }
        current == section {
            left = $1
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", left)
            if (left == key) {
                value = substr($0, index($0, "=") + 1)
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
                gsub(/^"|"$/, "", value)
                print value
                exit
            }
        }
    ' "$file_path"
}

require_root

INSTALL_DIR="$(prompt_value "Install directory" "$DEFAULT_INSTALL_DIR")"
[[ -n "$INSTALL_DIR" ]] || fail "Install directory cannot be empty."

CONFIG_PATH="${INSTALL_DIR}/config.ini"
CONTENT_DIR_DEFAULT="${INSTALL_DIR}/content"
CONTENT_DIR="$CONTENT_DIR_DEFAULT"

if [[ -f "$CONFIG_PATH" ]]; then
    CONFIGURED_CONTENT_DIR="$(extract_config_value "$CONFIG_PATH" "storage" "content_dir" || true)"
    if [[ -n "$CONFIGURED_CONTENT_DIR" ]]; then
        CONTENT_DIR="$CONFIGURED_CONTENT_DIR"
    fi
fi

printf 'Uninstaller will stop and disable %s, remove %s, and reload systemd.\n' "$SERVICE_NAME" "$SERVICE_PATH"
printf 'Install directory selected: %s\n' "$INSTALL_DIR"
printf 'Content directory detected: %s\n' "$CONTENT_DIR"

if ! confirm "Continue uninstall"; then
    fail "Uninstall cancelled."
fi

if systemctl is-active --quiet "$SERVICE_NAME"; then
    printf 'Stopping service %s...\n' "$SERVICE_NAME"
    systemctl stop "$SERVICE_NAME"
else
    printf 'Service %s is already stopped or not active.\n' "$SERVICE_NAME"
fi

if systemctl is-enabled --quiet "$SERVICE_NAME"; then
    printf 'Disabling service %s...\n' "$SERVICE_NAME"
    systemctl disable "$SERVICE_NAME"
else
    printf 'Service %s is already disabled or not installed.\n' "$SERVICE_NAME"
fi

if [[ -f "$SERVICE_PATH" ]]; then
    printf 'Removing service file %s...\n' "$SERVICE_PATH"
    rm -f "$SERVICE_PATH"
else
    warn "Service file ${SERVICE_PATH} not found."
fi

printf 'Reloading systemd...\n'
systemctl daemon-reload
systemctl reset-failed "$SERVICE_NAME" >/dev/null 2>&1 || true

if [[ -d "$INSTALL_DIR" ]]; then
    if confirm "Remove install directory ${INSTALL_DIR}"; then
        rm -rf "$INSTALL_DIR"
        printf 'Removed install directory: %s\n' "$INSTALL_DIR"
    else
        warn "Install directory kept: ${INSTALL_DIR}"
    fi
else
    warn "Install directory ${INSTALL_DIR} not found."
fi

if [[ "$CONTENT_DIR" != "${INSTALL_DIR}/content" && -d "$CONTENT_DIR" ]]; then
    if confirm "Remove external content directory ${CONTENT_DIR}"; then
        rm -rf "$CONTENT_DIR"
        printf 'Removed content directory: %s\n' "$CONTENT_DIR"
    else
        warn "Content directory kept: ${CONTENT_DIR}"
    fi
fi

printf '\nUninstall complete.\n'
