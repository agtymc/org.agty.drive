# AGTY/DRIVE

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

## Developer Quick Start

### Requirements

- Java 21
- PostgreSQL
- Linux/macOS/WSL for the standard shell-based install flow

### Configuration

1. Copy `config.ini-sample` to `config.ini`.
2. Fill in database, network, admin, and storage settings.
3. Make sure PostgreSQL is reachable and ready.

### Run locally

```bash
./mvnw spring-boot:run
```

### Build

```bash
./mvnw package
```

The executable jar will be generated under `target/`.

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

## Important Paths

- `src/main/java` - backend source code
- `src/main/resources/templates` - Thymeleaf templates
- `src/main/resources/static` - CSS and JavaScript
- `config.ini-sample` - reference configuration without secrets
- `install/install.sh` - automated Linux installer
- `docs/USER_MANUAL.md` - end-user manual
- `!files/` - internal materials and archived technical tasks

## GitHub Release Assets

For the installer to work, the GitHub `latest` release must contain:

- the application jar file;
- `config.ini-sample`.

Detailed naming and publishing rules are described in [install/RELEASE_ASSETS.md](install/RELEASE_ASSETS.md).

## License

This project is distributed under a permissive attribution-based license. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

## Product Reference Sources

The product description was informed by official product pages:

- Yandex Disk: https://360.yandex.com/disk/
- Yandex 360 help: https://yandex.com/support/yandex-360/customers/disk/web/en/
- Google Drive: https://workspace.google.com/products/drive/
