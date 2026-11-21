package nihil.sunshine.parts;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class MessageCatalog {
    private final Locale locale;
    private final Map<String, String> messages = new HashMap();
    private static final Pattern USED = Pattern.compile("^([0-9]{1,3})% used$");
    private static final Pattern AGO = Pattern.compile("^([0-9]{1,4})s ago$");
    private static final String[] SUFFIXES = {" GB available", " GB total memory", " visual workload", " ready"};
    private static final String[] PREFIXES = {"Limit: ", "Effective limit: ", "Saved to ", "Native display settings: min "};

    MessageCatalog(String[] strArr, String[] strArr2, Locale locale) {
        if (strArr.length != strArr2.length) {
            throw new IllegalArgumentException("Catalog length");
        }
        this.locale = locale;
        for (int i = 0; i < strArr.length; i++) {
            if (this.messages.put(strArr[i], strArr2[i]) != null) {
                throw new IllegalArgumentException("Duplicate message");
            }
        }
    }

    String format(String str, Object... objArr) {
        Locale locale = this.locale;
        if (this.messages.containsKey(str)) {
            str = this.messages.get(str);
        }
        return String.format(locale, str, objArr);
    }

    String text(String str) {
        if (str == null) {
            return "";
        }
        String str2 = this.messages.get(str);
        if (str2 != null) {
            return str2;
        }
        Matcher matcher = USED.matcher(str);
        if (matcher.matches()) {
            return format("%1$d%% used", Integer.valueOf(Integer.parseInt(matcher.group(1))));
        }
        Matcher matcher2 = AGO.matcher(str);
        if (matcher2.matches()) {
            return format("%ds ago", Integer.valueOf(Integer.parseInt(matcher2.group(1))));
        }
        for (String str3 : SUFFIXES) {
            if (str.endsWith(str3)) {
                return format("%1$s" + str3, text(str.substring(0, str.length() - str3.length())));
            }
        }
        for (String str4 : PREFIXES) {
            if (str.startsWith(str4)) {
                return this.messages.getOrDefault(str4, str4) + str.substring(str4.length());
            }
        }
        if (str.endsWith("…")) {
            String str5 = this.messages.get(str.substring(0, str.length() - 1));
            if (str5 != null) {
                return str5 + "…";
            }
        }
        return str;
    }
}
