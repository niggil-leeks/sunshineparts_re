package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class RuntimeProfile {
    private static boolean captured;
    private static boolean recovery = true;

    static synchronized void capture(boolean z) {
        if (!captured) {
            recovery = z;
            captured = true;
        }
    }

    static boolean recovery() {
        return recovery;
    }

    private RuntimeProfile() {
    }
}
