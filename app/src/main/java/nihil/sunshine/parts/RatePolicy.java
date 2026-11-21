package nihil.sunshine.parts;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class RatePolicy {
    static final int TARGET_HEIGHT = 2400;
    static final int TARGET_WIDTH = 1080;

    static boolean isTargetMode(int i, int i2, float f, float f2) {
        return isTargetMode(i, i2, f, f2, TARGET_WIDTH, TARGET_HEIGHT);
    }

    static boolean isTargetMode(int i, int i2, float f, float f2, int i3, int i4) {
        return i3 > 0 && i4 > 0 && i == i3 && i2 == i4 && Float.isFinite(f) && Float.isFinite(f2) && Math.abs(f - 120.0f) < 0.1f && Math.abs(f2 - f) < 0.1f;
    }

    static boolean protectedVote(String str) {
        return str == null || !str.startsWith("PRIORITY_") || str.contains("THERMAL") || str.contains("TEMPERATURE") || str.contains("PROXIMITY") || str.contains("UDFPS") || str.contains("FINGERPRINT") || str.contains("LOW_POWER") || str.contains("LIMIT_MODE") || str.contains("LAYOUT_LIMITED") || str.contains("MAX_LIMIT_REFRESH_RATE") || str.contains("MIN_LIMIT_REFRESH_RATE");
    }

    static boolean compatibleLimit(String str, Object obj) {
        if (str == null || (!str.contains("MAX_LIMIT_REFRESH_RATE") && !str.contains("MIN_LIMIT_REFRESH_RATE"))) {
            return false;
        }
        try {
            return allows(obj, 120.0f, 0);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean allows(Object obj, float f, int i) throws Exception {
        if (obj == null || i > 4) {
            return false;
        }
        Class<?> superclass = obj.getClass();
        if (!superclass.getName().startsWith("com.android.server.display.mode.")) {
            return false;
        }
        if (!superclass.getName().equals("com.android.server.display.mode.CombinedVote")) {
            while (superclass != null) {
                if (!superclass.getName().equals("com.android.server.display.mode.RefreshRateVote")) {
                    superclass = superclass.getSuperclass();
                } else {
                    Field declaredField = superclass.getDeclaredField("mMinRefreshRate");
                    Field declaredField2 = superclass.getDeclaredField("mMaxRefreshRate");
                    declaredField.setAccessible(true);
                    declaredField2.setAccessible(true);
                    float f2 = declaredField.getFloat(obj);
                    float f3 = declaredField2.getFloat(obj);
                    return Float.isFinite(f2) && !Float.isNaN(f3) && f2 >= 0.0f && f3 >= f2 && f2 <= f + 0.1f && f3 >= f - 0.1f;
                }
            }
            return false;
        }
        Field declaredField3 = superclass.getDeclaredField("mVotes");
        declaredField3.setAccessible(true);
        Object obj2 = declaredField3.get(obj);
        if (obj2 instanceof List) {
            List list = (List) obj2;
            if (!list.isEmpty() && list.size() <= 16) {
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    if (!allows(it.next(), f, i + 1)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private RatePolicy() {
    }
}
