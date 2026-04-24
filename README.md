# akihz

A lightweight Android refresh rate manager with a Quick Settings tile. No root required — works through [Shizuku](https://shizuku.rikka.app/).

## Features

- **Dynamic detection** — automatically reads your display's supported refresh rates instead of using hardcoded values
- **Quick Settings tile** — tap to cycle through rates instantly; long-press to open the app
- **Multi-OEM support** — works on Xiaomi, Samsung, Pixel, and other devices by targeting the correct system settings keys
- **Instant switching** — no artificial delays; the rate changes as soon as you tap

## Requirements

- Android 11+ (API 30)
- [Shizuku](https://shizuku.rikka.app/) installed and running

## Install

Download the latest APK from [Releases](https://github.com/anlaki-py/akihz/releases).

## Usage

1. Open Shizuku and start it (wireless debugging or ADB)
2. Open akihz and grant Shizuku permission
3. Select a refresh rate from the buttons, or add the **akihz** tile to your Quick Settings panel and tap it to cycle

## Build

```bash
./gradlew assembleRelease
```

## License

[MIT](LICENSE)
