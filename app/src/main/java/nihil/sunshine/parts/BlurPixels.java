package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class BlurPixels {
    static int[] previewSize(int i, int i2, int i3, int i4) {
        if (i < 1 || i2 < 1 || i3 < 1 || i4 < 1) {
            throw new IllegalArgumentException("Preview dimensions");
        }
        float f = i;
        float f2 = i2;
        float fMin = Math.min(1.0f, Math.min(Math.min(256, i3) / f, Math.min(256, i4) / f2));
        return new int[]{Math.max(1, Math.round(f * fMin)), Math.max(1, Math.round(f2 * fMin))};
    }

    static int[] soften(int[] iArr, int i, int i2, boolean z) {
        if (i < 1 || i2 < 1 || i > 256 || i2 > 256 || iArr == null || iArr.length != i * i2) {
            throw new IllegalArgumentException("Preview dimensions");
        }
        int[] iArr2 = (int[]) iArr.clone();
        int[] iArr3 = new int[iArr2.length];
        for (int i3 = 0; i3 < 2; i3++) {
            box(iArr2, iArr3, i, i2, true);
            box(iArr3, iArr2, i, i2, false);
        }
        return iArr2;
    }

    private static void box(int[] iArr, int[] iArr2, int i, int i2, boolean z) {
        int i3 = z ? i2 : i;
        if (z) {
            i2 = i;
        }
        for (int i4 = 0; i4 < i3; i4++) {
            int i5 = 0;
            int i6 = 0;
            int i7 = 0;
            for (int i8 = -3; i8 <= 3; i8++) {
                int iPixel = pixel(iArr, i4, clamp(i8, i2), i, z);
                i5 += (iPixel >> 16) & 255;
                i6 += (iPixel >> 8) & 255;
                i7 += iPixel & 255;
            }
            for (int i9 = 0; i9 < i2; i9++) {
                iArr2[z ? (i4 * i) + i9 : (i9 * i) + i4] = ((i5 / 7) << 16) | (-16777216) | ((i6 / 7) << 8) | (i7 / 7);
                int iPixel2 = pixel(iArr, i4, clamp(i9 - 3, i2), i, z);
                int iPixel3 = pixel(iArr, i4, clamp(i9 + 3 + 1, i2), i, z);
                i5 += ((iPixel3 >> 16) & 255) - ((iPixel2 >> 16) & 255);
                i6 += ((iPixel3 >> 8) & 255) - ((iPixel2 >> 8) & 255);
                i7 += (iPixel3 & 255) - (iPixel2 & 255);
            }
        }
    }

    private static int clamp(int i, int i2) {
        return Math.max(0, Math.min(i2 - 1, i));
    }

    private static int pixel(int[] iArr, int i, int i2, int i3, boolean z) {
        return iArr[z ? (i * i3) + i2 : i + (i2 * i3)];
    }

    private BlurPixels() {
    }
}
