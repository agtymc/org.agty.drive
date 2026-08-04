# Changelog

## Unreleased

## 1.1.6 - 2026-08-04

### Changed
- Video player now has separate SVG actions for `expand to window width` and `fullscreen`.
- Fullscreen video mode now hides the control overlay together with the mouse cursor after inactivity.

### Improved
- Video player action buttons were visually aligned with the existing volume control.

### Documentation
- Added a production Nginx configuration for streaming large uploads with `proxy_request_buffering off`.
- Documented the cause of the upload pause at `95%`, request-size limits, and configuration verification steps.

## 1.1.3 - 2026-08-04

### Changed
- Upload storage flow no longer writes an extra application-managed temporary copy before moving files into final storage.
- Stored file names are now generated without computing `SHA-256` from file contents.

### Improved
- Large file and video uploads perform better when application storage is placed on a local disk.

## 1.1.2 - 2026-07-31

### Added
- `Настроить` action for WebDAV entries inside `Совместный доступ`.
- Application version display under the yellow storage block in the left sidebar.

### Fixed
- WebDAV configuration can be reopened from `Совместный доступ` with current connection parameters.

## 1.1.1 - 2026-07-31

### Added
- Folder-level WebDAV in `Файлы` with separate login and password per folder.
- WebDAV setup in the folder actions menu under `Доступ > Настроить WebDAV`.
- WebDAV connection details in the setup modal, including `https`, `dav(s)` and `webdav(s)` addresses.
- WebDAV indicator in the left status column for folders with active WebDAV.
- WebDAV section at the top of `Совместный доступ` for folders with active WebDAV.
- README and docs coverage for WebDAV usage and client connection.

### Changed
- Access actions were grouped into the `Доступ >` submenu.
- Submenu order updated to `Публичная ссылка`, `Совместный доступ`, `Настроить WebDAV`.
- Rename and move actions were placed on one row in the item actions menu.
- Labels updated from `Изменить имя объекта` to `Изменить имя` and from `Выбрать другую директорию` to `В другую папку`.
- WebDAV modal layout was tightened to keep connection fields and copy buttons inside modal bounds.
- Browser opening of WebDAV folder URLs now returns an HTML listing instead of `405`.
- WebDAV connection block no longer shows the login as a separate copy field.

### Fixed
- WebDAV deletion now removes stored access data completely instead of leaving a disabled record in the database.
- WebDAV UI state after deletion no longer shows stale connection details from disabled records.
- Left-side status indicators were aligned and tuned visually for link, collaborative, and WebDAV markers.
