# Changelog

All notable changes to akiHz are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions before `0.0.31` are reconstructed from the Git history.

## [Unreleased]

### Added

- Added Performance monitoring debug page with live CPU/PSS/threads/heap/refresh-rate readout, JSON-line recording to `cache/perf/perf-active.log` (`PerfLogJson` with `Locale.US`), session header, and export/share via FileProvider.
- Added Crash logs screen under Debug options (unlocked by tapping the version row six times) with stored reports in `filesDir/crash_logs`, detail view, copy, save, and share actions backed by synchronous `CrashLogStore.saveSync()`.
- Added ADB debug CLI via `akihz.anlaki.dev.DebugCliReceiver` (`android:exported="true"`, `ACTION=akihz.anlaki.dev.DEBUG_CLI`) and helper `scripts/debug-cli.sh` (`list_crashes|cat_crash|create_test_crash|clear_crashes|trigger_crash|perf_status|perf_start|perf_stop|perf_cat|ps|kill_stale`, `open-crash|open-performance`, `pull-*`, `logcat`), with explicit broadcast `adb shell am broadcast -n $PKG/akihz.anlaki.dev.DebugCliReceiver -a akihz.anlaki.dev.DEBUG_CLI --es cmd <cmd>` and output to `filesDir/debug_cli_output.txt`.
- Added direct debug activities `CrashLogActivity` and `PerformanceMonitorActivity` plus `MainActivity` deep link `adb shell am start -n $PKG/akihz.anlaki.dev.presentation.MainActivity --es akihz.extra.DEBUG_PAGE crash_logs|performance` (auto-unlocks `debugOptionsUnlocked` via `openDebugRequest`).
- Extended performance samples with per-process metrics (`processCount`, `totalPssKb`, `processes[]` with `pid`/`name`/`pssKb`/`rssKb`/`cpuPercent`/`threads`) and top-5 display in `PerformanceMonitorScreen`, collected via `ShizukuHelper.runShellCommand("ps -A -o PID,ARGS")` and `cat /proc/<pid>/status` fallback to bypass hidepid, plus `ICommandService.getPid()` (`TRANSACTION_getPid`) for stale-process detection.
- Added developer website link to `README.md`.

### Fixed

- Fixed RAM hog where `ShizukuHelper` with `daemon(false)` and `version(2)` leaked 50+ `shell` `refresh_rate_service` processes (~70 MB RSS, ~50 MB PSS each) — changed to `daemon(true)` `version(3)`, added delayed `killStaleRefreshServices()` in `onServiceConnected` and `kill_stale` CLI that keeps the current service PID alive.
- Fixed `IllegalStateException: Vertically scrollable component was measured with infinity` from nested `verticalScroll` inside already-scrollable `PreferenceLayout` in Performance and Crash screens.
- Fixed `String.format` locale bug (`PerfLogModels.kt:103` `Locale.US` for `\u%04x`) that produced comma decimals in perf JSON, and removed dead `pendingCopyUri` / `SavedStateHandle` state from `PerformanceMonitorViewModel`.
- Fixed hidepid visibility: `ProcessMetricsCollector` now prefers `ps` via Shizuku shell and falls back to `/proc` + `cat` via shell for `cmdline`/`status`/`stat`, with per-PID CPU delta map and RSS/PSS fallback via `ActivityManager` or `VmRSS`/`RssAnon`.

### Changed

- Split the former `Build & Release` workflow into channel-locked manual-only workflows `release-beta.yml` (publishes `-beta.N` prerelease from `beta`) and `release-main.yml` (stable from `main`), each with its own lint and unit-test gates and branch guards (`GITHUB_REF_NAME` check).
- Made `Build & Release` manual-only on both branches before the split and documented the universal-only debug APK task and debug CLI in `AGENTS.md`.

## [0.0.42] - 2026-08-14

### Fixed

- Kept the Home screen's selected refresh rate synchronized with successful
  changes made from the Quick Settings tile.

### Added

- Added per-rate Home screen toggles for choosing which refresh rates the
  Quick Settings tile cycles through.

## [0.0.41] - 2026-08-12

### Added

- Added a friendly first-run project notice that users acknowledge before
  akiHz starts Shizuku setup or its background service.
- Added the developer name and profile to machine-readable release metadata.

## [0.0.40] - 2026-08-12

### Changed

- Made GitHub releases use the matching section of this changelog as their
  release notes instead of GitHub-generated commit summaries.
- Made release publication stop when the expected changelog section is missing
  or empty, preventing releases with incorrect notes.
- Added the matching changelog notes to machine-readable release metadata.
- Split lint and unit tests into a dedicated CI workflow, while making signed
  GitHub releases manual-only so ordinary pushes do not publish new versions.

## [0.0.39] - 2026-08-12

### Added

- Added automatic GitHub update checks using battery-conscious WorkManager
  scheduling, with daily, three-day, weekly, and disabled frequency options.
- Added deduplicated update-available notifications that open the existing secure
  in-app download flow.

The changelog entry for version `0.0.38` was intentionally skipped.

## [0.0.37] - 2026-08-12

### Added

- Added upstream Fastlane store metadata with the app icon, descriptions, and
  light and dark screenshots for app catalogs and alternative stores.
- Added a repository social-preview image and moved download, requirements,
  and supported Android version details near the top of the README.
- Added canonical project and release links, release date, Shizuku dependency,
  direct APK URLs, and the APK signing-certificate fingerprint to generated
  release metadata.

### Changed

- Documented the universal-only Gradle task used for debug APK builds.

### Fixed

- Made release signing-certificate extraction support both current `V2 Signer`
  and older `Signer #1` output formats from Android's `apksigner` tool.

## [0.0.36] - 2026-08-11

### Added

- Added persistent Debug options, unlocked by tapping the version row six
  times, with an organized category page for Home screen tuning.
- Added controls for refresh-rate text size, button height, width, spacing,
  and resting, selected, and pressed Material 3 Expressive corner shapes.
- Added a reusable page surface and directional slide-and-fade transitions for
  settings detail pages, debug categories, and floating navigation visibility.
- Added a unit-test job to the release workflow and regression coverage for
  update verification, custom-profile drafts, candidate detection, and
  settings snapshot retention.

### Changed

- Restored large, centered Home refresh-rate buttons with 44sp labels and a
  160dp default height while keeping oversized layouts scrollable.
- Made custom refresh-rate profile edits save asynchronously and flush before
  tests, activation changes, or navigation.
- Compacted custom-key snapshots into one baseline plus bounded per-rate
  differences, and improved candidate inference from those snapshots.
- Moved update status checks and APK digest verification off the main thread
  and cached repeated verification results for the same download.

### Fixed

- Kept Debug options unlocked across app launches and made its controls
  readable in pitch-black AMOLED mode.
- Preserved the latest custom-profile draft when leaving the editor and
  prevented edited profiles from retaining stale tested or enabled state.
- Prevented redundant APK verification work during update installation.

## [0.0.35] - 2026-08-11

### Added

- Restored the foreground keep-alive service so Quick Settings refresh-rate
  controls remain ready while the app is closed.
- Added an Advanced settings shortcut that requests an Android battery-
  optimization exemption, with a fallback to the system optimization list.

### Fixed

- Made Pitch-black AMOLED mode use true `#000000` for every Material surface
  role instead of near-black container colors.

## [0.0.34] - 2026-07-29

### Added

- Added custom refresh-rate key profiles for devices that are not covered by
  the built-in OEM strategies.
- Added an in-app update system with stable and prerelease channels,
  ABI-specific APK selection, Android Download Manager integration, and
  install-ready notifications.
- Added persistent update recovery across activity and process recreation.
- Added SHA-256 verification before handing downloaded APKs to Android's
  package installer.
- Added unit tests for update availability, release asset selection, digest
  validation, and shared Shizuku connection ownership.

### Changed

- Replaced hardcoded refresh-rate tile PNGs with dynamically generated numeric
  icons that support any detected refresh rate.
- Made Shizuku user-service connections shared and independently owned by the
  app activity and Quick Settings tile.
- Made Shizuku component resolution use the runtime application ID so debug
  and release variants bind correctly.
- Simplified the About section and replaced browser-based update links with
  the in-app updater.

### Removed

- Removed watchdog mode, its foreground service, boot receiver, battery
  optimization controls, permissions, settings, and documentation.
- Removed the website entry and obsolete refresh-rate PNG assets.

### Fixed

- Preserved update downloads and installation prompts when akiHz is not open.
- Prevented prerelease builds from offering destructive downgrades when the
  latest stable release has a lower version code.
- Prevented one app component from disconnecting a Shizuku service still used
  by another component.

## [0.0.33] - 2026-07-28

### Added

- Added System, Light, and Dark appearance modes with an optional AMOLED
  theme.
- Added swipe navigation between Home and Settings.
- Added optional progressive edge blur behind system bars and floating
  navigation.
- Added an icon splash screen with light and dark variants.

### Changed

- Standardized the app on a modern Material 3 interface with redesigned
  settings controls and a floating bottom navigation bar.
- Restored large refresh-rate controls on Home with animated press and
  selection shape morphing.
- Updated app branding and documentation to consistently use the `akiHz`
  name.
- Clarified that the project is provided as-is without maintenance, support,
  or update commitments.

### Fixed

- Fixed content insets and edge transitions around the status and navigation
  bars.
- Fixed regular dark mode incorrectly using pitch-black surfaces when AMOLED
  mode was disabled.
- Replaced the temporary disconnected-state flash during Shizuku startup with
  a loading indicator.

## [0.0.32] - 2026-07-27

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

[Unreleased]: https://github.com/anlaki-py/akihz/compare/v0.0.33...HEAD
[0.0.33]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.33
[0.0.32]: https://github.com/anlaki-py/akihz/releases/tag/v0.0.32
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
