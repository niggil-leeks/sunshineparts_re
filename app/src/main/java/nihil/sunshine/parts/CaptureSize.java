package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class CaptureSize {
    static boolean reduce(Object[] objArr, int i, int i2) {
        if (objArr == null || objArr.length != 9 || !(objArr[4] instanceof Integer) || !(objArr[5] instanceof Integer)) {
            return false;
        }
        int iIntValue = ((Integer) objArr[4]).intValue();
        int iIntValue2 = ((Integer) objArr[5]).intValue();
        if (iIntValue <= 0 || iIntValue2 <= 0) {
            return false;
        }
        int[] size = BlurPlan.size(iIntValue, iIntValue2, i, i2);
        if (size[0] == iIntValue && size[1] == iIntValue2) {
            return false;
        }
        objArr[4] = Integer.valueOf(size[0]);
        objArr[5] = Integer.valueOf(size[1]);
        return true;
    }

    private CaptureSize() {
    }
}
