FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
      docker.io curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ARG TARGETARCH
RUN case "$TARGETARCH" in \
      arm64) ARCH=aarch64 ;; \
      amd64) ARCH=x86_64 ;; \
      *) ARCH="$TARGETARCH" ;; \
    esac \
    && curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-${ARCH}" \
       -o /usr/local/bin/docker-compose \
    && chmod +x /usr/local/bin/docker-compose

RUN pip install --no-cache-dir flask==3.0.3

WORKDIR /app
COPY app.py .
EXPOSE 8081
CMD ["python", "app.py"]
