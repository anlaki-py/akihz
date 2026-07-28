<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="akiHz app icon" width="160">

  <h1><strong>akiHz</strong></h1>

  <h3>Android refresh rate switcher (FOSS)</h3>

  <p><strong>akiHz</strong> (pronounced "akiHertz") is a lightweight, open-source Android app that lets you instantly change your device's refresh rate (60Hz / 90Hz / 120Hz / etc.) using <a href="https://shizuku.rikka.app/">Shizuku</a>. No root required.</p>

  <p>Features a Quick Settings tile for one-tap cycling, automatic detection of supported refresh rates, and OEM-specific settings support.</p>

  <p>
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT"></a>
    <a href="app/build.gradle.kts"><img src="https://img.shields.io/badge/Android-11%2B-green.svg" alt="Android 11+"></a>
    <a href="https://github.com/anlaki-py/akihz/releases/latest"><img src="https://img.shields.io/github/v/release/anlaki-py/akihz?label=release" alt="Latest release"></a>
  </p>
</div>

---

## Screenshots

| Home (light) | Home (dark) |
|:---:|:---:|
| ![Home screen in light theme](screenshots/home_light.jpg) | ![Home screen in dark theme](screenshots/home_dark.jpg) |

| Settings (light) | Settings (dark) |
|:---:|:---:|
| ![Settings screen in light theme](screenshots/settings_light.jpg) | ![Settings screen in dark theme](screenshots/settings_dark.jpg) |

## Features

- **Dynamic detection** — automatically reads your display's supported refresh rates instead of using hardcoded values
- **Quick Settings tile** — tap to cycle through rates instantly; long-press to open the app
- **Multi-OEM support** — targets the correct system settings keys per manufacturer
- **Instant switching** — no artificial delays; the rate changes as soon as you tap
- **Watchdog** (optional, untested on all devices) — monitors and re-applies your refresh rate when the system overrides it
- **OEM override** — manually pick a device profile in Settings if auto-detection does not match your phone

## Supported devices

akiHz auto-detects your manufacturer and applies the right system settings keys. Android 11+ and Shizuku are required on all devices.

| Manufacturer | Brands / notes |
|---|---|
| **Xiaomi** | Xiaomi, Redmi (many POCO devices use Xiaomi keys; MIUI / HyperOS) |
| **Samsung** | Galaxy phones (One UI) |
| **Google** | Pixel (stock + `smooth_display` keys) |
| **OnePlus** | OxygenOS / ColorOS builds |
| **OPPO** | OPPO devices |
| **realme** | realme devices |
| **vivo** | vivo, iQOO (FunTouch OS) |
| **ASUS** | ASUS, ROG Phone |
| **Motorola** | moto devices |
| **Sony** | Xperia |
| **Stock Android** | Fallback for other OEMs using standard AOSP keys |

If auto-detection fails, open **Settings → Advanced → OEM override** and select your manufacturer manually.

## Requirements

- Android 11+ (API 30)
- [Shizuku](https://shizuku.rikka.app/) installed and running

## Install

Download the latest APK from [Releases](https://github.com/anlaki-py/akihz/releases).

Each release provides standalone APKs for `arm64-v8a`, `armeabi-v7a`, `x86`,
and `x86_64`, plus a `universal` APK. Most modern phones use `arm64-v8a`;
use `universal` if you are unsure.

Releases from `main` are stable. Releases from `beta` are marked as GitHub
prereleases. Both channels use the same application ID and signing key, and
monotonically increasing version codes, so a newer beta can update an installed
stable release without uninstalling it.

GitHub release assets use predictable names and include SHA-256 checksums and a
machine-readable `release-metadata.json` file for release indexers and Android
update clients.

See [CHANGELOG.md](CHANGELOG.md) for the complete release history.

## Usage

1. Open Shizuku and start it (wireless debugging or ADB)
2. Open akiHz and grant Shizuku permission
3. Select a refresh rate from the buttons, or add the **akiHz** tile to your Quick Settings panel and tap it to cycle

## FAQ

**Why does Shizuku need to be running?**  
Android blocks normal apps from changing refresh-rate settings directly. Shizuku grants akiHz the privileged access needed to update the correct OEM-specific keys.

**Does the Quick Settings tile work when the app is closed?**  
Yes. Android starts the Quick Settings tile service when you tap the tile, so akiHz does not need to run a permanent keep-alive service. The optional watchdog uses a foreground service only while enabled.

**What does the watchdog do?**  
It periodically checks whether the system changed your refresh rate (for example after reboot) and writes your selected rate back. Enable it under **Settings → Watchdog**. This feature is untested on all devices.

**My phone is not switching rates correctly — what should I try?**  
1. Confirm Shizuku is running and akiHz has permission  
2. Try **Settings → Advanced → OEM override** and pick your manufacturer  
3. Open a [GitHub issue](https://github.com/anlaki-py/akihz/issues) with your device model and Android version

**Is akiHz safe?**  
The app is MIT-licensed and fully open source. It only changes display refresh-rate related system settings — review the code on GitHub if you want to verify behavior.

## Contributing and feedback

Contributions are welcome. [Open an issue](https://github.com/anlaki-py/akihz/issues/new) to report a bug or suggest an improvement, or [submit a pull request](https://github.com/anlaki-py/akihz/pulls) with a change.

## Build

```bash
./gradlew assembleDebug
```

## License

[MIT](LICENSE)
