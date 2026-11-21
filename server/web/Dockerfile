FROM node:20-bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
      git python3 make g++ android-tools-adb ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN git clone --depth 1 https://github.com/NetrisTV/ws-scrcpy.git .

# appium opsional tidak diperlukan untuk Android
RUN npm install --omit=optional
RUN npm run dist

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8000
ENTRYPOINT ["/entrypoint.sh"]
