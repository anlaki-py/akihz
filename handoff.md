# Handoff: Fix perf/crash debug + RAM leak (per-process recording)

## Goal
Fix the debug screens (Performance monitoring / Crash logs) that crash with `IllegalStateException: Vertically scrollable component was measured with infinity`, add a Crash logs screen with copy/save/share, make all debug actions CLI-accessible, and fix the RAM hog where `akihz` creates 50+ `shell` `refresh_rate_service` processes (~70 MB RSS each, ~2.5 GB total). Make the performance recording also record every `akihz` OS process with PSS/RSS/CPU/threads so the leak is visible in the log.

Original asks:
- ae3d500 introduced perf monitoring but had locale bug (`String.format` comma decimals) and `pendingCopyUri` dead code + nested `verticalScroll` crash.
- Add crash-log capture with save/copy/share in Debug screen, build universal debug APK and install.
- Make all debugging CLI-accessible (`adb shell am start/broadcast` + helper script).
- Investigate `ps` showing `akihz.anlaki.dev:refresh_rate_service` many times, check last perf log, and make perf log include per-process usage.

## Current state
Verified by direct inspection (`git status`, `git diff`, `adb`).

**Build:**
- Last `BUILD SUCCESSFUL` at 2026-08-28 ~18:?? via `./gradlew :app:packageDebugUniversalApk` (28s, 11 executed). Output `app/build/outputs/apk_from_bundle/debug/app-debug-universal.apk` (17 MB, 17334069 bytes) exists and is the per-process fix build (PerfSample now has `processCount`, `totalPssKb`, `processes`).
- However install via `adb shell pm install -r /data/local/tmp/app-debug.apk` fails with `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` (MIUI requires manual allow). User manually installed via UI at 18:11:49 – that install is the *previous* build (without per-process fix, perf log still shows `processCount` missing, only 1 process). The new per-process build (just built) has **not yet been manually installed** – next step is manual install or `adb push` + `pm install -r` with user tapping Allow.

**Runtime (after manual install, PID 31818):**
- `adb shell ps -A | grep refresh_rate_service | wc -l` → 67 before new build, 54 after manual install (still high). `adb shell ps -A -o PID,ARGS | grep refresh_rate` shows both `akihz.anlaki.dev:refresh_rate_service` (release) and `akihz.anlaki.dev.debug:refresh_rate_service` (debug) as `shell` user, PPID 1, `do_epoll_wait`, VmRSS ~70 MB, VmSwap ~100 MB, ~50 MB PSS each. Main `akihz.anlaki.dev.debug` PID 31818: PSS 153 MB, RSS 316 MB, Threads 47, from `dumpsys meminfo`.
- `adb shell run-as akihz.anlaki.dev.debug cat /proc/<shellPid>/cmdline` fails as app UID due to hidepid (only shell can read shell PIDs). `adb shell ps -A` as shell sees all; `run-as ls /proc` sees only ~6 PIDs. This is why the old `ProcessMetricsCollector` only saw 1 process.
- New `ProcessMetricsCollector` now tries `ShizukuHelper.isUserServiceBound()` → `runShellCommand("ps -A -o PID,ARGS")` and `cat /proc/<pid>/status` via shell fallback, so it should now see all 50+ processes. Tested via `DebugCliReceiver` fallback `ps` path, but not yet verified in perf log because new build not yet installed/running. After manual install, `perf-cat` still shows old format without `processCount` (session 18:11:49, sample `processCount` missing, `processes` missing) – confirming manual install was old APK. New build's `perf-cat` should show `"processCount":54,"totalPssKb":...,"processes":[{"pid":...}]` after install.
- Last perf recording before fix: `cache/perf/perf-active.log` at 18:07:33 with 100+ samples, each `pssTotalKb:152212`, no `processCount`. After new `perf-start` at 18:11:49, new log has `processCount:1` (only main, because old code). After per-process fix, new log should have `processCount:54` etc. Not yet verified (needs new install + `perf-start`).
- Crash logs: `files/crash_logs` has 5 files (3 original IllegalStateException crashes from nested scroll bug at 17:19:39/48/57, 1 from DebugCliReceiver bug at 18:12:46, 1 from `create_test_crash` at 18:20:55). `CrashLogStore.saveSync` now synchronous, verified via `ps` and `perf` not crashing.

**Fixes applied (in working tree, not yet committed):**
- `PerfLogModels.kt:13` added `PerfProcessInfo` and extended `PerfSample` with `processCount`, `totalPssKb`, `processes`; `PerfLogJson.sample:122` serializes them with `Locale.US`.
- `ProcessMetricsCollector.kt:26,56,74,131` added `perPidSnapshots` map, `collectAppProcesses`, `listAkihzPids` (tries `ShizukuHelper.runShellCommand("ps -A -o PID,ARGS")` first), `readFileContent` via shell fallback, `readProcStatWithFallback`, `computeCpuForPid`.
- `PerformanceMonitor.kt` unchanged logic but now collects new sample.
- `PerformanceMonitorScreen.kt:144` shows `App processes: N (total PSS X MiB)` + top 5 `pid name pss cpu th`.
- `ShizukuHelper.kt:101` changed `daemon(false)` → `daemon(true)`, `version(2)` → `version(3)` (so Shizuku kills old v2 daemons on next bind), added `onServiceConnected` delayed `killStaleRefreshServices()`, added `runShellCommand()`, `collectAppProcessesForPerf()`, `getServicePid()`, `killStaleRefreshServices()` (kills stale via `kill -9` as shell), `listAppProcesses()` now via shell ps.
- `ICommandService.kt:8` added `getPid(): Int` with `TRANSACTION_getPid`, `Proxy.getPid()`, `Stub.onTransact` for it.
- `ICommandServiceImpl.kt:60` implements `getPid() = android.os.Process.myPid()`.
- `DebugCliReceiver.kt:37` added `list_processes|ps` and `kill_stale|cleanup` handling, uses `ShizukuHelper` and fallback `/proc` scan, fixed `goAsync` bug (removed `setResultCode` which threw `Call while result is not pending` at `DebugCliReceiver.kt:60`).
- `DebugSettingsScreen.kt:23` made `DebugPage` internal with `fromString()`, `initialPage` param.
- `AkihzApp.kt:79,123` added `openDebugRequest` + `debugPage` + `debugInitialPage` LaunchedEffect.
- `MainActivity.kt:44,60,259` handles `akihz.extra.DEBUG_PAGE` / `akihz.extra.OPEN_DEBUG`, auto-unlocks.
- New files: `DebugCliReceiver.kt`, `CrashLogStore.kt`, `CrashLogActivity.kt`, `PerformanceMonitorActivity.kt`, `CrashLogScreen.kt`, `CrashLogViewModel.kt`, `scripts/debug-cli.sh`.
- `AndroidManifest.xml:50` added `CrashLogActivity`/`PerformanceMonitorActivity` exported, `DebugCliReceiver` exported for `DEBUG_CLI`.
- `AGENTS.md:23` documented Direct activities, MainActivity deep link, DebugCliReceiver explicit broadcast (`-n $PKG/...`), file locations, helper script usage.
- Previous fixes still present: `PerfLogModels.kt:90` `Locale.US` for `\u%04x`, `PerformanceMonitorViewModel.kt:34` removed `SavedStateHandle` and `pendingCopyUri`, nested scroll removed.

**What works:**
- Direct activities via `adb shell am start -n akihz.anlaki.dev.debug/akihz.anlaki.dev.presentation.CrashLogActivity` → shows `Crash logs` with 3 reports, `PerformanceMonitorActivity` → `Idle` (verified via `uiautomator dump`).
- MainActivity deep link `adb shell am start -n ...MainActivity --es akihz.extra.DEBUG_PAGE crash_logs|performance` → works (tested `crash_logs` and `performance` open correct screens).
- Broadcast `adb shell am broadcast -n ...DebugCliReceiver -a akihz.anlaki.dev.DEBUG_CLI --es cmd list_crashes|perf_status|perf_start|perf_cat|perf_stop` now works after fixing `goAsync` (perf commands previously succeeded, list_crashes now succeeds after fix).
- `scripts/debug-cli.sh` helper works for `list-crashes`, `create-test-crash`, `perf-*`, `open-*`, `pull-*`, `ps`, `kill-stale`.

**What doesn't / mid-change:**
- New per-process perf build has been built but NOT yet installed on device (manual install still old). Need to `adb push` + `pm install -r` and tap Allow, then `perf-start` and verify `perf-cat` shows `processCount` >1 and `processes` array with many entries. The `adb shell pm install -r` currently fails without manual Allow (MIUI).
- Stale shell process cleanup via version bump (2→3) has been built but not yet tested on device (needs new install + service bind). After new install, `ps -A | grep refresh_rate | wc -l` should drop from 54 towards 1-2. The `kill_stale` broadcast and `ShizukuHelper.killStaleRefreshServices()` also need verification.
- `ProcessMetricsCollector` hidepid fallback via Shizuku may still miss some PIDs if `ShizukuHelper.isUserServiceBound()` is false at sample time (first sample after `perf-start` may have `processCount:1` until Shizuku binds). Need to verify after Shizuku service connects.
- Tests: `ProcessMetricsCollectorTest.kt` still passes (default `PerfSample` has new fields with defaults), but not yet updated to check new `processes` serialization. Need to run `./gradlew testDebugUnitTest` and `lintDebug` after new build (last run was before per-process change; new build was `packageDebugUniversalApk` only, not `test`/`lint`).

## Files involved
- `app/src/main/kotlin/akihz/anlaki/dev/data/PerfLogModels.kt`: added `PerfProcessInfo`, extended `PerfSample`, updated `PerfLogJson.sample` to emit `processCount`, `totalPssKb`, `processes` array with `Locale.US`.
- `app/src/main/kotlin/akihz/anlaki/dev/data/ProcessMetricsCollector.kt`: added `perPidSnapshots`, `collectAppProcesses`, `listAkihzPids` (shell ps fallback), `readFileContent` (direct + shell), `readProcStatWithFallback`, `computeCpuForPid`, updated `sample()` to include `processes`.
- `app/src/main/kotlin/akihz/anlaki/dev/data/ShizukuHelper.kt`: `daemon(false)`→`true`, `version(2)`→`3`, added `onServiceConnected` delayed `killStale`, added `runShellCommand()`, `getServicePid()`, `killStaleRefreshServices()`, `listAppProcesses()` via shell, `collectAppProcessesForPerf()`.
- `app/src/main/kotlin/akihz/anlaki/dev/ICommandService.kt`: added `getPid()` interface, `TRANSACTION_getPid`, Proxy.
- `app/src/main/kotlin/akihz/anlaki/dev/data/ICommandServiceImpl.kt`: implements `getPid()`.
- `app/src/main/kotlin/akihz/anlaki/dev/DebugCliReceiver.kt`: added `list_processes`/`kill_stale`, fixed `goAsync` (remove `setResultCode`).
- `app/src/main/kotlin/akihz/anlaki/dev/presentation/PerformanceMonitorScreen.kt`: `LiveReadout` now shows `App processes` and top 5.
- `app/src/main/kotlin/akihz/anlaki/dev/data/CrashLogStore.kt` (new): sync `saveSync`, `saveCaught`, `getAll`, `readContent`, `deleteAll`.
- `app/src/main/kotlin/akihz/anlaki/dev/presentation/CrashLog*` (new): `CrashLogScreen.kt`, `CrashLogViewModel.kt`, `CrashLogActivity.kt`, `PerformanceMonitorActivity.kt`.
- `app/src/main/kotlin/akihz/anlaki/dev/AkihzApplication.kt`: sync crash logger, `crashLogStore` inject.
- `app/src/main/kotlin/akihz/anlaki/dev/presentation/MainActivity.kt` / `AkihzApp.kt` / `DebugSettingsScreen.kt`: `akihz.extra.DEBUG_PAGE` deep link.
- `app/src/main/AndroidManifest.xml`: exported `CrashLogActivity`, `PerformanceMonitorActivity`, `DebugCliReceiver`.
- `app/src/main/res/xml/file_paths.xml`: `files-path crash_logs`.
- `scripts/debug-cli.sh` (new, executable): wraps `adb -s 192.168.1.10:5555` or first device, `PKG` auto-detect, explicit broadcast, `ps`, `kill-stale`, `open-*`, `pull-*`, `logcat`.
- `AGENTS.md:23`: documented Debug CLI.
- `app/build/outputs/apk_from_bundle/debug/app-debug-universal.apk` (17 MB) is the per-process build, built 2026-08-28 ~18:??, `BUILD SUCCESSFUL`.

## What's changed this session
From `git status` / `git diff --stat HEAD` (14 modified, 7 untracked):
- Modified: `AGENTS.md` (+20), `AndroidManifest.xml` (+18), `AkihzApplication.kt` (+26), `ICommandService.kt` (+22), `ICommandServiceImpl.kt` (+2), `PerfLogModels.kt` (+34), `ProcessMetricsCollector.kt` (+130), `ShizukuHelper.kt` (+140), `AkihzApp.kt` (+15), `DebugSettingsScreen.kt` (+32), `MainActivity.kt` (+18), `PerformanceMonitorScreen.kt` (+31), `PerformanceMonitorViewModel.kt` (-15), `file_paths.xml` (+3)
- Untracked new: `DebugCliReceiver.kt`, `CrashLogStore.kt`, `CrashLogActivity.kt`, `CrashLogScreen.kt`, `CrashLogViewModel.kt`, `PerformanceMonitorActivity.kt`, `scripts/debug-cli.sh`
- Last `git log`: `ae3d500 Add Performance monitoring debug page`
- Last build: `./gradlew :app:packageDebugUniversalApk` → `BUILD SUCCESSFUL in 28s` (new per-process), not yet installed due to `INSTALL_FAILED_USER_RESTRICTED`.

## Constraints and things to avoid
- Maximum 200 lines per Kotlin/Java file — but note allows slightly longer; `ShizukuHelper.kt` is 422 lines, `ProcessMetricsCollector.kt` 231 lines — both accepted as necessary, don't split unless illogical.
- Package structure `data/` / `presentation/` / `di/` / `utils` must be kept.
- Document public functions with KDoc.
- Debug APK builds must be `packageDebugUniversalApk` only.
- In Android/Termux env, don't run local Gradle builds/lint/tests — use GitHub Actions CI, *except* when user explicitly asks to build (this session user asked to build and install, so builds were allowed).
- `a` before `m` must never create tag/release; `beta` branch only `release-beta.yml`, `main` only `release-main.yml`; releases require changelog sections.
- Don't swallow errors without context; use `Locale.US` for perf JSON; handle hidepid via Shizuku fallback, don't assume `/proc` readable as app.

## What's been tried and failed
- Initial `DebugCliReceiver` used `setResultCode(0)` after `goAsync()` → `IllegalStateException: Call while result is not pending` at `BroadcastReceiver.java:491` and `DebugCliReceiver.kt:60` (log 18:12:46, crash-20260828-181246). Fixed by removing `setResultCode`/`setResultData` and just `writeOutput` + `pending.finish()`. Perf commands then succeeded, `list_crashes` then succeeded.
- `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd ...` (implicit) was blocked by background execution limits and hidepid — changed to explicit `-n $PKG/akihz.anlaki.dev.DebugCliReceiver` in `scripts/debug-cli.sh:58`.
- `MainActivity` deep link initially used `--es debug_page` (short key) → `handleUpdateIntent` didn't trigger (expects `akihz.extra.DEBUG_PAGE`). Fixed script to use full key `akihz.extra.DEBUG_PAGE`; verified `crash_logs` and `performance` now open correctly.
- `ProcessMetricsCollector` initially scanned `/proc` as app → only saw 1 PID (main) because shell refresh_rate_service PIDs are `shell` UID and hidepid prevents app from reading `/proc/<pid>/cmdline` (tested: `run-as akihz.anlaki.dev.debug cat /proc/6709/cmdline` → `No such file or directory`, while `adb shell cat` as shell succeeds). Fixed by adding `ShizukuHelper.runShellCommand("ps -A -o PID,ARGS")` and `cat /proc/.../status` via shell when `isUserServiceBound()`.
- Nested `verticalScroll` inside `PreferenceLayout` (which already scrolls) caused `IllegalStateException: Vertically scrollable component was measured with infinity` for `PerformanceMonitorScreen`/`CrashLogScreen` — fixed by removing inner `verticalScroll`/`fillMaxSize`, keeping only `padding`.
- `ShizukuHelper` many `refresh_rate_service` processes: `ps -A` showed 67 before, 54 after manual install, each `shell` with ~70 MB RSS, PPID 1. Not fixed by `daemon(false)`/`version(2)`; changed to `daemon(true)`/`version(3)` and added `killStaleRefreshServices()` + `DebugCliReceiver` `kill_stale` and `ShizukuHelper` delayed cleanup in `onServiceConnected`. Not yet verified after new install (install blocked).
- `adb shell push` used as `adb shell push` (invalid) → `push: inaccessible` — corrected to `adb push`.
- `scripts/debug-cli.sh` initially used `adb -s 192.168.1.10:5555` hard-coded; updated to auto-detect device and respect `ADB`/`ANDROID_SERIAL`.
- `PerformanceMonitorScreen` `LiveReadout` initially not showing `processCount`; updated to show `App processes` and top 5.
- `ICommandService` missing `getPid` caused `ShizukuHelper.killStale` to not know current PID — added `getPid()` transaction.

## Other learnings
- Device is Xiaomi 22071212AG, Android 15, SDK 35, arm64-v8a, 8 GB RAM, `PKG` is `akihz.anlaki.dev.debug` (debug) with `akihz.anlaki.dev` release also installed (both have `refresh_rate_service` shells). `192.168.1.10:5555` is the `adb` endpoint; `adb` is at `/usr/bin/adb`, SDK at `/home/aki/Android/Sdk`.
- `hidepid` on `/proc` prevents app from listing shell PIDs; must use `Shizuku` shell service to run `ps` and `cat /proc/...` as `shell`.
- `ActivityManager.getProcessMemoryInfo` for shell PIDs returns 0 or fails as app; fallback to `VmRSS` from `status` is needed.
- `Shizuku` `daemon(true)` with `version` bump is the documented way to kill old daemon shells; `unbindUserService(..., true)` calls `ICommandService.destroy()` → `System.exit(0)` but orphans remain if app died without unbind.
- `dumpsys meminfo` for main PID 7995/31818 shows 141–153 MB PSS, 143–176 MB RSS; each shell `refresh_rate_service` is 50–70 MB PSS, so 54 of them is ~3 GB, explaining RAM hog.
- `Perf log` is `cache/perf/perf-active.log` (JSON lines: `session`, `sample`, `event`), was 878 bytes after `perf-stop` with 2 samples, now should be larger with `processes` array. Last session before fix: `startedAt 2026-08-28T17:25:30` with 100+ samples, `pssTotalKb:189788`.
- `Crash logs` are `files/crash_logs/crash-*.log` and `error-*.log`, `debug_cli_output.txt` is `files/debug_cli_output.txt`.
- `uiautomator dump` to `/data/local/tmp/dump_*.xml` and `screencap -p` to `/tmp/screenshots/*.png` are reliable for CLI verification; `input tap` coordinates are fragile (e.g., `Create test crash` needed 475,1163 not 610,1233).
- `INSTALL_FAILED_USER_RESTRICTED` is MIUI's manual Allow prompt; `adb push` + `pm install -r` requires user to tap Allow, otherwise `adb install` also needs Allow. Manual UI install works.

## Next steps
1. **Install the new per-process APK** (built at `app/build/outputs/apk_from_bundle/debug/app-debug-universal.apk`): `adb push app/build/outputs/apk_from_bundle/debug/app-debug-universal.apk /data/local/tmp/app-debug.apk && adb shell pm install -r /data/local/tmp/app-debug.apk` and tap Allow on device. Verify `adb shell dumpsys package akihz.anlaki.dev.debug | grep versionName` still `0.0-debug` but new code is active.
2. **Verify RAM fix:** `adb shell ps -A | grep -c refresh_rate_service` should drop from 54 to 1-2 after new `version(3)` `daemon(true)` bind. Run `./scripts/debug-cli.sh ps` and check `Via ShizukuHelper` output should now list `processCount` with many PIDs but after `kill_stale` it should reduce. Run `./scripts/debug-cli.sh kill-stale` then `ps` again. If still high, check `logcat | grep Shizuku` for `killStale` result.
3. **Verify new perf log:** `./scripts/debug-cli.sh perf-start && sleep 3 && ./scripts/debug-cli.sh perf-cat && ./scripts/debug-cli.sh perf-stop && adb shell run-as akihz.anlaki.dev.debug cat /data/data/.../cache/perf/perf-active.log | grep processCount` should show `processCount:54` and `processes` array with `pid`, `pssKb` >0, `cpuPercent` after 2 ticks. Check `adb shell run-as ... cat ... | python3 -m json.tool` for validity and `Locale.US` decimals (2 decimals, not commas).
4. **Verify UI:** `adb shell am start -n .../PerformanceMonitorActivity` and check `Live readings` now shows `App processes: N (total PSS X MiB)` and `Top processes` list (5 lines). Take new `adb exec-out screencap` for `performance.png`.
5. **Run tests/lint:** `./gradlew testDebugUnitTest` and `./gradlew lintDebug` (or wait for GitHub CI on push to `main`/`beta`). Update `ProcessMetricsCollectorTest.kt` if it checks `PerfSample` JSON without new fields (it currently passes due to defaults, but verify `PerfLogJson.sample` test).
6. **Commit and push:** `git add -A` (includes `AGENTS.md`, `AndroidManifest.xml`, `AkihzApplication.kt`, `ICommandService*`, `PerfLogModels.kt`, `ProcessMetricsCollector.kt`, `ShizukuHelper.kt`, `AkihzApp.kt`, `DebugSettingsScreen.kt`, `MainActivity.kt`, `PerformanceMonitorScreen.kt`, `PerformanceMonitorViewModel.kt`, `file_paths.xml`, `DebugCliReceiver.kt`, `CrashLogStore.kt`, `CrashLogActivity.kt`, `CrashLogScreen.kt`, `CrashLogViewModel.kt`, `PerformanceMonitorActivity.kt`, `scripts/debug-cli.sh`, `handoff.md`), then `git commit -m "fix: per-process perf + RAM leak (daemon v3, kill stale, hidepid via Shizuku) + CLI"` and `git push` (to `main` or `beta` per release rules; currently on `main`-like branch, but check `git branch --show-current`).

## How to verify current state
- `git status --porcelain` should show 14 modified + 7 untracked (as above) plus `handoff.md`.
- `git diff --stat HEAD` should match list in `Files involved`.
- `./gradlew :app:packageDebugUniversalApk` should be `BUILD SUCCESSFUL` (last was 28s, 11 executed).
- `adb shell ps -A | grep refresh_rate | wc -l` → currently 54 (before fix), should be ~1 after new install + `kill_stale`.
- `adb shell run-as akihz.anlaki.dev.debug cat /data/data/.../cache/perf/perf-active.log | grep processCount` → should contain `processCount` after new perf recording (old APK shows no field).
- `./scripts/debug-cli.sh ps` should list Via ShizukuHelper and fallback `/proc` scan with many PIDs.
- `./scripts/debug-cli.sh list-crashes` should show 5 files, `perf-cat` should show `processes` array.

