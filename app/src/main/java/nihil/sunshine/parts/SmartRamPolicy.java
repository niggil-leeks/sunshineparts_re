package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class SmartRamPolicy {
    private long last = -120000;
    private int lowSamples;

    SmartRamPolicy() {
    }

    boolean trim(long j, boolean z, long j2, long j3, long j4, boolean z2, boolean z3) {
        if (!z || z3 || j2 < 0 || j3 <= 0 || j2 > j3) {
            this.lowSamples = 0;
            return false;
        }
        long jMax = Math.max(268435456L, Math.min(Math.max(0L, j4), j3));
        if (!z2 && j2 >= jMax) {
            this.lowSamples = 0;
            return false;
        }
        int i = this.lowSamples + 1;
        this.lowSamples = i;
        if (i < 2 || j - this.last < 120000) {
            return false;
        }
        this.lowSamples = 0;
        this.last = j;
        return true;
    }
}
