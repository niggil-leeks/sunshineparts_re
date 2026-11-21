package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class CareState {
    final int kind;
    final boolean optimize;
    final String summary;
    final String title;

    private CareState(String str, String str2, boolean z, int i) {
        this.title = str;
        this.summary = str2;
        this.optimize = z;
        this.kind = i;
    }

    static CareState recovery() {
        return new CareState("System recovery", "Native interface active. Module effects are paused.", false, 3);
    }

    static CareState temperature(boolean z, int i, long j, long j2) {
        if (!z) {
            return new CareState("Check module", "Check the SystemUI connection.", false, 3);
        }
        if (i < 0 || i > 700) {
            return new CareState("Checking", "Battery temperature is not available yet.", false, 3);
        }
        boolean z2 = j2 > 0 && j >= j2 && j - j2 < 45000;
        if (i <= 350) {
            return new CareState("Great", "Your phone is cool. No optimization is needed.", false, 0);
        }
        if (i < 390) {
            return new CareState("Good", z2 ? "Your visual profile has just been checked." : "Your phone is running well. Check the visual profile to keep it responsive.", !z2, 1);
        }
        return new CareState(i < 430 ? "Warm" : "Hot", z2 ? "The visual profile is ready. Give your phone time to cool." : "A lighter visual profile can help while your phone cools down.", !z2, 2);
    }

    static boolean memoryPressure(long j, long j2, long j3, boolean z) {
        return j >= 0 && j2 > 0 && j <= j2 && (z || j < Math.max(262144L, Math.min(Math.max(0L, j3), j2)));
    }

    static CareState assess(boolean z, long j, long j2, long j3, boolean z2, int i, long j4, long j5) {
        if (!z) {
            return new CareState("Check module", "Check the SystemUI connection.", false, 3);
        }
        if (j >= 0 && j2 > 0 && j <= j2) {
            long jMax = Math.max(262144L, Math.min(Math.max(0L, j3), j2));
            boolean z3 = z2 || j < jMax;
            boolean z4 = j5 > 0 && j4 >= j5 && j4 - j5 < 45000;
            if (z3) {
                return new CareState("Needs attention", z4 ? "Android is managing memory. Give it a moment." : "Memory is low. Eligible cached apps can be released.", true ^ z4, 2);
            }
            if (i >= 2 && i <= 6) {
                return new CareState(i >= 4 ? "Hot" : "Warm", "Use Light UI workload while the device cools.", false, 2);
            }
            if (j - jMax < jMax) {
                return new CareState("Good", z4 ? "Memory has just been checked." : "Some cached apps can be cleared if you need more memory.", !z4, 1);
            }
            if (j / j2 < 0.4d) {
                return new CareState("Good", "Your system has enough memory for everyday use.", false, 1);
            }
            return new CareState("Great", "There is enough memory headroom. No optimization needed.", false, 0);
        }
        return new CareState("Checking", "Memory information is not available yet.", false, 3);
    }

    private CareState() {
        throw new AssertionError();
    }
}
