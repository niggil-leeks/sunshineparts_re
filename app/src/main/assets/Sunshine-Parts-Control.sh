#!/system/bin/sh
# Root-only rescue/control helper for the supplied 2.31.1-r.
[ "$(id -u)" = 0 ] || { echo 'Run with su.' >&2; exit 1; }
sunshine_display=$(getprop ro.build.display.id | tr '[:upper:]' '[:lower:]')
case "$sunshine_display" in *sunshine*) echo 'Retriever only.' >&2; exit 16 ;; esac
printf '%s' "$sunshine_display" | grep -Eq '(^|[^[:alnum:]_])retriever([^[:alnum:]_]|$)' || { echo 'Retriever only.' >&2; exit 16; }
[ "$(getprop ro.build.version.sdk)" = 36 ] || { echo 'Android 16 is required.' >&2; exit 16; }
case "$(getprop ro.build.version.oneui)" in 800[0-9][0-9]) ;; *) echo 'One UI 8.0 is required.' >&2; exit 16 ;; esac
action=${1:-status}
set_option() {
 if command -v resetprop >/dev/null 2>&1; then resetprop -p "persist.sys.sunshineparts.$1" "$2"; else setprop "persist.sys.sunshineparts.$1" "$2"; fi
 [ "$(getprop "persist.sys.sunshineparts.$1")" = "$2" ] || exit 2
}
case "$action" in
 status) op=status ;;
 recovery)
  case "${2:-}" in on) set_option recovery_mode 1 ;; off) set_option recovery_mode 0 ;; *) exit 3 ;; esac
  echo "Restart the phone to apply the runtime mode."; op=reload ;;
 workload)
  case "${2:-}" in off) mode=off ;; auto|automatic) mode=auto ;; light) mode=light ;; extra|extra-light) mode=extra ;; ultra|ultra-light) mode=ultra ;; *) echo 'Choose off, automatic, light, extra-light or ultra-light.' >&2; exit 3 ;; esac
  set_option workload_mode "$mode"; op=reload ;;
 workload-detail)
  case "${2:-}" in light) mode=light ;; extra-light) mode=extra ;; ultra-light) mode=ultra ;; *) exit 3 ;; esac
  set_option workload_mode "$mode"; op=reload ;;
 prepared-blur)
  case "${2:-}" in on) set_option prepared_blur 1 ;; off) set_option prepared_blur 0 ;; *) exit 3 ;; esac
  op=reload
  ;;
 native-blur|native-animations)
  case "${2:-}" in on) value=0 ;; off) value=1 ;; *) echo 'Choose on or off.' >&2; exit 3 ;; esac
  case "$action" in native-blur) key=disable_blur ;; native-animations) key=disable_motion ;; esac
  set_option "$key" "$value"; op=reload ;;
 soundalive-read)
  case "${2:-}" in on) set_option soundalive_async 1 ;; off) set_option soundalive_async 0 ;; *) exit 3 ;; esac
  op=reload ;;
 edge-extension)
  case "${2:-}" in on) set_option reduce_edge_extension 1 ;; off) set_option reduce_edge_extension 0 ;; *) exit 3 ;; esac
  op=reload ;;
 transition-effects)
  case "${2:-}" in default|shadow|corners) set_option transition_effects "$2" ;; *) exit 3 ;; esac
  op=reload ;;
 defaults)
  set_option recovery_mode 0
  set_option soundalive_async 0
  set_option reduce_edge_extension 1
  set_option transition_effects shadow
  set_option launcher_preset low
  set_option smart_ram 0
  set_option gpu_blur 0
  set_option compose_layout 0
  set_option compose_host 0
  set_option workload_mode auto
  set_option light_ui 0
  set_option extra_light_ui 0
  for key in disable_blur disable_motion; do set_option "$key" 0; done
  set_option refresh 0
  for key in launcher_motion lean_qs prepared_blur tile_layers tile_layout tile_work light_tone adaptive_workload; do set_option "$key" 1; done
  set_option media_step 4; op=reload ;;
 *) echo 'Usage: sh Sunshine-Parts-Control.sh [status|recovery on|off|workload off|automatic|light|extra-light|ultra-light|workload-detail light|extra-light|ultra-light|native-blur on|off|native-animations on|off|prepared-blur on|off|soundalive-read on|off|edge-extension on|off|transition-effects default|shadow|corners|defaults]' >&2; exit 3 ;;
esac
for pkg in com.android.systemui android com.sec.android.app.launcher; do
 echo "$pkg"
 result=$(am broadcast --receiver-foreground -a nihil.sunshine.parts.FLIGHT_CONTROL -p "$pkg" --es op "$op" --es nonce 25000000000000000000000000000000 2>&1)
 data=$(printf '%s\n' "$result" | sed -n 's/.*SP250:25000000000000000000000000000000:\([A-Za-z0-9+\/=]*\).*/\1/p')
 if [ -n "$data" ]; then printf '%s' "$data" | base64 -d; printf '\n'; else echo 'No module reply; check scope and reboot.'; fi
done
