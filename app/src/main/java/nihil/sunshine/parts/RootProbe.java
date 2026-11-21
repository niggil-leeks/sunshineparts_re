package nihil.sunshine.parts;

import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
final class RootProbe {
    static final String SCRIPT = "if [ \"$(id -u)\" = 0 ]; then echo SUNSHINE_ROOT_OK; else exit 13; fi";

    static boolean granted(int i, String str) {
        return i == 0 && str != null && Arrays.asList(str.trim().split("\\r?\\n")).contains("SUNSHINE_ROOT_OK");
    }

    private RootProbe() {
    }
}
