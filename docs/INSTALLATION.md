# Установка и публикация

## Что нужно для сервера

- Linux с `systemd`
- Java 21
- PostgreSQL
- доступ к GitHub для скачивания `latest` release

## Что делает установщик

Сценарий [install/install.sh](../install/install.sh):

- проверяет версию Java;
- спрашивает сетевые параметры приложения;
- спрашивает параметры PostgreSQL;
- проверяет подключение к базе;
- скачивает jar и `config.ini-sample` из `latest` release;
- создает рабочий `config.ini`;
- создает `systemd` service и включает его.

## Как публиковать release

1. Соберите jar через `./mvnw package`.
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
- должен обновляться одновременно с изменениями рабочего `config.ini`.
