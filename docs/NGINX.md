# Настройка Nginx

Этот документ описывает публикацию AGTY/DRIVE через Nginx с HTTPS и корректной потоковой загрузкой больших файлов.

## Почему важен `proxy_request_buffering`

По умолчанию Nginx сначала полностью принимает тело запроса от браузера и сохраняет большой файл во временное хранилище Nginx. Только после этого запрос передается приложению, где Tomcat формирует multipart-файл в `.upload-staging`.

Без дополнительной настройки загрузка проходит последовательно:

```text
браузер -> временный файл Nginx -> .upload-staging Tomcat -> итоговое хранилище
```

В интерфейсе это выглядит как пауза на `95%`: браузер уже передал файл Nginx, но Nginx еще отправляет его приложению.

Для потоковой передачи запросов обязательно отключите буферизацию тела запроса:

```nginx
proxy_request_buffering off;
```

После этого Nginx передает данные Tomcat по мере получения:

```text
браузер -> Nginx -> .upload-staging Tomcat -> итоговое хранилище
```

## Рекомендуемая конфигурация

Замените домен, порт приложения и пути к сертификатам своими значениями:

```nginx
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;

    server_name drive.example.com;

    client_max_body_size 1024M;

    ssl_certificate /etc/letsencrypt/live/drive.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/drive.example.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    location / {
        proxy_http_version 1.1;
        proxy_request_buffering off;
        proxy_pass http://127.0.0.1:8091;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port 443;
        proxy_set_header Connection "";

        proxy_connect_timeout 60s;
        proxy_send_timeout 600s;
        proxy_read_timeout 600s;
        send_timeout 600s;
    }
}
```

`client_max_body_size` должен быть не меньше значений `upload.max_file_size` и `upload.max_request_size` из `config.ini`. Иначе Nginx отклонит запрос до того, как он попадет в приложение.

Не заменяйте `proxy_request_buffering off` на `proxy_buffering off`. Параметр `proxy_buffering` управляет буферизацией ответа приложения и не устраняет задержку при загрузке файла.

## Применение конфигурации

Сначала проверьте синтаксис, затем перечитайте конфигурацию без остановки Nginx:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Проверить эффективное значение параметра можно командой:

```bash
sudo nginx -T | grep -n "proxy_request_buffering"
```

## Проверка загрузки

После применения настройки:

- файл в `.upload-staging` появляется вскоре после начала загрузки;
- его размер увеличивается одновременно с прогрессом в браузере;
- после окончания передачи временный multipart-файл перемещается в итоговое хранилище;
- длительной паузы на `95%` для обычного файла или видео быть не должно.

Один файл в `.upload-staging` для большого загружаемого файла является нормальной частью стандартной обработки `MultipartFile`. AGTY/DRIVE не создает дополнительную временную копию содержимого и не вычисляет SHA-256 загруженного файла.

При `proxy_request_buffering off` соединение с приложением остается занятым в течение всей загрузки, а Nginx не сможет повторно отправить уже начатый запрос другому upstream-серверу. Для одной локальной точки `proxy_pass`, используемой AGTY/DRIVE, это ожидаемый режим работы.

Описание параметров также доступно в официальной документации Nginx: [`proxy_request_buffering`](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_request_buffering) и [`client_max_body_size`](https://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size).
