# Docker и PostgreSQL

Этот документ показывает один из простых способов подготовить PostgreSQL для AGTY/DRIVE через Docker.

## 1. Установить Docker

Пример для Ubuntu:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

Проверка:

```bash
sudo docker version
sudo docker ps
```

Если хотите запускать Docker без `sudo`, добавьте пользователя в группу `docker` и перелогиньтесь:

```bash
sudo usermod -aG docker "$USER"
```

## 2. Запустить PostgreSQL в Docker

Если хотите, можно использовать готовый скрипт из репозитория:

```bash
sudo POSTGRES_PASSWORD='change-me' bash docs/install-postgres-docker.sh
```

По умолчанию скрипт запускает `postgres:18`.

Если нужен другой порт на хосте, задайте его через `HOST_PORT`, например:

```bash
sudo HOST_PORT=55432 POSTGRES_PASSWORD='change-me' bash docs/install-postgres-docker.sh
```

Если хотите запустить контейнер вручную, используйте команды ниже.

Создайте директорию для данных:

```bash
sudo mkdir -p /opt/agtydrive/postgres-data
sudo chown -R 999:999 /opt/agtydrive/postgres-data
```

Запустите контейнер:

```bash
sudo docker run -d \
  --name agtydrive-postgres \
  --restart unless-stopped \
  -e POSTGRES_DB=agtydrive \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=change-me \
  -p 5432:5432 \
  -v /opt/agtydrive/postgres-data:/var/lib/postgresql/data \
  postgres:18
```

Проверка:

```bash
sudo docker ps
sudo docker logs agtydrive-postgres --tail 50
```

## 3. Проверить подключение к базе

Пример проверки через `psql` на хосте:

```bash
psql "host=127.0.0.1 port=5432 dbname=agtydrive user=postgres password=change-me" -c "select 1;"
```

Если `psql` еще не установлен:

```bash
sudo apt update
sudo apt install -y postgresql-client
```

## 4. Какие значения потом вводить в install.sh

Если использовали команды выше, в установщик обычно нужно вводить:

- `PostgreSQL host`: `127.0.0.1`
- `PostgreSQL port`: `5432`
- `PostgreSQL database`: `agtydrive`
- `PostgreSQL schema`: `public`
- `PostgreSQL user`: `postgres`
- `PostgreSQL password`: тот пароль, который вы указали в `POSTGRES_PASSWORD`

## 5. Если нужен отдельный пользователь для приложения

Подключитесь к контейнеру:

```bash
sudo docker exec -it agtydrive-postgres psql -U postgres -d agtydrive
```

И выполните SQL:

```sql
CREATE USER agtydrive_app WITH PASSWORD 'strong-password';
GRANT ALL PRIVILEGES ON DATABASE agtydrive TO agtydrive_app;
```

После этого можно использовать `agtydrive_app` в `install.sh`.
