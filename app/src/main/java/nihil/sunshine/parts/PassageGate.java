package nihil.sunshine.parts;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: loaded from: classes.dex */
public final class PassageGate {
    public static final String DIR = "/data/adb/sunshineparts-access";
    public static final String KEY_ACCESS = "persist.sys.sunshineparts.access";
    public static final String PASSAGE_FILE = "/data/adb/sunshineparts-access/passage";
    public static final String STATE_FILE = "/data/adb/sunshineparts-access/state";
    private static final byte[] SEED = {83, 117, 110, 80, 97, 114, 116, 115, 45, 86, 51, 45, 82, 101, 116, 114};
    private static volatile Boolean cachedPassage = null;

    public static String sha256(String str) {
        if (str == null) {
            return "";
        }
        try {
            byte[] bArrDigest = MessageDigest.getInstance("SHA-256").digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bArrDigest.length * 2);
            for (byte b : bArrDigest) {
                sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String romHash() {
        String strSha256 = sha256(Config.property("ro.build.display.id") + "|" + Config.property("ro.build.version.oneui") + "|" + Config.property("ro.build.version.incremental"));
        return strSha256.length() >= 12 ? strSha256.substring(0, 12) : strSha256;
    }

    private static String sign(String str) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SEED, "HmacSHA256"));
            byte[] bArrDoFinal = mac.doFinal(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(bArrDoFinal[i] & 255)));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String generateCompactToken(int i) {
        String str = "PASS:" + i + ":" + romHash();
        return str + ":" + sign(str);
    }

    public static boolean validateToken(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        String strTrim = str.trim();
        if (!strTrim.startsWith("PASS:")) {
            return false;
        }
        String[] strArrSplit = strTrim.split(":", -1);
        if (strArrSplit.length != 4) {
            return false;
        }
        try {
            if (Integer.parseInt(strArrSplit[1]) != 23103 || !romHash().equalsIgnoreCase(strArrSplit[2])) {
                return false;
            }
            return MessageDigest.isEqual(sign(strArrSplit[0] + ":" + strArrSplit[1] + ":" + strArrSplit[2]).getBytes(StandardCharsets.UTF_8), strArrSplit[3].getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasValidPassage(Context context) {
        if (cachedPassage != null && cachedPassage.booleanValue()) {
            return true;
        }
        if (validateToken(Config.property(KEY_ACCESS))) {
            cachedPassage = true;
            return true;
        }
        if (context != null) {
            try {
                Object objInvoke = context.getClass().getMethod("getSharedPreferences", String.class, Integer.TYPE).invoke(context, "sunshine_passage_store", 0);
                if (objInvoke != null && validateToken((String) objInvoke.getClass().getMethod("getString", String.class, String.class).invoke(objInvoke, "passage_token", ""))) {
                    cachedPassage = true;
                    return true;
                }
            } catch (Throwable th) {
            }
        }
        return false;
    }

    public static boolean hasValidPassage(Context context, RootOps rootOps) {
        if (hasValidPassage(context)) {
            return true;
        }
        if (rootOps == null) {
            return false;
        }
        try {
            RootOps.Result resultRun = rootOps.run("if [ -f /data/adb/sunshineparts-access/passage ]; then cat /data/adb/sunshineparts-access/passage; else printf 'ABSENT'; fi", 4);
            if (resultRun.code == 0 && !resultRun.text.equals("ABSENT") && validateToken(resultRun.text.trim())) {
                cachedPassage = true;
                return true;
            }
        } catch (Throwable th) {
        }
        return false;
    }

    public static void grantPassage(Context context, RootOps rootOps, String str) throws Exception {
        String strGenerateCompactToken = generateCompactToken(Config.VERSION_CODE);
        if (context != null) {
            try {
                Object objInvoke = context.getClass().getMethod("getSharedPreferences", String.class, Integer.TYPE).invoke(context, "sunshine_passage_store", 0);
                if (objInvoke != null) {
                    Object objInvoke2 = objInvoke.getClass().getMethod("edit", new Class[0]).invoke(objInvoke, new Object[0]);
                    objInvoke2.getClass().getMethod("putString", String.class, String.class).invoke(objInvoke2, "passage_token", strGenerateCompactToken);
                    objInvoke2.getClass().getMethod("commit", new Class[0]).invoke(objInvoke2, new Object[0]);
                }
            } catch (Throwable th) {
            }
        }
        if (rootOps != null) {
            rootOps.run("umask 077\nmkdir -p /data/adb/sunshineparts-access\nchmod 700 /data/adb/sunshineparts-access\nprintf '%s' " + FlightCommands.quote(strGenerateCompactToken) + " > " + PASSAGE_FILE + ".tmp\nchmod 600 " + PASSAGE_FILE + ".tmp\nmv " + PASSAGE_FILE + ".tmp " + PASSAGE_FILE + "\nif command -v resetprop >/dev/null 2>&1; then resetprop -p " + FlightCommands.quote(KEY_ACCESS) + " " + FlightCommands.quote(strGenerateCompactToken) + "; else setprop " + FlightCommands.quote(KEY_ACCESS) + " " + FlightCommands.quote(strGenerateCompactToken) + "; fi\n", 5).require();
        }
        cachedPassage = true;
    }

    public static void invalidate() {
        cachedPassage = false;
    }

    private PassageGate() {
    }
}
