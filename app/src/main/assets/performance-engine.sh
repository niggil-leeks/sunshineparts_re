#!/system/bin/sh
# Root compatibility engine. CPU policy limits only; hardware thermal/QoS limits remain active.
set -eu
[ "$(id -u)" = 0 ] || exit 1
sunshine_display=$(getprop ro.build.display.id | tr '[:upper:]' '[:lower:]')
case "$sunshine_display" in *sunshine*) echo 'Retriever only.' >&2; exit 16 ;; esac
printf '%s' "$sunshine_display" | grep -Eq '(^|[^[:alnum:]_])retriever([^[:alnum:]_]|$)' || { echo 'Retriever only.' >&2; exit 16; }
[ "$(getprop ro.build.version.sdk)" = 36 ] || { echo 'Android 16 is required.' >&2; exit 16; }
case "$(getprop ro.build.version.oneui)" in 800[0-9][0-9]) ;; *) echo 'One UI 8.0 is required.' >&2; exit 16 ;; esac
base=$1; mode=$2; light=$3
case "$mode:$light" in 0:0|0:1|1:0|1:1|2:0|2:1) ;; *) echo 'Invalid performance profile.' >&2; exit 2 ;; esac
umask 077
mkdir -p "$base"
policies=''
for p in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do
 [ -d "$p" ] || continue
 [ -w "$p/scaling_min_freq" ] && [ -w "$p/scaling_max_freq" ] || { echo "CPU policy is read-only: $p" >&2; exit 3; }
 policies="$policies $p"
done
[ -n "$policies" ] || { echo 'No writable CPU frequency policies were found.' >&2; exit 4; }
if [ "$mode" != 0 ] && [ "$(settings get global low_power 2>/dev/null || true)" = 1 ]; then
 echo 'Turn off Power saving before selecting High or Maximum processing speed.' >&2; exit 5
fi
identity=$({ uname -r; for p in $policies; do printf '%s\n' "${p##*/}"; cat "$p/cpuinfo_min_freq" "$p/cpuinfo_max_freq" "$p/scaling_driver"; done; } | cksum | awk '{print $1}')
store=$base/$identity
mkdir -p "$store"
transaction=$(mktemp -d "$base/transaction.XXXXXX")
ok=0
rollback(){
 if [ "$ok" != 1 ]; then
  for saved in "$transaction"/policy*; do
   [ -f "$saved" ] || continue
   name=${saved##*/}; p=/sys/devices/system/cpu/cpufreq/$name
   read -r lo hi < "$saved"
   lowest=$(cat "$p/cpuinfo_min_freq")
   printf '%s\n' "$lowest" > "$p/scaling_min_freq" || true
   printf '%s\n' "$hi" > "$p/scaling_max_freq" || true
   printf '%s\n' "$lo" > "$p/scaling_min_freq" || true
  done
 fi
 rm -rf "$transaction"
}
trap rollback EXIT HUP INT TERM
for p in $policies; do
 name=${p##*/}
 lo=$(cat "$p/scaling_min_freq"); hi=$(cat "$p/scaling_max_freq")
 floor=$(cat "$p/cpuinfo_min_freq"); ceiling=$(cat "$p/cpuinfo_max_freq")
 case "$lo:$hi:$floor:$ceiling" in *[!0-9:]*|'') echo "Invalid policy data: $name" >&2; exit 6 ;; esac
 [ "$floor" -gt 0 ] && [ "$lo" -le "$hi" ] && [ "$hi" -le "$ceiling" ] || exit 6
 printf '%s %s\n' "$lo" "$hi" > "$transaction/$name"
 if [ ! -f "$store/$name" ]; then printf '%s %s\n' "$lo" "$hi" > "$store/$name"; fi
 read -r original_min original_max < "$store/$name"
 case "$original_min:$original_max" in *[!0-9:]*|'') exit 6 ;; esac
 [ "$original_min" -ge "$floor" ] && [ "$original_max" -le "$ceiling" ] && [ "$original_min" -le "$original_max" ] || exit 6
 minimum=$original_min; maximum=$original_max
 if [ "$mode" = 1 ]; then minimum=$((original_max * 35 / 100)); fi
 if [ "$mode" = 2 ]; then minimum=$((original_max * 50 / 100)); fi
 [ "$minimum" -ge "$original_min" ] || minimum=$original_min
 if [ "$light" = 1 ]; then maximum=$((original_max * 80 / 100)); fi
 if [ -r "$p/scaling_available_frequencies" ]; then
  frequencies=$(cat "$p/scaling_available_frequencies")
  case "$frequencies" in *[!0-9[:space:]]*) exit 6 ;; esac
  minimum=$(printf '%s\n' "$frequencies" | tr ' ' '\n' | sort -n | awk -v target="$minimum" '$1 >= target { print $1; exit }')
  maximum=$(printf '%s\n' "$frequencies" | tr ' ' '\n' | sort -n | awk -v target="$maximum" '$1 > 0 && $1 <= target { n=$1 } END { print n }')
 fi
 [ -n "$minimum" ] && [ -n "$maximum" ] || exit 6
 [ "$maximum" -ge "$floor" ] || maximum=$floor
 [ "$minimum" -le "$maximum" ] || minimum=$maximum
 printf '%s\n' "$floor" > "$p/scaling_min_freq"
 printf '%s\n' "$maximum" > "$p/scaling_max_freq"
 printf '%s\n' "$minimum" > "$p/scaling_min_freq"
 actual_min=$(cat "$p/scaling_min_freq"); actual_max=$(cat "$p/scaling_max_freq")
 [ "$actual_min" -ge "$floor" ] && [ "$actual_max" -le "$ceiling" ] && [ "$actual_min" -le "$actual_max" ] || exit 7
 printf '%s: %s–%s kHz\n' "$name" "$actual_min" "$actual_max"
done
ok=1
printf 'Verified CPU limits. Mode=%s Light=%s\n' "$mode" "$light"
