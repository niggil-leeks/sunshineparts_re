#!/system/bin/sh
# Optional migration between signing certificates. Run backup before uninstalling.
set -eu
[ "$(id -u)" = 0 ] || { echo 'Run with su.' >&2; exit 1; }
pkg=nihil.sunshine.parts
data=/data/user/0/$pkg
saved=/data/adb/sunshineparts-migration-218
case "${1:-}" in
 backup)
  [ -d "$data" ] || { echo 'Installed app data not found.' >&2; exit 2; }
  [ ! -e "$saved" ] || { echo 'A backup already exists. Keep it until migration is verified.' >&2; exit 3; }
  am force-stop "$pkg"
  umask 077
  mkdir "$saved"
  if [ -f "$data/shared_prefs/flight_baseline.xml" ]; then cp "$data/shared_prefs/flight_baseline.xml" "$saved/flight_baseline.xml"; fi
  if [ -d "$data/files/performance-baseline" ]; then cp -R "$data/files/performance-baseline" "$saved/performance-baseline"; fi
  echo 'Preferences and original CPU policy baselines saved. You can now uninstall the old APK and install 2.31.1-r.'
  ;;
 restore)
  [ -d "$saved" ] && [ -d "$data" ] || { echo 'Backup or installed app data not found. Open the new app once first.' >&2; exit 4; }
  uid=$(stat -c %u "$data")
  case "$uid" in ''|*[!0-9]*) exit 5 ;; esac
  [ "$uid" -ge 10000 ] || { echo 'App UID not verified.' >&2; exit 5; }
  if [ -d "$saved/performance-baseline" ] && [ -e "$data/files/performance-baseline" ]; then
   echo 'New CPU baseline already exists. Restore before applying CPU changes in the new app.' >&2; exit 6
  fi
  am force-stop "$pkg"
  mkdir -p "$data/shared_prefs" "$data/files"
  if [ -f "$saved/flight_baseline.xml" ]; then
   cp "$saved/flight_baseline.xml" "$data/shared_prefs/flight_baseline.xml"
   rm -f "$data/shared_prefs/flight_baseline.xml.bak"
   chown "$uid:$uid" "$data/shared_prefs" "$data/shared_prefs/flight_baseline.xml"
   chmod 600 "$data/shared_prefs/flight_baseline.xml"
   restorecon -RF "$data/shared_prefs"
  fi
  if [ -d "$saved/performance-baseline" ]; then
   cp -R "$saved/performance-baseline" "$data/files/performance-baseline"
   chown -R "$uid:$uid" "$data/files/performance-baseline"
   restorecon -RF "$data/files/performance-baseline"
  fi
  echo 'Settings restored. Grant root, enable the three LSPosed scopes and reboot. Backup retained.'
  ;;
 *) echo 'Usage: su -c "sh Sunshine-Parts-Migrate.sh backup|restore"' >&2; exit 7 ;;
esac
