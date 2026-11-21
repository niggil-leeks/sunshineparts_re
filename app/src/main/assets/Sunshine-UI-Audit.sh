#!/system/bin/sh
# Read-only scheduler/cgroup/graphics audit. No ART profiler or persistent configuration writes.
set -u
[ "$(id -u)" = 0 ] || { echo 'Run as root.' >&2; exit 1; }
[ "$#" -le 1 ] || exit 2
out=${1:-/sdcard/Download}
[ -d "$out" ] || exit 2
for command_name in timeout tar mktemp base64; do command -v "$command_name" >/dev/null 2>&1 || exit 3; done
umask 077
work=$(mktemp -d /data/local/tmp/sunshine-ui-audit.XXXXXX) || exit 4
log_pid=
saved=0
stop_log() {
  if [ -n "$log_pid" ]; then
    kill "$log_pid" 2>/dev/null || true
    wait "$log_pid" 2>/dev/null || true
    log_pid=
  fi
}
cleanup() {
  code=$?
  trap - EXIT
  stop_log
  if [ "$saved" = 1 ]; then rm -rf "$work"; else echo "Partial audit retained: $work" >&2; fi
  exit "$code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' HUP TERM
module_status() {
  label=$1
  nonce=$(printf '%016x%08x00000001' "$(date +%s)" "$$")
  timeout 6 am broadcast --receiver-foreground -a nihil.sunshine.parts.FLIGHT_CONTROL \
    -p com.android.systemui --es op status --es nonce "$nonce" > "$work/$label-broadcast.txt" 2>&1 || return 0
  encoded=$(sed -n "s/.*SP250:$nonce:\([A-Za-z0-9+\/=]*\).*/\1/p" "$work/$label-broadcast.txt")
  if [ -n "$encoded" ]; then printf '%s' "$encoded" | base64 -d > "$work/$label-module.json"; fi
}
pressure() {
  label=$1
  {
    date -u
    for file in /proc/pressure/cpu /proc/pressure/memory /proc/pressure/io /proc/meminfo /proc/swaps; do
      if [ -r "$file" ]; then printf '\n%s\n' "$file"; cat "$file"; fi
    done
  } > "$work/$label-pressure.txt" 2>&1
}
{
  date -u
  uname -a
  id
  getenforce
  getprop | grep -E 'ro\.build\.display|ro\.build\.version|ro\.product\.(model|device)|ro\.vendor\.(api|build)|sunshineparts|log\.tag|debug\.sf\.|ro\.surface_flinger\.|debug\.hwui|sys\.use_fifo_ui|cpuset|cgroup|sched'
  printf '\nControllers:\n'
  cat /proc/cgroups
  printf '\nMounts:\n'
  grep -E 'cgroup|cpuset|stune' /proc/self/mountinfo
  printf '\nKernel options if available:\n'
  if [ -r /proc/config.gz ]; then
    gzip -dc /proc/config.gz | grep -E 'CONFIG_(CGROUP|CPUSETS|SCHEDTUNE|UCLAMP|FAIR_GROUP|RT_GROUP|MEMCG|BLK_CGROUP|PSI)'
  fi
} > "$work/environment.txt" 2>&1
for file in /system/etc/task_profiles*.json /system/etc/task_profiles/*.json /system/etc/cgroups*.json \
    /vendor/etc/task_profiles*.json /vendor/etc/task_profiles/*.json /vendor/etc/cgroups*.json \
    /odm/etc/task_profiles*.json /odm/etc/cgroups*.json; do
  [ -f "$file" ] || continue
  relative=${file#/}
  dest="$work/configs/$relative"
  mkdir -p "${dest%/*}"
  cat "$file" > "$dest" 2>> "$work/config-errors.txt"
  ls -lZ "$file" >> "$work/config-metadata.txt" 2>&1
done
{
  for controller in /dev/cpuctl /dev/cpuset /dev/stune /dev/blkio /sys/fs/cgroup; do
    [ -d "$controller" ] || continue
    for directory in "$controller" "$controller"/top-app "$controller"/foreground "$controller"/background \
        "$controller"/system-background "$controller"/restricted; do
      [ -d "$directory" ] || continue
      printf '\nDIRECTORY %s\n' "$directory"
      ls -ldZ "$directory"
      for attr in tasks cgroup.procs cgroup.threads cgroup.controllers cgroup.subtree_control \
          cpus mems cpuset.cpus cpuset.mems cpu.shares cpu.weight cpu.uclamp.min cpu.uclamp.max \
          schedtune.boost schedtune.prefer_idle; do
        file="$directory/$attr"
        [ -e "$file" ] || continue
        ls -lZ "$file"
        [ -r "$file" ] && head -c 8192 "$file"
        printf '\n'
      done
    done
  done
} > "$work/controller-files.txt" 2>&1
{
  for file in /system/etc/init/hw/init*.rc /vendor/etc/init/hw/init*.rc /vendor/etc/init/init*.rc; do
    [ -f "$file" ] || continue
    matches=$(grep -n -E 'cpuctl|cpuset|stune|cgroup|timerslack|Olaf' "$file")
    if [ -n "$matches" ]; then printf '\n%s\n%s\n' "$file" "$matches"; fi
  done
} > "$work/init-cgroups.txt" 2>&1
{
  for process_name in system_server com.android.systemui com.sec.android.app.launcher surfaceflinger; do
    for process_id in $(pidof "$process_name" 2>/dev/null); do
      case "$process_id" in ''|*[!0-9]*) continue;; esac
      printf '\nPROCESS %s PID %s\n' "$process_name" "$process_id"
      grep -E '^(Name|State|Uid|Gid|Groups|Cap|Threads|VmRSS)' "/proc/$process_id/status"
      cat "/proc/$process_id/attr/current"
      cat "/proc/$process_id/cgroup"
      ls -l "/proc/$process_id/ns/mnt" /proc/self/ns/mnt
      grep -E 'cgroup|cpuset|stune' "/proc/$process_id/mountinfo"
      for task in /proc/"$process_id"/task/*; do
        [ -r "$task/comm" ] || continue
        IFS= read -r thread_name < "$task/comm" || continue
        thread_id=${task##*/}
        if [ "$thread_id" != "$process_id" ]; then
          case "$thread_name" in *RenderThread*|*hwui*|*UiThread*|*Animation*|*Choreographer*) :;; *) continue;; esac
        fi
        printf '\nTHREAD %s %s\n' "$thread_id" "$thread_name"
        for attr in cgroup sched schedstat timerslack_ns; do
          printf '%s\n' "$attr"; head -c 8192 "$task/$attr"
        done
      done
    done
  done
} > "$work/processes-before.txt" 2>&1
timeout 6 dmesg 2>&1 | tail -n 1500 > "$work/kernel-recent.txt"
timeout 6 dumpsys SurfaceFlinger > "$work/surfaceflinger-before.txt" 2>&1 || true
timeout 6 dumpsys activity settings > "$work/activity-settings.txt" 2>&1 || true
timeout 6 dumpsys thermalservice > "$work/thermal-before.txt" 2>&1 || true
for app in com.android.systemui com.sec.android.app.launcher; do
  timeout 6 dumpsys gfxinfo "$app" framestats > "$work/before-$app-gfx.txt" 2>&1 || true
done
module_status before
pressure before
timeout 13 logcat -b main -b system -b crash -v threadtime -T 1 \
  -f "$work/gesture-logcat.txt" -r 4096 -n 2 '*:V' > "$work/logcat-errors.txt" 2>&1 &
log_pid=$!
echo 'For 12 seconds: open/close QS and notifications, then swipe Home and open an app.'
date -u > "$work/gesture-start.txt"
sleep 12
date -u > "$work/gesture-end.txt"
wait "$log_pid" 2>/dev/null || true
log_pid=
pressure after
module_status after
timeout 6 dmesg 2>&1 | tail -n 1500 > "$work/kernel-after.txt"
for app in com.android.systemui com.sec.android.app.launcher; do
  timeout 6 dumpsys gfxinfo "$app" framestats > "$work/after-$app-gfx.txt" 2>&1 || true
  timeout 6 dumpsys meminfo "$app" > "$work/after-$app-memory.txt" 2>&1 || true
done
timeout 6 dumpsys cpuinfo > "$work/cpu.txt" 2>&1 || true
cat > "$work/READ-ME.txt" <<'EOF'
Explicit read-only UI audit. No settings, scheduling, cgroup, clock, SELinux or logging properties are changed.
No method profiler is started; existing logs are not cleared. Recording ends after 12 seconds (13-second logcat guard).
Log rotation bounds the three recording files to about 12 MiB. Snapshot commands still add some collection overhead.
Config files, controller ownership/labels, process groups, timer slack, module counters, graphics stats and memory pressure are included.
Activity/notification names and device information can occur in diagnostic output.
EOF
archive="$out/Sunshine-UI-Audit-$(date +%Y%m%d-%H%M%S)-$$.tar.gz"
[ ! -e "$archive" ] && [ ! -L "$archive" ] || exit 5
tar -czf "$archive" -C "$work" . || exit 6
saved=1
echo "Saved: $archive"
