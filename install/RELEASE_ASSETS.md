# Latest Release Assets

`install/install.sh` downloads files only from GitHub `latest`.

Prepare release assets like this:

1. Create a GitHub Release and mark it as `latest`.
2. Upload the application jar file.
Recommended name: `org-agty-drive-<version>.jar`
3. Upload `config.ini-sample`.
Recommended content: comments and example keys only, without production passwords.

Recommended `config.ini-sample` keys:

```ini
[application]
title = "AGTY/DRIVE"
about = "Private file storage and sharing workspace"
uri = "http://127.0.0.1:8080"

[server]
port = "8080"
address = "0.0.0.0"

[app]
timezone = "Europe/Moscow"

[db]
pool.max.size = "32"
pool.max.lifetime.min = "30"
pool.borrow.timeout.ms = "300"

[db.agtydrive]
server = "localhost"
port = "5432"
user = "postgres"
password = "change-me"
database = "agtydrive"
schema = "public"

[bootstrap.admin]
login = "admin"
password = "change-me"
display_name = "Administrator"

[session]
timeout_minutes = "43200"
cookie_max_age_minutes = "43200"

[storage]
content_dir = "/opt/org.agty.drive/content"

[upload]
max_file_size = "1024MB"
max_request_size = "1024MB"
```

Installer behavior:

- jar asset: first asset in `latest` ending with `.jar`
- config asset: `config.ini-sample` preferred, then `config.ini.sample`, then `config.ini`
- downloaded jar is saved as `bin/org.agty.drive.jar`
- downloaded sample is saved as `config.ini-sample`
- working `config.ini` is generated from installer answers
- `uninstall.sh` and `update.sh` are downloaded from `https://raw.githubusercontent.com/agtymc/org.agty.drive/master/install/`
- downloaded maintenance scripts are saved into `<install-dir>/install/`
- `update.sh` always refreshes both maintenance scripts before finishing
- `update.sh` does not overwrite the working `config.ini`; it only refreshes `config.ini-sample`
- if `<install-dir>/config.ini` exists, `update.sh` also generates `<install-dir>/config.ini.merged`
- `config.ini.merged` is built from the new sample and reuses values from the current `config.ini` for matching keys
- `update.sh` also writes `<install-dir>/config.deprecated-keys.txt` with keys that exist in the current config but no longer exist in the new sample; these keys should be reviewed because they can indicate removed or renamed parameters
- during update, the script prints a configuration report so the operator can see what happened
- if `config.ini.merged` differs from the current `config.ini`, `update.sh` asks whether to replace the working config with the merged one
- if the operator confirms, `update.sh` creates a backup file like `<install-dir>/config.ini.bak.YYYYMMDD-HHMMSS` before replacing `config.ini`
- if the operator declines, the working `config.ini` is kept unchanged and `config.ini.merged` remains available for manual review
