package nihil.lain.sunshine;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* JADX INFO: loaded from: classes2.dex */
final class ApkContract {
    ApkContract() {
    }

    static boolean matchesBlur(String str) {
        if (str == null) {
            return false;
        }
        try {
            ZipFile zipFile = new ZipFile(str);
            try {
                ZipEntry entry = zipFile.getEntry("classes2.dex");
                boolean z = entry != null && entry.getSize() == 11129728 && entry.getCrc() == 1435496645;
                zipFile.close();
                return z;
            } catch (Throwable th) {
                try {
                    zipFile.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception unused) {
            return false;
        }
    }

    static boolean matchesQs(String str) {
        if (str == null) {
            return false;
        }
        try {
            ZipFile zipFile = new ZipFile(str);
            try {
                ZipEntry entry = zipFile.getEntry("classes3.dex");
                boolean z = entry != null && entry.getSize() == 11600804 && entry.getCrc() == 1120523985;
                zipFile.close();
                return z;
            } catch (Throwable th) {
                try {
                    zipFile.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception unused) {
            return false;
        }
    }

    static boolean matches(String str) {
        if (str == null) {
            return false;
        }
        try {
            ZipFile zipFile = new ZipFile(str);
            try {
                ZipEntry entry = zipFile.getEntry("classes4.dex");
                boolean z = entry != null && entry.getSize() == 10167584 && entry.getCrc() == 2810694790L;
                zipFile.close();
                return z;
            } catch (Throwable th) {
                try {
                    zipFile.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception unused) {
            return false;
        }
    }
}
