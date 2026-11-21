package nihil.sunshine.parts;

import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class MotionPolicy {
    private static final ClassValue<Integer> VIEW_RULES = new ClassValue<Integer>() { // from class: nihil.sunshine.parts.MotionPolicy.1
        @Override // java.lang.ClassValue
        protected /* bridge */ /* synthetic */ Integer computeValue(Class cls) {
            return computeValue((Class<?>) cls);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.lang.ClassValue
        protected Integer computeValue(Class<?> cls) {
            String name = cls.getName();
            return Integer.valueOf((MotionPolicy.visualRoot(name) ? 2 : 0) | (MotionPolicy.protectedClass(name) ? 1 : 0));
        }
    };

    static int viewRules(Class<?> cls) {
        return VIEW_RULES.get(cls).intValue();
    }

    static boolean regionClass(String str) {
        return str != null && (str.startsWith("com.android.systemui.qs.") || str.startsWith("com.android.systemui.samsung.quicksetting.") || str.startsWith("com.android.systemui.shade.") || str.startsWith("com.android.systemui.statusbar.notification."));
    }

    static boolean protectedClass(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return lowerCase.startsWith("com.android.systemui.media.") || lowerCase.startsWith("com.android.systemui.volume.") || lowerCase.contains("audiovis") || lowerCase.contains("seekbar") || lowerCase.contains("wave") || lowerCase.contains("mediacontrol") || lowerCase.contains("keyguard") || lowerCase.contains("bouncer") || lowerCase.contains("biometric") || lowerCase.contains("doze") || lowerCase.contains("password") || lowerCase.contains("clock");
    }

    static boolean visualRoot(String str) {
        return regionClass(str) && (str.contains("QS") || str.contains("Qs") || str.contains("Tile") || str.contains("NotificationStack") || str.contains("ExpandableNotification") || str.contains("NotificationShadeWindowView"));
    }

    private MotionPolicy() {
    }
}
