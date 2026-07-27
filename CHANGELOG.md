# Changelog

All notable changes to akiHz are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions before `0.0.31` are reconstructed from the Git history.

## [Unreleased]

### Added

- Added adaptive, round, legacy, and monochrome launcher icon resources.
- Added a 512×512 store listing icon.
- Stable releases from `main` and prereleases from `beta`.
- Standalone `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` APKs.
- A universal APK, SHA-256 checksums, and machine-readable release metadata.
- Regression tests for OEM override resolution and settings values.

### Changed

- Made refresh-rate commands report partial failures instead of false success.
- Made shell execution timeout-safe and resistant to output-buffer deadlocks.
- Unified selected-rate persistence between the app, tile, and watchdog.
- Made watchdog startup idempotent and prevented overlapping checks.
- Removed the permanent keep-alive foreground service.

### Fixed

- Made app and Quick Settings selections respond visually without waiting for
  OEM settings commands to finish.
- Fixed manual OEM overrides silently falling back to auto-detection.
- Fixed mode and boolean settings receiving literal refresh-rate values.
- Fixed reset leaving the watchdog able to restore a stale rate.
- Fixed Shizuku reconnection listener registration.

## [0.0.31] - 2026-06-29

### Changed

- Enlarged refresh-rate button text and height.

## [0.0.30] - 2026-06-29

### Fixed

- Fixed missing padding imports and replaced unavailable toggle components.
- Restored Hilt compatibility with the Android Gradle Plugin.

### Changed

- Introduced Hilt dependency injection, ViewModel and StateFlow UI state,
  repository boundaries, a domain layer, and Timber logging.

## [0.0.29] - 2026-06-05

### Added

- Added advanced settings actions and expanded project documentation.

## [0.0.28] - 2026-05-09

### Changed

- Marked the watchdog as untested across all supported devices.

## [0.0.27] - 2026-05-09

### Fixed

- Added bottom padding so settings are not obscured by navigation.

## [0.0.26] - 2026-05-09

### Removed

- Removed the current refresh-rate readout from the home screen.

## [0.0.25] - 2026-05-09

### Removed

- Removed lock mode.

## [0.0.24] - 2026-05-09

### Removed

- Removed battery-saver override handling.

## [0.0.23] - 2026-05-09

### Changed

- Simplified the preference UI styling.

## [0.0.22] - 2026-05-09

### Fixed

- Fixed a missing settings icon import.

## [0.0.21] - 2026-05-09

### Removed

- Removed App Monitor.

## [0.0.20] - 2026-05-09

### Fixed

- Prevented the watchdog from overriding profiles after notification overlays.

## [0.0.19] - 2026-05-09

### Fixed

- Prevented notification-bar activity from causing false app switches.

## [0.0.18] - 2026-05-09

### Fixed

- Coordinated watchdog and App Monitor overrides.

## [0.0.17] - 2026-05-09

### Fixed

- Restored the global refresh rate after leaving a profiled app.

## [0.0.16] - 2026-05-09

### Changed

- Disabled the package-query permission lint check.
- Completed a broad app-listing, architecture, and UX overhaul.

## [0.0.15] - 2026-05-09

### Fixed

- Fixed back-button navigation and polished the interface.

## [0.0.14] - 2026-05-09

### Fixed

- Corrected user-app filtering and moved loading off the UI thread.

## [0.0.13] - 2026-05-09

### Fixed

- Fixed a missing text-field import.

## [0.0.12] - 2026-05-09

### Fixed

- Fixed App Monitor accessibility detection and layout.

## [0.0.11] - 2026-05-09

### Added

- Added watchdog, App Monitor, and expanded OEM support.

### Changed

- Disabled the protected accessibility permission lint check.

## [0.0.10] - 2026-05-07

### Added

- Added refresh-rate-specific Quick Settings tile icons.

## [0.0.9] - 2026-05-07

### Added

- Added a foreground keep-alive service.

## [0.0.8] - 2026-05-07

### Changed

- Adopted Material navigation icons.

## [0.0.7] - 2026-05-07

### Changed

- Removed tests from the CI pipeline.

## [0.0.6] - 2026-05-07

### Added

- Added bottom navigation and the settings screen.
- Added version and update information.

## [0.0.5] - 2026-04-24

### Added

- Added realme support using the system peak-refresh-rate setting.
- Added initial project documentation.

## [0.0.4] - 2026-04-24

### Fixed

- Prevented duplicate activities when long-pressing the Quick Settings tile.

## [0.0.3] - 2026-04-24

### Fixed

- Made a Quick Settings tile long-press open akiHz instead of system settings.

## [0.0.2] - 2026-04-24

### Fixed

- Removed a blocking delay from refresh-rate switching.

## [0.0.1] - 2026-04-24

### Changed

- Updated CI to use `main` as the default branch.

## [0.0.0-beta.main.0] - 2026-04-24

### Added

- Added the initial Android application, Shizuku integration, CI, and tests.

[Unreleased]: https://github.com/anlaki-py/akihz/compare/v0.0.31...HEAD
[0.0.31]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.31
[0.0.30]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.30
[0.0.29]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.29
[0.0.28]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.28
[0.0.27]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.27
[0.0.26]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.26
[0.0.25]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.25
[0.0.24]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.24
[0.0.23]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.23
[0.0.22]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.22
[0.0.21]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.21
[0.0.20]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.20
[0.0.19]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.19
[0.0.18]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.18
[0.0.17]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.17
[0.0.16]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.16
[0.0.15]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.15
[0.0.14]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.14
[0.0.13]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.13
[0.0.12]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.12
[0.0.11]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.11
[0.0.10]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.10
[0.0.9]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.9
[0.0.8]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.8
[0.0.7]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.7
[0.0.6]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.6
[0.0.5]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.5
[0.0.4]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.4
[0.0.3]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.3
[0.0.2]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.2
[0.0.1]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.1
[0.0.0-beta.main.0]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.0-beta.main.0
