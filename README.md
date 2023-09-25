![NadBin](nadbin.png)

Temporary anonymous file sharing

**NadBin is still in development and not ready for production use.**  
And the code is very shitty, I'm working on it, I promise :D

## Why NadBin?

-   Extremely easy to setup
-   Direct downloads without redirects
-   Very lightweight (at least on the client side)

![NadBin index page](nadbin-index.png)

[Demo instance](https://nadbin.nadwey.pl)

## Setup

### Install dependencies and run

> Bun won't work because of [this](https://github.com/oven-sh/bun/issues/5265) issue.

To install dependencies:

```bash
npm install
```

To run:

```bash
npm start
```

### Configuration

Copy .env.example to .env and edit it.

#### Variables

| Name               | Description                                      | Default value |
| ------------------ | ------------------------------------------------ | ------------- |
| `PORT`             | Port on which NadBin will listen                 | `7000`        |
| `PHUSIONPASSENGER` | Set to `true` if you are using Phusion Passenger | `false`       |

### Webserver configuration

#### Caddy

```caddy
example.com {
    reverse_proxy localhost:7000
}
```

#### Nginx

```nginx
server {
    listen 80;
    listen [::]:80;

    server_name example.com; # your domain

    # redirect to https
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;

    client_max_body_size 10000M; # max file size that is accepted by Nginx

    server_name example.com; # your domain

    sendfile           on;
    sendfile_max_chunk 10m;

    ssl_certificate /path/to/your/cert.pem;
    ssl_certificate_key /path/to/your/privkey.pem;

    location / {
            proxy_pass http://localhost:7000;
    }

    # disable logging
    access_log off;
    error_log /dev/null;
}
```
