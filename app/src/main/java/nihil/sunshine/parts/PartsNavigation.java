package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class PartsNavigation {
    static int parent(int i) {
        if (i == 7 || i == 8 || i == 10) {
            return 2;
        }
        if (i == 13) {
            return 4;
        }
        if (i == 4) {
            return 9;
        }
        if (i == 11) {
            return 5;
        }
        if (i == 1) {
            return 9;
        }
        return 0;
    }

    private PartsNavigation() {
    }
}
