# AGTY/DRIVE

![main.png](docs/screenshots/main.png)

AGTY/DRIVE is a self-hosted web application for storing, previewing, and sharing files. Its usage model is close to Yandex Disk and Google Drive: users upload files in a browser, organize them into folders, switch between list and grid views, preview media, and publish secure links when needed. The main difference is control: AGTY/DRIVE runs on your own server.

The product direction follows the same core ideas promoted by major cloud drives in their official descriptions: reliable file storage, browser access, previews, link sharing, and convenient media handling. AGTY/DRIVE brings those workflows into a private deployment you control.

## Features

- personal file space with virtual folders;
- `List` and `Grid` views;
- file upload via form, drag-and-drop, and clipboard paste with `Ctrl+V`;
- multi-file upload queue with progress and statuses;
- overwrite confirmation when a file with the same name already exists;
- previews for images, video, audio, and text files;
- dedicated photo, video, and shared-link libraries;
- file download and folder download as archive;
- public links for files and folders;
- password protection and expiration for public links;
- file or folder lifetime with automatic deletion;
- automatic cleanup of public links and shared access after expiration;
- invite-based registration and admin user management;
- light and dark themes;
- PostgreSQL for metadata and filesystem storage for file content.

## Good Fit

AGTY/DRIVE works well for:

- small teams that need a private file service instead of external SaaS;
- restricted environments where data must stay on internal infrastructure;
- internal portals and secure file exchange workflows;
- temporary file delivery with automatic cleanup.

## Main Use Cases

### Store and organize files
Users sign in, create folders, upload files, and work with them entirely from the browser.

### Preview before download
Images, videos, audio files, and text files can be opened in a modal preview without downloading them first.

### Share safely
Files and folders can be shared through public links with optional password protection, expiry, and preview/download permissions.

### Temporary uploads
Files and folders can have a lifetime. Once it expires, the resource is removed automatically together with related public links and sharing state.

## Why self-hosted

- file data stays on your infrastructure;
- you control PostgreSQL, storage paths, upload limits, and network settings;
- it can be used inside a local or corporate network;
- the product is optimized for controlled deployment rather than public-cloud distribution.

## Technology Stack

- Java 21
- Spring Boot 4
- Spring Security
- Thymeleaf
- PostgreSQL
- Flyway
- Maven Wrapper

## Automated Linux Installation

The repository includes `install/install.sh`, which:

- verifies Java 21;
- asks for PostgreSQL and application settings;
- checks database connectivity;
- downloads assets from the GitHub `latest` release;
- creates application directories;
- downloads the jar and `config.ini-sample`;
- generates a working `config.ini`;
- creates and enables a `systemd` service.

See [install/RELEASE_ASSETS.md](install/RELEASE_ASSETS.md) for release asset requirements.

## Step-by-Step Installation

### 1. Install Java and basic shell tools

Minimum supported Java version: `21`.

You can install any Java version `>= 21`.

Example for Ubuntu:

```bash
sudo apt update
sudo apt install -y openjdk-21-jre curl bash coreutils sed grep gawk
java -version
```

If you plan to build the project on the server, you can install `openjdk-21-jdk` instead of `openjdk-21-jre`.

### 2. Prepare PostgreSQL

The recommended quick-start option is PostgreSQL in Docker.

The detailed guide is available in a separate document:

- [docs/DOCKER_POSTGRESQL.md](docs/DOCKER_POSTGRESQL.md)

That document explains:

- how to install Docker;
- how to run PostgreSQL in a container;
- how to verify the database connection;
- which values to enter later in `install.sh`.

### 3. Download `install.sh` and run it with `sudo`

Example with explicit download:

```bash
mkdir -p ~/agtydrive-install
cd ~/agtydrive-install
curl -fsSL -o install.sh https://raw.githubusercontent.com/agtymc/org.agty.drive/master/install/install.sh
chmod +x install.sh
sudo ./install.sh
```

During installation, the script will ask a series of questions.

First, it prints service messages such as:

```text
Java check passed: openjdk version "25.0.3" 2026-04-21
Installer will create application directories, download latest release assets, generate config.ini and register a systemd service.
```

Then it asks for installation parameters in sequence:

- `Install directory`
  Installation path, for example `/opt/org.agty.drive`.
- `System user for service`
  The system user that will run the service, for example `agarty`.
- `PostgreSQL host`
  PostgreSQL host, for example `localhost` or `127.0.0.1`.
- `PostgreSQL port`
  PostgreSQL port, usually `5432`.
- `PostgreSQL database`
  Database name, for example `agtydrive`.
- `PostgreSQL schema`
  PostgreSQL schema name, usually `public`.
- `PostgreSQL user`
  PostgreSQL user used by the application.
- `PostgreSQL password`
  Password for that PostgreSQL user.
- `Application bind IP`
  The address the application will listen on. If you set `0.0.0.0`, the app will listen on all interfaces.
- `Application bind port`
  Application port, for example `8080` or `8090`.
- `Application title`
  UI title shown in the application, for example your organization name.
- `Application subtitle/about`
  Subtitle or text shown on the login page.
- `Public application URI`
  Public application address, for example `https://drive.example.com`.
- `Application timezone`
  Application timezone, for example `Europe/Moscow`.
- `Content storage directory`
  Server directory where uploaded files will be stored, for example `/opt/org.agty.drive/content`.
- `Max single file upload size`
  Maximum size of one uploaded file, for example `1024MB`.
- `Max request upload size`
  Maximum total size of files in one request.

After that, the script prints technical progress messages such as:

```text
Checking PostgreSQL connection...
PostgreSQL connection check passed.
Requesting latest GitHub release metadata...
Creating directories...
Downloading jar asset...
Downloading config sample asset...
Downloading maintenance scripts...
Generating config.ini...
Generating launch script...
Generating systemd service...
Fixing ownership...
Reloading and enabling service...
Created symlink /etc/systemd/system/multi-user.target.wants/org.agty.drive.service → /etc/systemd/system/org.agty.drive.service.
```

When installation is complete, the script prints final information: paths, generated files, and useful follow-up commands:

```text
Installation complete.
Application directory: /opt/org.agty.drive
Launch script: /opt/org.agty.drive/org.agty.drive.sh
Service: org.agty.drive.service
Config: /opt/org.agty.drive/config.ini
Sample config: /opt/org.agty.drive/config.ini-sample
Logs: /opt/org.agty.drive/logs
Maintenance scripts: /opt/org.agty.drive/install
```

Before using the application, wait until the service finishes starting:

```text
Wait about 30 seconds for the service to finish starting.

Connection details:
URL: http://127.0.0.1:8090
Login: admin
Password: admin
Note: on a non-first installation with an existing database, use the login and password already stored in the database.
```

Use the following commands to manage the service:

```text
Useful commands:
sudo systemctl status org.agty.drive.service
sudo systemctl start org.agty.drive.service
sudo systemctl stop org.agty.drive.service
sudo systemctl restart org.agty.drive.service
sudo journalctl -u org.agty.drive.service -f
```

To update or remove the installed service, use:

```text
sudo bash /opt/org.agty.drive/install/update.sh
sudo bash /opt/org.agty.drive/install/uninstall.sh
```

### 4. One-line command: download and run install immediately

If you do not want to save the script manually:

```bash
curl -fsSL https://raw.githubusercontent.com/agtymc/org.agty.drive/master/install/install.sh -o /tmp/agtydrive-install.sh && chmod +x /tmp/agtydrive-install.sh && sudo /tmp/agtydrive-install.sh
```

After installation, the script:

- downloads the `latest` release;
- creates `config.ini`;
- registers the `systemd` service;
- starts the application.

## How update works

To update the installed service, run:

```bash
sudo bash /opt/org.agty.drive/install/update.sh
```

During update, the script:

- downloads the new jar from the GitHub `latest` release;
- refreshes `config.ini-sample`;
- refreshes the maintenance scripts `update.sh` and `uninstall.sh`;
- restarts the `systemd` service.

### How update interacts with config

`update.sh` does not overwrite the working `config.ini` automatically.

Instead, it works like this:

- keeps the current `config.ini` unchanged;
- downloads the new `config.ini-sample`;
- creates `config.ini.merged`
  this is the new sample with your current values automatically applied for matching keys;
- creates `config.deprecated-keys.txt`
  this file contains keys that exist in the current `config.ini` but are missing from the new sample.

If `config.ini.merged` differs from the current `config.ini`, the script explicitly tells you and asks whether the working config should be replaced.

If you confirm:

- a backup file like `config.ini.bak.YYYYMMDD-HHMMSS` is created;
- the current `config.ini` is replaced with `config.ini.merged`.

If you decline:

- the current `config.ini` stays unchanged;
- `config.ini.merged` remains available for manual review.

`config.deprecated-keys.txt` is especially important during updates:

- it shows keys that no longer exist in the new sample;
- that can mean a parameter was removed;
- that can also mean a parameter was renamed and should be migrated manually to a new key.

## Additional Documentation

- [docs/DOCKER_POSTGRESQL.md](docs/DOCKER_POSTGRESQL.md) - Docker and PostgreSQL setup for AGTY/DRIVE
- [docs/MAVEN.md](docs/MAVEN.md) - running, building, and testing the project through Maven Wrapper
- [docs/USER_MANUAL.md](docs/USER_MANUAL.md) - end-user manual

## GitHub Release Assets

For the installer to work, the GitHub `latest` release must contain:

- the application jar file;
- `config.ini-sample`.

Detailed naming and publishing rules are described in [install/RELEASE_ASSETS.md](install/RELEASE_ASSETS.md).

## License

This project is distributed under the Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
