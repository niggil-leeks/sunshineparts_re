package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class BlurPlan {
    static int[] size(int i, int i2, int i3, int i4) {
        if (i < 1 || i2 < 1 || i3 < 1 || i4 < 1) {
            throw new IllegalArgumentException("Blur dimensions");
        }
        float f = i;
        float f2 = i2;
        float fMin = Math.min(1.0f, Math.min(i3 / f, i4 / f2));
        return new int[]{Math.max(1, Math.round(f * fMin)), Math.max(1, Math.round(f2 * fMin))};
    }

    static float[] color(boolean z) {
        float[] fArr = new float[20];
        fArr[12] = 1.0f;
        fArr[6] = 1.0f;
        fArr[0] = 1.0f;
        fArr[19] = 255.0f;
        return fArr;
    }

    private BlurPlan() {
    }
}
