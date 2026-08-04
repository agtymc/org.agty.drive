# Установка и публикация

Этот документ описывает установку AGTY/DRIVE как готового jar-приложения на сервер Linux.

## Что нужно для сервера

- Linux с `systemd`
- Java `>= 21`
- PostgreSQL
- доступ к GitHub для скачивания `latest` release

## Рекомендуемый путь установки

Основной способ установки: использовать готовый сценарий [install/install.sh](../install/install.sh).

Он:

- проверяет Java;
- запрашивает параметры PostgreSQL и приложения;
- проверяет подключение к базе;
- скачивает jar и `config.ini-sample` из GitHub `latest` release;
- создает рабочий `config.ini`;
- создает и включает `systemd` service;
- запускает приложение.

## Подготовка PostgreSQL

Если PostgreSQL еще не развернут, рекомендуемый быстрый вариант описан здесь:

- [docs/DOCKER_POSTGRESQL.md](DOCKER_POSTGRESQL.md)

В этом документе показано:

- как установить Docker;
- как запустить PostgreSQL в контейнере;
- как проверить подключение;
- какие значения потом вводить в `install.sh`.

## Как запустить установщик

### Вариант 1. Скачать `install.sh` и запустить вручную

```bash
mkdir -p ~/agtydrive-install
cd ~/agtydrive-install
curl -fsSL -o install.sh https://raw.githubusercontent.com/agtymc/org.agty.drive/master/install/install.sh
chmod +x install.sh
sudo ./install.sh
```

### Вариант 2. Скачать и запустить одной командой

```bash
curl -fsSL https://raw.githubusercontent.com/agtymc/org.agty.drive/master/install/install.sh -o /tmp/agtydrive-install.sh && chmod +x /tmp/agtydrive-install.sh && sudo /tmp/agtydrive-install.sh
```

## Как работает update

Для обновления установленного сервиса используйте:

```bash
sudo bash /opt/org.agty.drive/install/update.sh
```

Во время обновления скрипт:

- скачивает новый jar из `latest` release;
- обновляет `config.ini-sample`;
- обновляет `update.sh` и `uninstall.sh`;
- перезапускает `systemd` service.

### Как update работает с конфигом

`update.sh` не перезаписывает рабочий `config.ini` автоматически.

Вместо этого он:

- сохраняет текущий `config.ini` без изменений;
- скачивает новый `config.ini-sample`;
- создает `config.ini.merged`;
- создает `config.deprecated-keys.txt`.

`config.ini.merged` нужен для безопасного переноса значений в новую структуру sample-конфига.

`config.deprecated-keys.txt` показывает ключи, которые есть в текущем конфиге, но отсутствуют в новом sample. Это важный сигнал, что параметр мог быть удален или переименован.

Если merged-конфиг отличается от текущего рабочего конфига, скрипт предложит:

- создать backup `config.ini.bak.YYYYMMDD-HHMMSS`;
- заменить `config.ini` на `config.ini.merged`.

Если вы не подтверждаете замену, рабочий `config.ini` остается без изменений.

## Публикация через Nginx

Если приложение доступно через Nginx, в проксирующем `location` необходимо отключить буферизацию тела запроса:

```nginx
location / {
    proxy_http_version 1.1;
    proxy_request_buffering off;
    proxy_pass http://127.0.0.1:8091;
}
```

Без `proxy_request_buffering off` Nginx сначала полностью принимает большой файл во временное хранилище и только затем передает его приложению. Это приводит к дополнительной паузе на `95%` после завершения передачи файла браузером.

Значение `client_max_body_size` в Nginx должно быть не меньше лимитов из секции `[upload]` файла `config.ini`.

Полная конфигурация HTTPS, описание цепочки временных файлов и команды проверки находятся в [NGINX.md](NGINX.md).

## Как публиковать release

1. Подготовьте jar приложения.
Если вы работаете с исходниками, команды Maven вынесены в [docs/MAVEN.md](MAVEN.md).
2. Создайте GitHub Release.
3. Отметьте его как `latest`.
4. Загрузите в release:
   - jar-файл приложения;
   - `config.ini-sample`.

Подробные требования смотрите в [install/RELEASE_ASSETS.md](../install/RELEASE_ASSETS.md).

## Почему нужен `config.ini-sample`

Этот файл:

- служит эталоном всех поддерживаемых параметров;
- не должен содержать реальные пароли;
- используется как release asset для автоматической установки;
- используется `update.sh` для построения `config.ini.merged`;
- должен обновляться одновременно с изменениями рабочего `config.ini`.
