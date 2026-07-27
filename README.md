# akiHz — Android refresh rate switcher (FOSS)

**akiHz** is a lightweight, open-source Android app that lets you instantly change your device's refresh rate (60Hz / 90Hz / 120Hz / etc.) using [Shizuku](https://shizuku.rikka.app/). No root required.

Features a Quick Settings tile for one-tap cycling, automatic detection of supported refresh rates, and OEM-specific settings support.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android 11+](https://img.shields.io/badge/Android-11%2B-green.svg)](app/build.gradle.kts)
[![Latest release](https://img.shields.io/github/v/release/anlaki-py/akihz?label=release)](https://github.com/anlaki-py/akihz/releases/latest)

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

## Comparison

| | **akiHz** | **Display settings** | **ADB / shell** | **Tasker + Shizuku** |
|---|---|---|---|---|
| One-tap switch from Quick Settings | Yes | No | No | Possible (setup required) |
| Cycle through rates | Yes | No | Manual commands | Custom profiles |
| Detects supported refresh rates | Yes | Yes | No | Depends on setup |
| No root | Yes (Shizuku) | Yes | Yes (USB/wireless debugging) | Yes (Shizuku) |
| Open source | Yes | N/A | N/A | Tasker is paid |
| Multi-OEM key handling | Built-in | Built-in | Manual per device | Manual per device |
| Watchdog for system overrides | Yes | No | No | Possible |

akiHz is aimed at people who switch refresh rates often and want a fast, FOSS alternative without maintaining ADB scripts or Tasker tasks.

## Requirements

- Android 11+ (API 30)
- [Shizuku](https://shizuku.rikka.app/) installed and running

## Install

Download the latest APK from [Releases](https://github.com/anlaki-py/akihz/releases).

## Usage

1. Open Shizuku and start it (wireless debugging or ADB)
2. Open akiHz and grant Shizuku permission
3. Select a refresh rate from the buttons, or add the **akihz** tile to your Quick Settings panel and tap it to cycle

## FAQ

**What is akiHz?**  
A free, open-source Android app that switches your display refresh rate (e.g. 60Hz, 90Hz, 120Hz) from in-app buttons or a Quick Settings tile.

**Do I need root?**  
No. akiHz uses [Shizuku](https://shizuku.rikka.app/) to write system display settings without root.

**Why does Shizuku need to be running?**  
Android blocks normal apps from changing refresh-rate settings directly. Shizuku grants akiHz the privileged access needed to update the correct OEM-specific keys.

**Which refresh rates are shown?**  
Only rates your display actually supports. akiHz reads them from the system instead of showing a fixed list.

**How is this different from Settings → Display?**  
akiHz lets you switch rates in one tap from Quick Settings and cycle through them without opening the full settings menu. The optional watchdog (untested on all devices) can re-apply your choice if the system changes it.

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

## Build

```bash
./gradlew assembleRelease
```

## License

[MIT](LICENSE)
