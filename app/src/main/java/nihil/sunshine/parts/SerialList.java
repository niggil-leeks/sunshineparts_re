package nihil.sunshine.parts;

import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class SerialList {
    static String normalize(String str) {
        if (str == null) {
            return "";
        }
        String upperCase = str.trim().toUpperCase(Locale.ROOT);
        return (!upperCase.matches("[A-Z0-9]{6,64}") || upperCase.equals("UNKNOWN")) ? "" : upperCase;
    }

    static boolean contains(String str, String str2) {
        String strNormalize = normalize(str2);
        if (strNormalize.isEmpty() || str == null || str.length() > 1048576) {
            return false;
        }
        if (str.startsWith("\ufeff")) {
            str = str.substring(1);
        }
        for (String str3 : str.split("\\r?\\n", -1)) {
            if (!str3.trim().isEmpty() && strNormalize.equals(normalize(str3))) {
                return true;
            }
        }
        return false;
    }

    private SerialList() {
    }
}
