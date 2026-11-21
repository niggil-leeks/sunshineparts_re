package nihil.sunshine.parts;

import java.lang.reflect.Method;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public final class Config {
    public static final String PREFIX = "persist.sys.sunshineparts.";
    public static final String VERSION = "2.31.3-r";
    public static final int VERSION_CODE = 23103;
    private static Method get;
    public static volatile boolean recoveryMode = false;
    public static volatile boolean nativeBlur = true;
    public static volatile boolean nativeAnimations = true;
    public static volatile boolean refresh = false;
    public static volatile boolean launcherMotion = true;
    public static volatile boolean leanQs = true;
    public static volatile boolean preparedBlur = true;
    public static volatile boolean gpuBlur = true;
    public static volatile boolean tileLayers = true;
    public static volatile boolean tileLayout = true;
    public static volatile boolean tileWork = true;
    public static volatile boolean composeLayout = true;
    public static volatile boolean composeHost = true;
    public static volatile boolean lightTone = true;
    public static volatile boolean adaptiveWorkload = true;
    public static volatile boolean lightUi = false;
    public static volatile boolean extraLightUi = false;
    public static volatile boolean smartRam = false;
    public static volatile boolean soundAliveAsync = false;
    public static volatile boolean reduceEdgeExtension = true;
    public static volatile String launcherPreset = "low";
    public static volatile int transitionEffects = 1;
    public static volatile int mediaStep = 4;
    public static volatile int workloadMode = 1;

    public static boolean compatibleRuntimeVersion(String str) {
        return VERSION.equals(str) || "2.31.2-r".equals(str) || "2.31.1-r".equals(str);
    }

    public static String transitionProfile() {
        if (transitionEffects == 1) {
            return "shadow";
        }
        return transitionEffects == 2 ? "corners" : "default";
    }

    public static String launcherProfile() {
        if ("auto".equals(launcherPreset)) {
            return AdaptiveWorkload.level() >= 2 ? "lowest" : "low";
        }
        return launcherPreset;
    }

    public static String property(String str) {
        try {
            if (get == null) {
                get = Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class);
            }
            return (String) get.invoke(null, str, "");
        } catch (Throwable th) {
            return "";
        }
    }

    public static boolean supported(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return lowerCase.matches("(?s).*\\bretriever\\b.*") && !lowerCase.contains("sunshine");
    }

    public static boolean target(String str, String str2, int i) {
        return i == 36 && supported(str) && str2 != null && str2.matches("800[0-9]{2}");
    }

    private static boolean flag(String str, boolean z) {
        String strProperty = property(PREFIX + str);
        if (strProperty.isEmpty()) {
            return z;
        }
        return "1".equals(strProperty) || "true".equals(strProperty);
    }

    public static synchronized void load() {
        int i;
        int i2;
        smartRam = flag("smart_ram", false);
        soundAliveAsync = flag("soundalive_async", false);
        reduceEdgeExtension = flag("reduce_edge_extension", true);
        String strProperty = property("persist.sys.sunshineparts.transition_effects");
        if ("corners".equals(strProperty)) {
            i = 2;
        } else {
            i = "default".equals(strProperty) ? 0 : 1;
        }
        transitionEffects = i;
        String strProperty2 = property("persist.sys.sunshineparts.launcher_preset");
        if (!"low".equals(strProperty2) && !"lowest".equals(strProperty2) && !"mass".equals(strProperty2) && !"high".equals(strProperty2)) {
            strProperty2 = "auto".equals(strProperty2) ? "auto" : "low";
        }
        launcherPreset = strProperty2;
        recoveryMode = flag("recovery_mode", false);
        nativeBlur = !flag("disable_blur", false);
        nativeAnimations = !flag("disable_motion", false);
        refresh = flag("refresh", false);
        launcherMotion = flag("launcher_motion", true);
        leanQs = flag("lean_qs", true);
        preparedBlur = flag("prepared_blur", true);
        gpuBlur = false;
        tileLayers = flag("tile_layers", true);
        tileLayout = flag("tile_layout", true);
        tileWork = flag("tile_work", true);
        composeLayout = false;
        composeHost = false;
        lightTone = flag("light_tone", true);
        String strProperty3 = property("persist.sys.sunshineparts.workload_mode");
        if ("off".equals(strProperty3)) {
            i2 = 0;
        } else if ("light".equals(strProperty3)) {
            i2 = 2;
        } else if ("extra".equals(strProperty3)) {
            i2 = 3;
        } else if ("ultra".equals(strProperty3)) {
            i2 = 4;
        } else {
            i2 = "auto".equals(strProperty3) ? 1 : 1;
        }
        workloadMode = i2;
        adaptiveWorkload = workloadMode == 1;
        lightUi = workloadMode >= 2;
        extraLightUi = workloadMode >= 3;
        try {
            int i3 = Integer.parseInt(property("persist.sys.sunshineparts.media_step"));
            if (i3 != 2 && i3 != 4 && i3 != 8) {
                i3 = 4;
            }
            mediaStep = i3;
        } catch (Exception e) {
            mediaStep = 4;
        }
        if (AccessState.blocked) {
            nativeOnly();
        }
    }

    static void nativeOnly() {
        nativeBlur = true;
        nativeAnimations = true;
        refresh = false;
        launcherMotion = false;
        leanQs = false;
        preparedBlur = false;
        gpuBlur = false;
        tileLayers = false;
        tileLayout = false;
        tileWork = false;
        composeLayout = false;
        composeHost = false;
        lightTone = false;
        adaptiveWorkload = false;
        lightUi = false;
        extraLightUi = false;
        smartRam = false;
        soundAliveAsync = false;
        reduceEdgeExtension = false;
        transitionEffects = 0;
        workloadMode = 0;
    }

    private Config() {
    }
}
