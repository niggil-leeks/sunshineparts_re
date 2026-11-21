package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class ActionFeedback {
    static String progressKey(String str) {
        if ("Connection".equals(str)) {
            return "Checking module connection…";
        }
        if ("Diagnostic archive".equals(str)) {
            return "Collecting diagnostic information…";
        }
        if ("UI shader caches".equals(str)) {
            return "Backing up shader caches…";
        }
        if ("Memory".equals(str)) {
            return "Releasing eligible cached apps…";
        }
        if ("Restore settings".equals(str)) {
            return "Restoring saved settings…";
        }
        if ("Restart SystemUI".equals(str)) {
            return "Restarting the system interface…";
        }
        return "Recording: pull QS now".equals(str) ? "Starting diagnostic recording…" : "";
    }

    static String subject(String str) {
        return "Reduce diagnostic logging".equals(str) ? "Diagnostic logging reduction" : str;
    }

    static String progressSubject(String str) {
        if ("Prefer 120 Hz".equals(str)) {
            return "120 Hz preference";
        }
        return "Keep panel content ready".equals(str) ? "Prepared panel content" : subject(str);
    }

    static String toggleResultKey(String str, boolean z) {
        return subject(str) + (z ? " enabled." : " disabled.");
    }

    static boolean setting(String str) {
        return !"Recovery mode".equals(str) && progressKey(str).isEmpty();
    }

    static String resultKey(String str) {
        if ("Processing speed".equals(str)) {
            return "Processing speed updated.";
        }
        if ("CPU power profile".equals(str)) {
            return "CPU power profile updated.";
        }
        if ("Visual workload".equals(str)) {
            return "Visual workload updated.";
        }
        if ("Home screen animations".equals(str)) {
            return "Home screen animations updated.";
        }
        if ("Media wave detail".equals(str)) {
            return "Media wave detail updated.";
        }
        if ("Panel animations".equals(str)) {
            return "Panel animations updated.";
        }
        if ("Panel blur".equals(str)) {
            return "Panel blur updated.";
        }
        if ("Prepared panel blur".equals(str)) {
            return "Prepared blur updated.";
        }
        if ("Power saving".equals(str)) {
            return "Power saving updated.";
        }
        if ("Adaptive battery".equals(str)) {
            return "Adaptive battery updated.";
        }
        if ("Smart RAM management".equals(str)) {
            return "Memory management updated.";
        }
        if ("Diagnostic logging".equals(str)) {
            return "Diagnostic logging updated.";
        }
        if ("Quick settings updates".equals(str)) {
            return "Quick settings updated.";
        }
        if ("Tile layout".equals(str)) {
            return "Tile layout updated.";
        }
        if ("Tile updates".equals(str)) {
            return "Tile updates configured.";
        }
        return "Muted light surfaces".equals(str) ? "Notification colors updated." : "";
    }

    static boolean generic(String str) {
        return str == null || str.trim().isEmpty() || "Applied".equals(str) || "Applied.".equals(str);
    }

    static String punctuate(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return (!strTrim.isEmpty() && ".!?…:".indexOf(strTrim.charAt(strTrim.length() + (-1))) < 0) ? strTrim + "." : strTrim;
    }

    private ActionFeedback() {
    }
}
