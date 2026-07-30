#!/usr/bin/env bash

set -Eeuo pipefail

APP_NAME="org.agty.drive"
APP_DESCRIPTION="AGTY/Drive Service"
SERVICE_NAME="${APP_NAME}.service"
REQUIRED_JAVA_MAJOR="21"
DEFAULT_INSTALL_DIR="/opt/${APP_NAME}"
DEFAULT_GITHUB_REPO="agtymc/org.agty.drive"
DEFAULT_INSTALL_SCRIPTS_RAW_BASE_URL="https://raw.githubusercontent.com/agtymc/org.agty.drive/master/install"
DEFAULT_DB_HOST="localhost"
DEFAULT_DB_PORT="5432"
DEFAULT_DB_NAME="agtydrive"
DEFAULT_DB_SCHEMA="public"
DEFAULT_DB_USER="postgres"
DEFAULT_BIND_ADDRESS="127.0.0.1"
DEFAULT_BIND_PORT="8080"
DEFAULT_TIMEZONE="Europe/Moscow"
DEFAULT_APP_TITLE="AGTY/DRIVE"
DEFAULT_APP_ABOUT="Private file storage and sharing workspace"
DEFAULT_UPLOAD_MAX_FILE_SIZE="1024MB"
DEFAULT_UPLOAD_MAX_REQUEST_SIZE="1024MB"
DEFAULT_SESSION_TIMEOUT_MINUTES="43200"
DEFAULT_SESSION_COOKIE_MAX_AGE_MINUTES="43200"
DEFAULT_DB_POOL_MAX_SIZE="32"
DEFAULT_DB_POOL_MAX_LIFETIME_MIN="30"
DEFAULT_DB_POOL_BORROW_TIMEOUT_MS="300"
DEFAULT_BOOTSTRAP_ADMIN_LOGIN="admin"
DEFAULT_BOOTSTRAP_ADMIN_PASSWORD="admin"
DEFAULT_BOOTSTRAP_ADMIN_DISPLAY_NAME="Administrator"

warn() {
    printf 'WARNING: %s\n' "$*" >&2
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
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

trim_value() {
    local value="$1"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    printf '%s' "$value"
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

prompt_secret() {
    local prompt_text="$1"
    local answer=""
    if [[ -r /dev/tty && -w /dev/tty ]]; then
        read -r -s -p "${prompt_text}: " answer </dev/tty
        printf '\n' >/dev/tty
    else
        read -r -s -p "${prompt_text}: " answer
        printf '\n' >&2
    fi
    printf '%s' "$(trim_value "$answer")"
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

parse_java_major() {
    local version_line="$1"
    local version_value
    version_value="$(printf '%s\n' "$version_line" | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n1)"
    if [[ -z "$version_value" ]]; then
        return 1
    fi
    if [[ "$version_value" =~ ^1\.([0-9]+) ]]; then
        printf '%s' "${BASH_REMATCH[1]}"
        return 0
    fi
    printf '%s' "$version_value" | cut -d'.' -f1
}

validate_port() {
    local port="$1"
    [[ "$port" =~ ^[0-9]+$ ]] || return 1
    (( port >= 1 && port <= 65535 ))
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

if [[ "${EUID}" -ne 0 ]]; then
    fail "Run this installer as root. Example: sudo bash install/install.sh"
fi

require_command curl curl
require_command java "openjdk-${REQUIRED_JAVA_MAJOR}-jre or openjdk-${REQUIRED_JAVA_MAJOR}-jdk"
require_command psql postgresql-client
require_command systemctl systemd
require_command install coreutils
require_command chmod coreutils
require_command chown coreutils

JAVA_BIN="$(command -v java)"
JAVA_VERSION_LINE="$("$JAVA_BIN" -version 2>&1 | head -n1)"
JAVA_MAJOR="$(parse_java_major "$JAVA_VERSION_LINE" || true)"
if [[ -z "$JAVA_MAJOR" ]]; then
    fail "Unable to detect Java version from: ${JAVA_VERSION_LINE}"
fi
if (( JAVA_MAJOR < REQUIRED_JAVA_MAJOR )); then
    fail "Java ${REQUIRED_JAVA_MAJOR} or newer is required. Detected: ${JAVA_VERSION_LINE}"
fi

DEFAULT_SERVICE_USER="${SUDO_USER:-$(id -un)}"
if [[ "$DEFAULT_SERVICE_USER" == "root" ]]; then
    DEFAULT_SERVICE_USER="agarty"
fi

printf 'Java check passed: %s\n' "$JAVA_VERSION_LINE"
printf 'Installer will create application directories, download latest release assets, generate config.ini and register a systemd service.\n'

INSTALL_DIR="$(prompt_value "Install directory" "$DEFAULT_INSTALL_DIR")"
SERVICE_USER="$(prompt_value "System user for service" "$DEFAULT_SERVICE_USER")"
GITHUB_REPO="$DEFAULT_GITHUB_REPO"
INSTALL_SCRIPTS_RAW_BASE_URL="$DEFAULT_INSTALL_SCRIPTS_RAW_BASE_URL"

id "$SERVICE_USER" >/dev/null 2>&1 || fail "User '$SERVICE_USER' does not exist."

DB_HOST="$(prompt_value "PostgreSQL host" "$DEFAULT_DB_HOST")"
DB_PORT="$(prompt_value "PostgreSQL port" "$DEFAULT_DB_PORT")"
validate_port "$DB_PORT" || fail "Invalid PostgreSQL port: $DB_PORT"
DB_NAME="$(prompt_value "PostgreSQL database" "$DEFAULT_DB_NAME")"
DB_SCHEMA="$(prompt_value "PostgreSQL schema" "$DEFAULT_DB_SCHEMA")"
DB_USER="$(prompt_value "PostgreSQL user" "$DEFAULT_DB_USER")"
DB_PASSWORD="$(prompt_secret "PostgreSQL password")"
[[ -n "$DB_PASSWORD" ]] || fail "PostgreSQL password cannot be empty."

BIND_ADDRESS="$(prompt_value "Application bind IP" "$DEFAULT_BIND_ADDRESS")"
[[ -n "$BIND_ADDRESS" ]] || fail "Application bind IP cannot be empty."
BIND_PORT="$(prompt_value "Application bind port" "$DEFAULT_BIND_PORT")"
validate_port "$BIND_PORT" || fail "Invalid application port: $BIND_PORT"
APP_TITLE="$(prompt_value "Application title" "$DEFAULT_APP_TITLE")"
APP_ABOUT="$(prompt_value "Application subtitle/about" "$DEFAULT_APP_ABOUT")"
APP_URI_DEFAULT="http://${BIND_ADDRESS}:${BIND_PORT}"
APP_URI="$(prompt_value "Public application URI" "$APP_URI_DEFAULT")"

TIMEZONE_VALUE="$(prompt_value "Application timezone" "$DEFAULT_TIMEZONE")"
CONTENT_DIR_DEFAULT="${INSTALL_DIR}/content"
CONTENT_DIR="$(prompt_value "Content storage directory" "$CONTENT_DIR_DEFAULT")"
UPLOAD_MAX_FILE_SIZE="$(prompt_value "Max single file upload size" "$DEFAULT_UPLOAD_MAX_FILE_SIZE")"
UPLOAD_MAX_REQUEST_SIZE="$(prompt_value "Max request upload size" "$DEFAULT_UPLOAD_MAX_REQUEST_SIZE")"
SESSION_TIMEOUT_MINUTES="$DEFAULT_SESSION_TIMEOUT_MINUTES"
SESSION_COOKIE_MAX_AGE_MINUTES="$DEFAULT_SESSION_COOKIE_MAX_AGE_MINUTES"
DB_POOL_MAX_SIZE="$DEFAULT_DB_POOL_MAX_SIZE"
DB_POOL_MAX_LIFETIME_MIN="$DEFAULT_DB_POOL_MAX_LIFETIME_MIN"
DB_POOL_BORROW_TIMEOUT_MS="$DEFAULT_DB_POOL_BORROW_TIMEOUT_MS"
BOOTSTRAP_ADMIN_LOGIN="$DEFAULT_BOOTSTRAP_ADMIN_LOGIN"
BOOTSTRAP_ADMIN_PASSWORD="$DEFAULT_BOOTSTRAP_ADMIN_PASSWORD"
BOOTSTRAP_ADMIN_DISPLAY_NAME="$DEFAULT_BOOTSTRAP_ADMIN_DISPLAY_NAME"

printf 'Checking PostgreSQL connection...\n'
if ! PGPASSWORD="$DB_PASSWORD" psql \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --username "$DB_USER" \
    --dbname "$DB_NAME" \
    --no-password \
    --tuples-only \
    --quiet \
    --set ON_ERROR_STOP=1 \
    --command 'select 1;' >/dev/null 2>&1; then
    fail "Failed to connect to PostgreSQL with provided settings."
fi
printf 'PostgreSQL connection check passed.\n'

BIN_DIR="${INSTALL_DIR}/bin"
INSTALL_SUPPORT_DIR="${INSTALL_DIR}/install"
LOG_DIR="${INSTALL_DIR}/logs"
CONFIG_SAMPLE_PATH="${INSTALL_DIR}/config.ini-sample"
CONFIG_PATH="${INSTALL_DIR}/config.ini"
RUN_SCRIPT_PATH="${INSTALL_DIR}/${APP_NAME}.sh"
JAR_PATH="${BIN_DIR}/${APP_NAME}.jar"
SERVICE_PATH="/etc/systemd/system/${SERVICE_NAME}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

printf 'Requesting latest GitHub release metadata...\n'
RELEASE_API_URL="https://api.github.com/repos/${GITHUB_REPO}/releases/latest"
if ! RELEASE_JSON="$(curl -fsSL --connect-timeout 10 "$RELEASE_API_URL")"; then
    fail "Failed to load latest GitHub release metadata from ${RELEASE_API_URL}.

Prepare latest release with these assets and run installer again:
1. Upload the application jar file, for example: org-agty-drive-1.0.0.jar
2. Upload config.ini-sample
3. Mark the release as latest."
fi

JAR_DOWNLOAD_URL="$(extract_latest_asset_url "$RELEASE_JSON" '/org-agty-drive-[^/]+\.jar$')"
CONFIG_DOWNLOAD_URL="$(extract_preferred_config_asset_url "$RELEASE_JSON")"

[[ -n "$JAR_DOWNLOAD_URL" ]] || fail "Latest GitHub release does not contain the expected jar asset.

Prepare latest release with:
- org-agty-drive-<version>.jar
- config.ini-sample"

[[ -n "$CONFIG_DOWNLOAD_URL" ]] || fail "Latest GitHub release does not contain config.ini, config.ini.sample, or config.ini-sample.

Recommended release assets:
- org-agty-drive-<version>.jar
- config.ini-sample"

printf 'Creating directories...\n'
install -d -m 0755 "$INSTALL_DIR" "$BIN_DIR" "$LOG_DIR" "$CONTENT_DIR" "$INSTALL_SUPPORT_DIR"

printf 'Downloading jar asset...\n'
download_file "$JAR_DOWNLOAD_URL" "${TMP_DIR}/app.jar"
install -m 0644 "${TMP_DIR}/app.jar" "$JAR_PATH"

printf 'Downloading config sample asset...\n'
download_file "$CONFIG_DOWNLOAD_URL" "${TMP_DIR}/config.ini-sample"
install -m 0644 "${TMP_DIR}/config.ini-sample" "$CONFIG_SAMPLE_PATH"

printf 'Downloading maintenance scripts...\n'
download_file "${INSTALL_SCRIPTS_RAW_BASE_URL}/uninstall.sh" "${TMP_DIR}/uninstall.sh"
download_file "${INSTALL_SCRIPTS_RAW_BASE_URL}/update.sh" "${TMP_DIR}/update.sh"
install -m 0755 "${TMP_DIR}/uninstall.sh" "${INSTALL_SUPPORT_DIR}/uninstall.sh"
install -m 0755 "${TMP_DIR}/update.sh" "${INSTALL_SUPPORT_DIR}/update.sh"

printf 'Generating config.ini...\n'
cat >"$CONFIG_PATH" <<EOF
# Generated by install/install.sh
# Reference sample downloaded from latest release:
# $(basename "$CONFIG_SAMPLE_PATH")

[application]
title = "${APP_TITLE}"
about = "${APP_ABOUT}"
uri = "${APP_URI}"

[server]
port = "${BIND_PORT}"
address = "${BIND_ADDRESS}"

[app]
timezone = "${TIMEZONE_VALUE}"

[db]
pool.max.size = "${DB_POOL_MAX_SIZE}"
pool.max.lifetime.min = "${DB_POOL_MAX_LIFETIME_MIN}"
pool.borrow.timeout.ms = "${DB_POOL_BORROW_TIMEOUT_MS}"

[db.agtydrive]
server = "${DB_HOST}"
port = "${DB_PORT}"
user = "${DB_USER}"
password = "${DB_PASSWORD}"
database = "${DB_NAME}"
schema = "${DB_SCHEMA}"

[bootstrap.admin]
login = "${BOOTSTRAP_ADMIN_LOGIN}"
password = "${BOOTSTRAP_ADMIN_PASSWORD}"
display_name = "${BOOTSTRAP_ADMIN_DISPLAY_NAME}"

[session]
timeout_minutes = "${SESSION_TIMEOUT_MINUTES}"
cookie_max_age_minutes = "${SESSION_COOKIE_MAX_AGE_MINUTES}"

[storage]
content_dir = "${CONTENT_DIR}"

[upload]
max_file_size = "${UPLOAD_MAX_FILE_SIZE}"
max_request_size = "${UPLOAD_MAX_REQUEST_SIZE}"
EOF
chmod 0600 "$CONFIG_PATH"

printf 'Generating launch script...\n'
cat >"$RUN_SCRIPT_PATH" <<EOF
#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_LOCATION="\$0"
cd "\`/usr/bin/dirname "\$SCRIPT_LOCATION"\`"

exec ${JAVA_BIN} -jar bin/${APP_NAME}.jar "\$@"
EOF
chmod 0755 "$RUN_SCRIPT_PATH"

printf 'Generating systemd service...\n'
cat >"$SERVICE_PATH" <<EOF
[Unit]
Description=${APP_DESCRIPTION}
After=network.target

[Service]
User=${SERVICE_USER}
WorkingDirectory=${INSTALL_DIR}
ExecStart=${RUN_SCRIPT_PATH}
SuccessExitStatus=143
TimeoutStopSec=10
Restart=always
RestartSec=5
StandardOutput=append:${LOG_DIR}/service.log
StandardError=append:${LOG_DIR}/service-error.log

[Install]
WantedBy=multi-user.target
EOF
chmod 0644 "$SERVICE_PATH"

printf 'Fixing ownership...\n'
chown -R "${SERVICE_USER}:${SERVICE_USER}" "$INSTALL_DIR"

printf 'Reloading and enabling service...\n'
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
systemctl restart "$SERVICE_NAME"

printf '\nInstallation complete.\n'
printf 'Application directory: %s\n' "$INSTALL_DIR"
printf 'Launch script: %s\n' "$RUN_SCRIPT_PATH"
printf 'Service: %s\n' "$SERVICE_NAME"
printf 'Config: %s\n' "$CONFIG_PATH"
printf 'Sample config: %s\n' "$CONFIG_SAMPLE_PATH"
printf 'Logs: %s\n' "$LOG_DIR"
printf 'Maintenance scripts: %s\n' "$INSTALL_SUPPORT_DIR"
printf '\nWait about 30 seconds for the service to finish starting.\n'
printf '\nConnection details:\n'
printf '  URL: %s\n' "$APP_URI"
printf '  Login: %s\n' "$BOOTSTRAP_ADMIN_LOGIN"
printf '  Password: %s\n' "$BOOTSTRAP_ADMIN_PASSWORD"
printf '  Note: on a non-first installation with an existing database, use the login and password already stored in the database.\n'
printf '\nUseful commands:\n'
printf '  sudo systemctl status %s\n' "$SERVICE_NAME"
printf '  sudo systemctl start %s\n' "$SERVICE_NAME"
printf '  sudo systemctl stop %s\n' "$SERVICE_NAME"
printf '  sudo systemctl restart %s\n' "$SERVICE_NAME"
printf '  sudo journalctl -u %s -f\n' "$SERVICE_NAME"
printf '  sudo bash %s/update.sh\n' "${INSTALL_SUPPORT_DIR}"
printf '  sudo bash %s/uninstall.sh\n' "${INSTALL_SUPPORT_DIR}"
