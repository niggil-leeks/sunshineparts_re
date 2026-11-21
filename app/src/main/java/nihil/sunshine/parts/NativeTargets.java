package nihil.sunshine.parts;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* JADX INFO: loaded from: classes.dex */
public final class NativeTargets {
    public static boolean systemUi(String str) {
        return match(str, new long[]{66324994, 737421251, 3115353673L, 2527315379L}, new long[]{10274056, 10759160, 10805128, 6120484});
    }

    public static boolean launcher(String str) {
        return match(str, new long[]{3103179694L, 1285778213, 1152908404, 806500879}, new long[]{9178472, 9710528, 8599720, 8880472});
    }

    private static boolean match(String str, long[] jArr, long[] jArr2) {
        if (str == null) {
            return false;
        }
        try {
            ZipFile zipFile = new ZipFile(str);
            int i = 0;
            while (i < jArr.length) {
                try {
                    ZipEntry entry = zipFile.getEntry(i == 0 ? "classes.dex" : "classes" + (i + 1) + ".dex");
                    if (entry == null || entry.getCrc() != jArr[i] || entry.getSize() != jArr2[i]) {
                        zipFile.close();
                        return false;
                    }
                    i++;
                } catch (Throwable th) {
                    try {
                        zipFile.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            }
            boolean z = zipFile.getEntry(new StringBuilder().append("classes").append(jArr.length + 1).append(".dex").toString()) == null;
            zipFile.close();
            return z;
        } catch (Exception e) {
            return false;
        }
    }

    private NativeTargets() {
    }
}
