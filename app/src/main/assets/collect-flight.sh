#!/system/bin/sh
# Sunshine Parts 2.31.1-r — read-only diagnostic capture. Requires root.
# Usage: su -c 'sh /sdcard/Download/collect-flight.sh /sdcard/Download'
set -u
[ "$(id -u)" = 0 ] || { echo 'Root is required.' >&2; exit 1; }
out=${1:-/sdcard/Download}
[ -d "$out" ] || { echo 'Output directory is missing.' >&2; exit 2; }
umask 077
work=$(mktemp -d /data/local/tmp/sunshine-flight.XXXXXX) || exit 3
finish() { rm -rf "$work"; }
trap finish EXIT HUP INT TERM
capture() {
 name=$1; shift
 if command -v timeout >/dev/null 2>&1; then timeout 10 "$@" > "$work/$name.txt" 2>&1; else "$@" > "$work/$name.txt" 2>&1; fi
 return 0
}
pressure_snapshot() {
 for f in /proc/pressure/cpu /proc/pressure/memory /proc/pressure/io /proc/meminfo /proc/vmstat; do
  printf '\n%s\n' "$f"; [ ! -r "$f" ] || cat "$f"
 done
 for p in $(pidof com.android.systemui system_server com.sec.android.app.launcher 2>/dev/null); do
  case "$p" in *[!0-9]*|'') continue;; esac
  for f in status cpuset schedstat; do
   printf '\n/proc/%s/%s\n' "$p" "$f"; [ ! -r "/proc/$p/$f" ] || cat "/proc/$p/$f"
  done
 done
}
pressure_snapshot > "$work/pressure-before.txt" 2>&1
cat > "$work/frames.pbtxt" <<'EOF'
buffers { size_kb: 32768 fill_policy: RING_BUFFER }
duration_ms: 15000
data_sources { config { name: "linux.ftrace" ftrace_config {
 ftrace_events: "sched/sched_switch"
 ftrace_events: "sched/sched_waking"
 ftrace_events: "power/cpu_frequency"
 ftrace_events: "power/cpu_idle"
 atrace_categories: "gfx"
 atrace_categories: "view"
 atrace_categories: "wm"
 atrace_categories: "am"
 atrace_categories: "input"
 atrace_apps: "com.android.systemui"
 atrace_apps: "com.android.settings"
} } }
data_sources { config { name: "linux.process_stats" process_stats_config { scan_all_processes_on_start: true } } }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
EOF
# Record why a device can produce FrameTimeline without kernel/scheduling events.
# This inspects availability only; it does not enable tracing or change permissions.
capture trace-capabilities sh -c '
 getprop init.svc.traced; getprop init.svc.traced_probes
 command -v perfetto >/dev/null 2>&1 && perfetto --query
 for base in /sys/kernel/tracing /sys/kernel/debug/tracing; do
  ls -ld "$base" "$base/events/sched/sched_switch" "$base/events/power/cpu_frequency" 2>&1
  if [ -r "$base/available_events" ]; then head -n 80 "$base/available_events"; fi
 done
'
capture module-before am broadcast --receiver-foreground -a nihil.sunshine.parts.FLIGHT_CONTROL -p com.android.systemui --es op status --es nonce 25000000000000000000000000000001
capture ui-frames-before dumpsys gfxinfo com.android.systemui framestats
printf 'Recording 15 seconds: open and close QS 4 times, then notifications twice.\n' >&2
if command -v perfetto >/dev/null 2>&1 && command -v timeout >/dev/null 2>&1; then
 timeout 25 perfetto --txt -c "$work/frames.pbtxt" -o "$work/ui.perfetto-trace" > "$work/perfetto.txt" 2>&1 || true
else
 echo 'Perfetto or timeout unavailable; frame dumps are still collected.' > "$work/perfetto.txt"
fi
capture process-map ps -A -o USER,PID,PPID,NAME
capture cpu-top top -b -n 1 -m 30
pressure_snapshot > "$work/pressure-after.txt" 2>&1
capture activity-settings dumpsys activity settings
capture ui-frames dumpsys gfxinfo com.android.systemui framestats
capture settings-frames dumpsys gfxinfo com.android.settings framestats
capture launcher-frames dumpsys gfxinfo com.sec.android.app.launcher framestats
capture surfaceflinger dumpsys SurfaceFlinger
capture display dumpsys display
capture activity-policy dumpsys activity settings
capture memory dumpsys meminfo
capture thermal dumpsys thermalservice
capture logcat logcat -b main -b system -b crash -d -v threadtime -t 6000
capture kernel sh -c 'dmesg | tail -n 1500'
{
 date -u
 uname -a
 getprop | grep -E '^\[(ro.build.display.id|ro.build.version.(release|sdk|oneui)|ro.product.(device|model)|debug.sf\.|debug.hwui\.|debug.renderengine\.|persist.sys.sunshineparts\.|log.tag|persist.log.tag.SurfaceFlinger|init.svc.(argos-daemon|surfaceflinger|zygote64))' || true
 for key in max_cached_processes; do device_config get activity_manager "$key"; done
 for key in cached_apps_freezer enhanced_processing sem_enhanced_cpu_responsiveness sem_low_heat_mode; do printf '%s=' "$key"; settings get global "$key"; done
 for f in /sys/kernel/mm/vmscan/am_app_launch /sys/class/kgsl/kgsl-3d0/devfreq/min_freq /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq /sys/class/kgsl/kgsl-3d0/devfreq/max_freq /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage; do
  printf '%s: ' "$f"; if [ -r "$f" ]; then head -c 256 "$f"; printf '\n'; else echo 'missing or unreadable'; fi
 done
} > "$work/environment.txt" 2>&1
for pkg in com.android.systemui android com.sec.android.app.launcher; do
 nonce=25000000000000000000000000000000
 capture "module-$pkg" am broadcast --receiver-foreground -a nihil.sunshine.parts.FLIGHT_CONTROL -p "$pkg" --es op status --es nonce "$nonce"
done
cat > "$work/READ-ME.txt" <<'EOF'
Sunshine Parts flight diagnostic — 2.31.1-r
Capture is read-only. No property, service, log filter, cache or clock was changed.
ui.perfetto-trace records 15 seconds if supported. Open it at https://ui.perfetto.dev.
Sunshine:NativePanelBlur marks worker capture/filter/upload preparation. Sunshine:ApplyPanelBlur marks UI delivery.
Compare module-before with module-com.android.systemui for per-run counters.
Compare ui-frames-before.txt with ui-frames.txt for cumulative frame-count deltas. No statistics are reset.
pressure-before/after includes PSI, VM counters and effective process CPU groups without requiring ftrace.
Recovery mode skips rendering/display/launcher hooks. runtime_profile and runtime_restart_required report the effective startup mode.
Exercise split normal, split reversed and unified separately; include each layout name with its capture.
Pair frame misses with main/render thread work, CPU scheduling, GPU fences and SurfaceFlinger FrameTimeline.
trace-capabilities.txt records probe/kernel availability when scheduling events are missing.
Panel display Hz is not application FPS. Repeated error lines alone do not establish a performance cause.
Review logs before sharing: they may contain application names, notification content and device-specific details.
EOF
archive=$out/Sunshine-Flight-$(date +%Y%m%d-%H%M%S).tar.gz
[ ! -e "$archive" ] || { echo 'Archive already exists.' >&2; exit 4; }
tar -czf "$archive" -C "$work" . || exit 5
printf '%s\n' "$archive"
