#!/usr/bin/env bash
# Debug CLI helper for akihz – lets you drive crash/perf debugging purely from adb.
# Usage:
#   ./scripts/debug-cli.sh <cmd> [args]
# Examples:
#   ./scripts/debug-cli.sh list-crashes
#   ./scripts/debug-cli.sh cat-crash error-20260828-174440-690-test_crash.log
#   ./scripts/debug-cli.sh create-test-crash
#   ./scripts/debug-cli.sh clear-crashes
#   ./scripts/debug-cli.sh perf-status
#   ./scripts/debug-cli.sh perf-start
#   ./scripts/debug-cli.sh perf-stop
#   ./scripts/debug-cli.sh perf-cat              # now includes per-process list
#   ./scripts/debug-cli.sh ps                    # list akihz processes + RAM
#   ./scripts/debug-cli.sh kill-stale            # kill stale refresh_rate_service shells
#   ./scripts/debug-cli.sh open-performance   # opens PerformanceMonitorActivity
#   ./scripts/debug-cli.sh open-crash         # opens CrashLogActivity
#   ./scripts/debug-cli.sh open-debug performance  # opens MainActivity -> Debug -> Performance
#   ./scripts/debug-cli.sh open-debug crash        # opens MainActivity -> Debug -> Crash logs
#   ./scripts/debug-cli.sh pull-crash <file> [dest]
#   ./scripts/debug-cli.sh pull-all-crashes [dest_dir]
#   ./scripts/debug-cli.sh pull-perf [dest]
#   ./scripts/debug-cli.sh logcat               # tail logcat for akihz
#   ./scripts/debug-cli.sh logcat-crash         # filter for AndroidRuntime
#   ./scripts/debug-cli.sh help

set -euo pipefail

ADB="${ADB:-adb}"
# Pick device: use first device if none specified, but respect ANDROID_SERIAL / -s
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB_ARGS=(-s "$ANDROID_SERIAL")
elif adb devices 2>/dev/null | grep -q "192.168.1.10:5555.*device"; then
  ADB_ARGS=(-s 192.168.1.10:5555)
else
  # fallback to first device
  DEV=$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1; exit}')
  if [[ -n "$DEV" ]]; then ADB_ARGS=(-s "$DEV"); else ADB_ARGS=(); fi
fi

PKG_DEBUG="akihz.anlaki.dev.debug"
PKG_RELEASE="akihz.anlaki.dev"

detect_pkg() {
  if $ADB "${ADB_ARGS[@]}" shell pm list packages 2>/dev/null | grep -q "$PKG_DEBUG"; then
    echo "$PKG_DEBUG"
  elif $ADB "${ADB_ARGS[@]}" shell pm list packages 2>/dev/null | grep -q "$PKG_RELEASE"; then
    echo "$PKG_RELEASE"
  else
    echo "$PKG_DEBUG"
  fi
}

PKG=$(detect_pkg)
OUT_FILE="debug_cli_output.txt"

run_broadcast() {
  local cmd="$1"
  local arg="${2:-}"
  echo "→ $ADB ${ADB_ARGS[*]} shell am broadcast -n $PKG/akihz.anlaki.dev.DebugCliReceiver -a akihz.anlaki.dev.DEBUG_CLI --es cmd $cmd ${arg:+--es arg \"$arg\"}"
  if [[ -n "$arg" ]]; then
    $ADB "${ADB_ARGS[@]}" shell am broadcast -n "$PKG/akihz.anlaki.dev.DebugCliReceiver" -a akihz.anlaki.dev.DEBUG_CLI --es cmd "$cmd" --es arg "$arg" 2>&1
  else
    $ADB "${ADB_ARGS[@]}" shell am broadcast -n "$PKG/akihz.anlaki.dev.DebugCliReceiver" -a akihz.anlaki.dev.DEBUG_CLI --es cmd "$cmd" 2>&1
  fi
  sleep 0.5
  echo "--- output file ($PKG/files/$OUT_FILE) ---"
  $ADB "${ADB_ARGS[@]}" shell run-as "$PKG" cat "/data/data/$PKG/files/$OUT_FILE" 2>&1 || echo "(no output file yet)"
}

case "${1:-help}" in
  list-crashes|list)
    run_broadcast list_crashes
    echo "--- direct ls ---"
    $ADB "${ADB_ARGS[@]}" shell run-as "$PKG" ls -lh "/data/data/$PKG/files/crash_logs/" 2>&1 || echo "(no crash_logs dir)"
    ;;
  cat-crash|cat)
    if [[ -z "${2:-}" ]]; then echo "Usage: $0 cat-crash <filename>"; exit 1; fi
    run_broadcast cat_crash "$2"
    ;;
  create-test-crash|test-crash)
    run_broadcast create_test_crash
    ;;
  clear-crashes|clear)
    run_broadcast clear_crashes
    ;;
  perf-status|perf)
    run_broadcast perf_status
    ;;
  perf-start)
    run_broadcast perf_start
    ;;
  perf-stop)
    run_broadcast perf_stop
    ;;
  perf-cat)
    run_broadcast perf_cat
    ;;
  ps|list-processes|list-ps)
    run_broadcast list_processes
    echo "--- direct ps via run-as cat /proc scan ---"
    $ADB "${ADB_ARGS[@]}" shell ps -A -o PID,USER,PCPU,RSS,ARGS 2>&1 | grep -i "akihz" | head -n 60
    echo "--- dumpsys meminfo for main pid ---"
    PID=$($ADB "${ADB_ARGS[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r' | head -n 1)
    if [[ -n "$PID" ]]; then $ADB "${ADB_ARGS[@]}" shell dumpsys meminfo "$PKG" 2>&1 | head -n 40; fi
    ;;
  kill-stale|cleanup)
    run_broadcast kill_stale
    echo "--- ps after cleanup ---"
    $ADB "${ADB_ARGS[@]}" shell ps -A -o PID,ARGS 2>&1 | grep -c "refresh_rate_service" | xargs echo "remaining refresh_rate_service processes:"
    $ADB "${ADB_ARGS[@]}" shell ps -A 2>&1 | grep -i "akihz" | wc -l | xargs echo "total akihz ps lines:"
    ;;
  trigger-crash)
    run_broadcast trigger_crash "${2:-CLI triggered crash}"
    ;;
  open-performance|perf-open)
    echo "→ opening PerformanceMonitorActivity"
    $ADB "${ADB_ARGS[@]}" shell am start -n "$PKG/akihz.anlaki.dev.presentation.PerformanceMonitorActivity" 2>&1
    ;;
  open-crash|crash-open)
    echo "→ opening CrashLogActivity"
    $ADB "${ADB_ARGS[@]}" shell am start -n "$PKG/akihz.anlaki.dev.presentation.CrashLogActivity" 2>&1
    ;;
  open-debug)
    PAGE="${2:-categories}"
    echo "→ opening MainActivity debug page: $PAGE"
    $ADB "${ADB_ARGS[@]}" shell am start -n "$PKG/akihz.anlaki.dev.presentation.MainActivity" --es akihz.extra.DEBUG_PAGE "$PAGE" 2>&1
    ;;
  pull-crash)
    FILE="${2:-}"
    DEST="${3:-.}"
    if [[ -z "$FILE" ]]; then echo "Usage: $0 pull-crash <filename> [dest]"; exit 1; fi
    echo "→ pulling crash $FILE"
    $ADB "${ADB_ARGS[@]}" shell run-as "$PKG" cat "/data/data/$PKG/files/crash_logs/$FILE" > "$DEST/$FILE" 2>&1 && echo "Saved to $DEST/$FILE" || echo "Failed"
    ;;
  pull-all-crashes)
    DEST="${2:-./crashes}"
    mkdir -p "$DEST"
    echo "→ pulling all crashes to $DEST"
    TMP=$(mktemp)
    $ADB "${ADB_ARGS[@]}" shell run-as "$PKG" ls "/data/data/$PKG/files/crash_logs/" 2>/dev/null | tr -d '\r' > "$TMP" || true
    while IFS= read -r f; do
      [[ -z "$f" ]] && continue
      echo "  $f"
      $ADB "${ADB_ARGS[@]}" shell run-as "$PKG" cat "/data/data/$PKG/files/crash_logs/$f" > "$DEST/$f" 2>&1 || echo "    failed"
    done < "$TMP"
    rm "$TMP"
    ls -lh "$DEST" 2>&1 | head -n 20
    ;;
  pull-perf)
    DEST="${2:-./perf.log}"
    echo "→ pulling active perf log"
    run_broadcast perf_cat > /tmp/aki_perf.txt 2>&1 || true
    $ADB "${ADB_ARGS[@]}" shell run-as "$PKG" cat "/data/data/$PKG/cache/perf/perf-active.log" > "$DEST" 2>&1 && echo "Saved to $DEST" || echo "No active perf log, trying broadcast output"
    cat /tmp/aki_perf.txt | head -n 100
    ;;
  logcat)
    echo "→ logcat for $PKG (Ctrl+C to stop)"
    $ADB "${ADB_ARGS[@]}" logcat --pid=$($ADB "${ADB_ARGS[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r') -v time 2>&1 | head -n 200 || $ADB "${ADB_ARGS[@]}" logcat -d -v time 2>&1 | grep -i "akihz\|AndroidRuntime" | tail -n 100
    ;;
  logcat-crash)
    $ADB "${ADB_ARGS[@]}" logcat -d -v time *:E 2>&1 | grep -E "AndroidRuntime|FATAL|akihz" | tail -n 100
    ;;
  help|--help|-h)
    cat <<'EOF'
Debug CLI – akihz
Detected package: PKG (auto)
Direct activity launches (no UI tap needed):
  open-performance      launch PerformanceMonitorActivity directly
  open-crash           launch CrashLogActivity directly
  open-debug <page>    launch MainActivity with debug_page=performance|crash|home|categories
Broadcast CLI (via DebugCliReceiver, works even if UI not visible):
  list-crashes         list files in files/crash_logs
  cat-crash <name>     cat file content (partial match allowed)
  create-test-crash    write error-*-test_crash.log
  clear-crashes        delete all crash files
  trigger-crash [msg]  write sync crash log (simulates uncaught)
  perf-status          show PerformanceMonitor state
  perf-start           start perf recording
  perf-stop            stop perf recording
  perf-cat             cat active perf log (now includes per-process pss/cpu)
  ps|list-processes    list all akihz PIDs with RSS/threads + ps + meminfo
  kill-stale|cleanup   kill stale refresh_rate_service shells (frees RAM, version bump to 3)
File pulls (no UI):
  pull-crash <file> [dest]     run-as cat single crash file
  pull-all-crashes [dest]      pull all crashes to ./crashes
  pull-perf [dest]             pull perf-active.log
Log helpers:
  logcat               tail logcat for app pid
  logcat-crash         filter logcat for crashes
All broadcast results also written to files/debug_cli_output.txt, readable via:
  adb shell run-as PKG cat /data/data/PKG/files/debug_cli_output.txt
EOF
    ;;
  *)
    echo "Unknown cmd: $1"
    echo "Try: $0 help"
    exit 1
    ;;
esac
