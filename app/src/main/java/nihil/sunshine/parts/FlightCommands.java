package nihil.sunshine.parts;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
final class FlightCommands {
    static final Set<String> FLAGS = Collections.unmodifiableSet(new HashSet(Arrays.asList("disable_blur", "recovery_mode", "smart_ram", "soundalive_async", "reduce_edge_extension", "disable_motion", "refresh", "launcher_motion", "lean_qs", "prepared_blur", "gpu_blur", "tile_layers", "tile_layout", "tile_work", "compose_layout", "compose_host", "light_tone", "adaptive_workload", "light_ui", "extra_light_ui")));
    static final Set<String> MAX = Collections.unmodifiableSet(new HashSet(Arrays.asList("default", "4", "8", "12", "16", "24", "32", "48", "64")));
    static final String[] SF_OVERRIDES = {"debug.sf.disable_backpressure", "debug.sf.enable_gl_backpressure", "debug.sf.enable_advanced_sf_phase_offset", "debug.sf.early_app_phase_offset_ns", "debug.sf.early_gl_app_phase_offset_ns", "debug.sf.early_gl_phase_offset_ns", "debug.sf.early_phase_offset_ns", "debug.sf.high_fps_early_gl_phase_offset_ns", "debug.sf.high_fps_early_phase_offset_ns", "debug.sf.high_fps_late_app_phase_offset_ns", "debug.sf.high_fps_late_sf_phase_offset_ns", "debug.sf.latch_unsignaled", "debug.sf.gpu_freq_index"};

    static String quote(String str) {
        return "'" + str.replace("'", "'\\''") + "'";
    }

    static String property(String str, String str2) {
        if (!str.startsWith(Config.PREFIX) && !str.equals("debug.hwui.renderer") && !Arrays.asList(SF_OVERRIDES).contains(str)) {
            throw new IllegalArgumentException("Unsupported property");
        }
        if (!str2.matches("[A-Za-z0-9_.-]{0,40}")) {
            throw new IllegalArgumentException("Invalid value");
        }
        return "if command -v resetprop >/dev/null 2>&1; then resetprop " + (str.startsWith("persist.") ? "-p " : "") + quote(str) + " " + quote(str2) + "; else setprop " + quote(str) + " " + quote(str2) + "; fi\n[ \"$(getprop " + quote(str) + ")\" = " + quote(str2) + " ] || exit 12";
    }

    static String flag(String str, boolean z) {
        if (str.equals("native_blur")) {
            z = !z;
            str = "disable_blur";
        } else if (str.equals("native_animations")) {
            z = !z;
            str = "disable_motion";
        }
        if (FLAGS.contains(str)) {
            return property(Config.PREFIX + str, z ? "1" : "0");
        }
        throw new IllegalArgumentException("Unsupported option");
    }

    static String refreshSettings(String str, String str2) {
        StringBuilder sb = new StringBuilder("set -e\n");
        boolean z = !"null".equals(str) && Float.parseFloat(str) >= 120.0f;
        String[] strArr = new String[2];
        if (z) {
            strArr[0] = "peak_refresh_rate";
            strArr[1] = "min_refresh_rate";
        } else {
            strArr[0] = "min_refresh_rate";
            strArr[1] = "peak_refresh_rate";
        }
        String[] strArr2 = z ? new String[]{str2, str} : new String[]{str, str2};
        for (int i = 0; i < strArr.length; i++) {
            String str3 = strArr2[i];
            if (!"null".equals(str3)) {
                try {
                    float f = Float.parseFloat(str3);
                    if (!Float.isFinite(f) || f < 0.0f || f > 1000.0f) {
                        throw new IllegalArgumentException("Refresh rate");
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Refresh rate");
                }
            }
            sb.append("settings ").append("null".equals(str3) ? "delete" : "put").append(" system ").append(strArr[i]);
            if (!"null".equals(str3)) {
                sb.append(' ').append(quote(str3));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    static String workload(String str) {
        if (!Arrays.asList("off", "auto", "light", "extra", "ultra").contains(str)) {
            throw new IllegalArgumentException("Workload mode");
        }
        return property("persist.sys.sunshineparts.workload_mode", str);
    }

    static String maximum(String str) {
        if (!MAX.contains(str)) {
            throw new IllegalArgumentException("Unsupported app limit");
        }
        if (str.equals("default")) {
            return "device_config delete activity_manager max_cached_processes";
        }
        return "device_config put activity_manager max_cached_processes " + str;
    }

    static String freezer(String str) {
        if (!Arrays.asList("device_default", "enabled", "disabled").contains(str)) {
            throw new IllegalArgumentException("Unsupported freezer option");
        }
        return "settings put global cached_apps_freezer " + str;
    }

    static String processing(int i) {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException("Unsupported processing mode");
        }
        return "settings put global enhanced_processing " + i + "\nsettings put global sem_enhanced_cpu_responsiveness " + (i == 2 ? 1 : 0);
    }

    static String shaderQuarantine() {
        return "set -eu\nbase=/data/local/tmp/sunshineparts-shaders-$(date +%Y%m%d-%H%M%S)\n[ ! -e \"$base\" ] && [ ! -L \"$base\" ] || exit 13\numask 077\nmkdir \"$base\"\ncount=0\nfor user in /data/user/0 /data/user_de/0; do\n [ -d \"$user\" ] && [ ! -L \"$user\" ] || continue\n for pkg in com.android.systemui com.sec.android.app.launcher com.android.settings nihil.sunshine.parts; do\n root=$user/$pkg\n [ -d \"$root\" ] && [ ! -L \"$root\" ] || continue\n for area in cache code_cache; do\n dir=$root/$area\n [ -d \"$dir\" ] && [ ! -L \"$dir\" ] || continue\n find \"$dir\" -xdev -type f \\( -name com.android.opengl.shaders_cache -o -name com.android.skia.shaders_cache -o -name skia_shader_cache \\) -print | while IFS= read -r f; do\n  rel=${f#/data/}\n  dst=$base/$rel\n  mkdir -p \"${dst%/*}\"\n  mv \"$f\" \"$dst\"\n  printf '%s\\n' \"$rel\" >> \"$base/moved.txt\"\n done\n done\n done\ndone\nif [ -f \"$base/moved.txt\" ]; then wc -l < \"$base/moved.txt\"; printf 'shader cache files quarantined. Backup: %s\\n' \"$base\"; else rmdir \"$base\"; echo 'No matching UI shader caches found.'; fi";
    }

    private FlightCommands() {
    }
}
