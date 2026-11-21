package nihil.sunshine.parts;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
final class DiagnosticLogs {
    static final Map<String, String> TARGETS;

    static {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("log.tag", "I");
        String[] strArr = {"APM_AudioPolicyManager", "ClatdController", "ConnectivityManager", "ConnectivityService", "IptablesRestoreController", "NetworkLogger", "SemDvfsHyPerManager", "SurfaceComposerClient", "SurfaceFlinger", "WindowManager", "gralloc", "snap_api::V2", "snap_api::V3", "snap_api::snap_eden::V3", "snap_api::snap_ofi::V1", "snap_api::snpe", "snap_compute::V2", "snap_compute::V3", "snap_hidl_v3", "snap_service@1.2", "snaplite_lib", "stats_log", "QuickTile", "PanelSplitOpenAnimator", "SecQpBlurController", "SecCapturedBlurContainerBinder", "SwipeToCollapseViewModel", "VolumeBarWithTile", "Codec2-MultiAccessUnitHelper", "Codec2-InputBufferManager", "SimbaDecoderJpeg", "HYPER-HAL", "HBD", "SecTileUtils", "QuickPanelLog"};
        for (int i = 0; i < 35; i++) {
            linkedHashMap.put("log.tag." + strArr[i], "I");
        }
        String[] strArr2 = {"View", "SpannableGrid", "EditableContainer", "QuickSettingTilePanel", "NowBarCardViewModel", "PanelTransitionAnimator", "SecPanelSplitHelper", "ShadeExpansionStateManager", "SecQuickSettingScene", "PluginLockMediatorImpl", "KeyguardVisible", "ImmersiveScrollAnimator", "StackScroller", "NotifHistoryProto", "RefactorFlag", "HoneySpace.RectFAnimation", "HoneySpace.FloatingAnimator", "HoneySpace.AnimationSession", "HoneySpace.OtherActivityInputConsumer"};
        for (int i2 = 0; i2 < 19; i2++) {
            linkedHashMap.put("log.tag." + strArr2[i2], "W");
        }
        TARGETS = Collections.unmodifiableMap(linkedHashMap);
    }

    static int priority(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        switch (str.toUpperCase(Locale.ROOT)) {
            case "V":
            case "VERBOSE":
                return 2;
            case "D":
            case "DEBUG":
                return 3;
            case "I":
            case "INFO":
                return 4;
            case "W":
            case "WARN":
            case "WARNING":
                return 5;
            case "E":
            case "ERROR":
                return 6;
            case "F":
            case "A":
            case "FATAL":
            case "ASSERT":
                return 7;
            case "S":
            case "SILENT":
            case "SUPPRESS":
                return 8;
            default:
                return -1;
        }
    }

    static Set<String> sources() {
        LinkedHashSet linkedHashSet = new LinkedHashSet(TARGETS.keySet());
        Iterator<String> it = TARGETS.keySet().iterator();
        while (it.hasNext()) {
            linkedHashSet.add("persist." + it.next());
        }
        return linkedHashSet;
    }

    static String read() {
        StringBuilder sb = new StringBuilder("[ \"$(id -u)\" = 0 ] || exit 13\n");
        for (String str : sources()) {
            sb.append("printf '").append(str).append("='; getprop '").append(str).append("'\n");
        }
        return sb.toString();
    }

    static Map<String, String> parse(String str) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        Set<String> setSources = sources();
        for (String str2 : str.split("\\n")) {
            int iIndexOf = str2.indexOf(61);
            if (iIndexOf > 0) {
                String strSubstring = str2.substring(0, iIndexOf);
                String strSubstring2 = str2.substring(iIndexOf + 1);
                if (setSources.contains(strSubstring) && priority(strSubstring2) >= 0) {
                    linkedHashMap.put(strSubstring, strSubstring2);
                }
            }
        }
        return linkedHashMap;
    }

    static String effective(Map<String, String> map, String str) {
        for (String str2 : str.equals("log.tag") ? new String[]{str, "persist." + str} : new String[]{str, "persist." + str, "log.tag", "persist.log.tag"}) {
            String str3 = map.get(str2);
            if (str3 != null && !str3.isEmpty()) {
                return str3;
            }
        }
        return "I";
    }

    static boolean reduced(Map<String, String> map) {
        for (String str : TARGETS.keySet()) {
            int iPriority = priority(TARGETS.get(str));
            if (priority(map.get(str)) < iPriority || priority(map.get("persist." + str)) < iPriority) {
                return false;
            }
        }
        return true;
    }

    static String write(Map<String, String> map, boolean z) {
        Set<String> setSources = sources();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!setSources.contains(entry.getKey()) || priority(entry.getValue()) < 0) {
                throw new IllegalArgumentException("Invalid log profile");
            }
        }
        StringBuilder sb = new StringBuilder("[ \"$(id -u)\" = 0 ] || exit 13\n");
        if (!z) {
            for (String str : setSources) {
                if (map.containsKey(str)) {
                    put(sb, str, map.get(str));
                }
            }
            return sb.toString();
        }
        for (String str2 : TARGETS.keySet()) {
            String str3 = TARGETS.get(str2);
            String[] strArr = {effective(map, str2), map.get(str2), map.get("persist." + str2), map.get("log.tag"), map.get("persist.log.tag")};
            for (int i = 0; i < 5; i++) {
                String str4 = strArr[i];
                if (priority(str4) > priority(str3)) {
                    str3 = str4;
                }
            }
            String[] strArr2 = {"persist." + str2, str2};
            for (int i2 = 0; i2 < 2; i2++) {
                String str5 = strArr2[i2];
                if (!str3.equals(map.get(str5))) {
                    put(sb, str5, str3);
                }
            }
        }
        return sb.toString();
    }

    private static void put(StringBuilder sb, String str, String str2) {
        sb.append("setprop '").append(str).append("' '").append(str2).append("' || exit 1\n");
    }

    private DiagnosticLogs() {
    }
}
