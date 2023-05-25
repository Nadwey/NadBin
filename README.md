![NadBin](nadbin.png)

Temporary anonymous file sharing

**NadBin is still in development and not ready for production use.**  
And the code is very shitty, I'm working on it, I promise :D

## Why NadBin?

- Extremely easy to setup
- Direct downloads without redirects
- Very lightweight (at least on the client side)

![NadBin index page](nadbin-index.png)

## Setup

### Requirements

- Java 11+ (8 will not work because of Javalin)

### Install & Run

- Download the latest release from the [releases page](https://github.com/Nadwey/NadBin/releases/latest)
- Run the jar file with `java -jar NadBin-<VERSION>-all.jar`
- The server will be available at `http://localhost:7000`

### Nginx configuration

#### Without SSL

```nginx
server {
        listen 80;
        listen [::]:80;

        client_max_body_size 10000M; # max file size that is accepted by Nginx

        server_name example.com; # your domain

        location / {
                proxy_pass http://127.0.0.1:7000;
        }

        # disable logging
        access_log off;
        error_log /dev/null;
}
```

#### With SSL

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