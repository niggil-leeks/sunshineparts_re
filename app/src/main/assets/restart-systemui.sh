#!/system/bin/sh
set -eu
[ "$(id -u)" = 0 ] || exit 1
sunshine_display=$(getprop ro.build.display.id | tr '[:upper:]' '[:lower:]')
case "$sunshine_display" in *sunshine*) echo 'Retriever only.' >&2; exit 16 ;; esac
printf '%s' "$sunshine_display" | grep -Eq '(^|[^[:alnum:]_])retriever([^[:alnum:]_]|$)' || { echo 'Retriever only.' >&2; exit 16; }
[ "$(getprop ro.build.version.sdk)" = 36 ] || { echo 'Android 16 is required.' >&2; exit 16; }
case "$(getprop ro.build.version.oneui)" in 800[0-9][0-9]) ;; *) echo 'One UI 8.0 is required.' >&2; exit 16 ;; esac
before=$(pidof com.android.systemui || true)
[ -n "$before" ] || { echo 'SystemUI is not running.' >&2; exit 2; }
killed=0
for pid in $before; do
 case "$pid" in ''|*[!0-9]*) continue ;; esac
 name=$(tr '\000' '\n' < "/proc/$pid/cmdline" 2>/dev/null | head -n 1)
 [ "$name" = com.android.systemui ] || continue
 kill -KILL "$pid"
 killed=1
done
[ "$killed" = 1 ] || { echo 'No exact SystemUI process was verified.' >&2; exit 3; }
for attempt in 1 2 3 4 5 6 7 8; do
 sleep 1
 after=$(pidof com.android.systemui || true)
 [ -n "$after" ] && [ "$after" != "$before" ] && { echo "SystemUI restarted. PID: $before -> $after"; exit 0; }
done
echo 'SystemUI was stopped; a replacement process has not appeared yet.' >&2
exit 4
