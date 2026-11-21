package nihil.sunshine.parts.launcher;

import android.view.animation.Interpolator;

/* JADX INFO: loaded from: classes.dex */
public final class PocoCurve implements Interpolator {
    public static final double ALPHA_RESPONSE = 0.2d;
    public static final double CLOSE_RESPONSE = 0.32400000000000007d;
    public static final double DEVICE_RATIO = 0.8d;
    private static final double ERROR = 0.001d;
    public static final double OPEN_RESPONSE = 0.144d;
    private static final int STEPS = 512;
    public final long durationMs;
    private final float[] samples = new float[513];

    public PocoCurve(double d) {
        double d2 = 0.0d;
        if (d <= 0.0d || Double.isInfinite(d)) {
            throw new IllegalArgumentException("Invalid spring response");
        }
        double d3 = 6.283185307179586d / d;
        double d4 = 20.0d;
        for (int i = 0; i < 60; i++) {
            double d5 = (d2 + d4) / 2.0d;
            if ((1.0d + d5) * Math.exp(-d5) > ERROR) {
                d2 = d5;
            } else {
                d4 = d5;
            }
        }
        this.durationMs = (long) Math.ceil((d4 / d3) * 1000.0d);
        double d6 = (d3 * this.durationMs) / 1000.0d;
        double dExp = 1.0d - ((d6 + 1.0d) * Math.exp(-d6));
        for (int i2 = 0; i2 <= STEPS; i2++) {
            double d7 = (((double) i2) * d6) / 512.0d;
            this.samples[i2] = (float) ((1.0d - ((d7 + 1.0d) * Math.exp(-d7))) / dExp);
        }
        this.samples[0] = 0.0f;
        this.samples[STEPS] = 1.0f;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float f) {
        if (f <= 0.0f) {
            return 0.0f;
        }
        if (f >= 1.0f) {
            return 1.0f;
        }
        float f2 = f * 512.0f;
        int i = (int) f2;
        return this.samples[i] + ((this.samples[i + 1] - this.samples[i]) * (f2 - i));
    }

    public static float closeStiffness() {
        return (float) 376.07088862556606d;
    }
}
