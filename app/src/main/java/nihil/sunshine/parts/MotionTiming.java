package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class MotionTiming {
    static long scale(long j, float f) {
        if (j <= 0 || f == 1.0f || !Float.isFinite(f) || f <= 0.0f || f > 1.0f) {
            return j;
        }
        return Math.max(1L, Math.round(j * ((double) f)));
    }

    private MotionTiming() {
    }
}
