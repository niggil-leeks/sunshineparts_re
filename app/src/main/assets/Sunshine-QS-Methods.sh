#!/system/bin/sh
# Explicit, bounded ART sampling for Sunshine Parts 2.8.0. No reboot or setting changes.
set -u
[ "$(id -u)" = 0 ] || { echo 'Run as root.' >&2; exit 1; }
out=${1:-/sdcard/Download}
[ "$#" -le 1 ] && [ -d "$out" ] || exit 2
for tool in timeout base64 tar mktemp; do command -v "$tool" >/dev/null 2>&1 || exit 3; done
umask 077
work=$(mktemp -d /data/local/tmp/sunshine-methods.XXXXXX) || exit 4
owned=0
counter=0
keep=0
control() {
  op=$1
  counter=$((counter + 1))
  nonce=$(printf '%016x%08x%08x' "$(date +%s)" "$$" "$counter")
  timeout 8 am broadcast --receiver-foreground -a nihil.sunshine.parts.FLIGHT_CONTROL \
    -p com.android.systemui --es op "$op" --es nonce "$nonce" > "$work/broadcast-$counter.txt" 2>&1 || return 1
  encoded=$(sed -n "s/.*SP250:$nonce:\([A-Za-z0-9+\/=]*\).*/\1/p" "$work/broadcast-$counter.txt")
  [ -n "$encoded" ] || return 1
  printf '%s' "$encoded" | base64 -d
}
cleanup() {
  code=$?
  trap - EXIT
  trap '' HUP INT TERM
  if [ "$owned" = 1 ]; then
    control profile_stop > "$work/cleanup-stop.json" 2>&1 || true
  fi
  if [ "$keep" = 0 ]; then rm -rf "$work"; else echo "Files retained: $work" >&2; fi
  exit "$code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' HUP TERM

control status > "$work/before-module.json" || { echo 'SystemUI module did not reply.' >&2; exit 5; }
grep -q '"profile_state":' "$work/before-module.json" || { echo 'Install and load Sunshine Parts 2.8.0 first.' >&2; exit 6; }
if grep -q '"profile_state":"recording"' "$work/before-module.json"; then
  echo 'Another diagnostic sampling session is running. Leave it to finish.' >&2
  exit 7
fi
timeout 6 dumpsys gfxinfo com.android.systemui > "$work/before-gfx.txt" 2>&1 || true
timeout 6 dumpsys SurfaceFlinger > "$work/surfaceflinger.txt" 2>&1 || true
{
  date -u
  uname -a
  getprop | grep -E 'sunshineparts|debug\.sf\.|ro\.surface_flinger\.|ro\.sf\.|debug\.hwui|ro\.build\.display'
  printf '\nTrace filesystem support:\n'
  cat /proc/filesystems
  ls -ld /sys/kernel/tracing /sys/kernel/debug/tracing 2>&1
} > "$work/environment.txt" 2>&1

# Mark a possibly delivered start as owned before sending it: a lost broadcast acknowledgment
# must still be followed by stop. An external profiler is never stopped by PanelProfiler.
owned=1
if ! control profile_start > "$work/start.json"; then
  echo 'Sampling acknowledgment missing; stopping the requested diagnostic.' >&2
  keep=1
  exit 8
fi
if ! grep -q '"profile_state":"recording"' "$work/start.json"; then
  owned=0
  cat "$work/start.json" >&2
  echo 'Sampling was unavailable; no security setting was changed.' >&2
  keep=1
  exit 9
fi
echo 'Get ready. For 12 seconds, repeatedly open and close QS, then notifications.'
for n in 3 2 1; do echo "$n"; sleep 1; done
date -u > "$work/gesture-start.txt"
echo 'RECORDING — pull the panel now.'
sleep 12
date -u > "$work/gesture-end.txt"
control profile_stop > "$work/stop.json" || { keep=1; echo 'Stop acknowledgment missing; the 18-second watchdog also stops this sampling session.' >&2; exit 10; }
owned=0
grep -q '"profile_state":"finished"' "$work/stop.json" || { keep=1; cat "$work/stop.json" >&2; exit 11; }
profile=$(sed -n 's/.*"profile_file":"\([^"]*\)".*/\1/p' "$work/stop.json" | sed 's#\\/#/#g')
case "$profile" in
  /data/user_de/[0-9]*/com.android.systemui/cache/sunshine-qs-*.trace|/data/user/[0-9]*/com.android.systemui/cache/sunshine-qs-*.trace|/data/data/com.android.systemui/cache/sunshine-qs-*.trace) ;;
  *) keep=1; echo 'Unexpected diagnostic path; copy skipped.' >&2; exit 12 ;;
esac
[ -f "$profile" ] && [ ! -L "$profile" ] && [ -s "$profile" ] || { keep=1; echo 'Sampling file missing or empty.' >&2; exit 13; }
cp "$profile" "$work/systemui-methods.trace" || { keep=1; exit 14; }
timeout 6 dumpsys gfxinfo com.android.systemui framestats > "$work/after-gfx.txt" 2>&1 || true
timeout 6 dumpsys meminfo com.android.systemui > "$work/after-memory.txt" 2>&1 || true
timeout 6 dumpsys cpuinfo > "$work/cpu.txt" 2>&1 || true
timeout 6 logcat -b main -b system -b crash -d -v threadtime -t 4000 '*:V' > "$work/logcat.txt" 2>&1 || true
cat > "$work/READ-ME.txt" <<'EOF'
ART sampled methods, 20,000 us interval, at most 18 seconds, explicitly requested by root/shell.
Use for attribution only. ART sampling adds overhead; do not use its FPS as an optimization benchmark.
The profiler does not start at boot and never stops an external profiler.
No CPU/GPU, blur, animation, graphics-property or SELinux configuration changes are made.
The source trace is in SystemUI's cache; this archive contains its copy.
The archive may include application/notification names and device details.
EOF
archive="$out/Sunshine-QS-Methods-$(date +%Y%m%d-%H%M%S)-$$.tar.gz"
[ ! -e "$archive" ] && [ ! -L "$archive" ] || exit 15
tar -czf "$archive" -C "$work" . || { keep=1; exit 16; }
echo "Saved: $archive"
