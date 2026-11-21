package nihil.lain.sunshine;

/* JADX INFO: loaded from: classes2.dex */
public final class Policy {
    public static final long LEASE_MS = 900;
    public static final String WARNING = "New code path expects SceneContainerFlag to be enabled.";

    public static boolean shadeAllowed(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7) {
        return (!z || z2 || z3 || z4 || z5 || !z6 || z7) ? false : true;
    }

    private Policy() {
    }

    public static boolean allowed(String str, String str2, String str3) {
        return "com.android.systemui".equals(str) && "com.android.systemui".equals(str2) && str3 != null && str3.contains("Sunshine");
    }

    public static boolean qsFrameLog(String str, String str2) {
        if ("QsOpenAnimator".equals(str2) && "panelExpandedFraction : ".equals(str)) {
            return true;
        }
        if ("QsExpandAnimator".equals(str2)) {
            return "setQsExpansionPosition ".equals(str) || "expandImmediate ".equals(str);
        }
        return false;
    }

    public static boolean downwardDrag(float f, float f2, float f3) {
        return Float.isFinite(f) && Float.isFinite(f2) && f2 > f3 && f2 > Math.abs(f);
    }

    public static float selectRate(float[] fArr, float f, boolean z) {
        if (z) {
            return 0.0f;
        }
        if (!Float.isFinite(f) || f <= 0.0f) {
            f = Float.MAX_VALUE;
        }
        float f2 = 0.0f;
        for (float f3 : fArr) {
            if (Float.isFinite(f3) && f3 > f2 && f3 <= 0.5f + f) {
                f2 = f3;
            }
        }
        if (f2 > 60.5f) {
            return f2;
        }
        return 0.0f;
    }
}
