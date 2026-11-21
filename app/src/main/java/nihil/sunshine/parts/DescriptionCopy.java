package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class DescriptionCopy {
    private static final String[] STARTS = {"Check ", "View ", "Show ", "Use ", "Choose ", "Prepare ", "Reduce ", "Release ", "Combine ", "Reuse ", "Keep ", "Avoid ", "Move ", "Record ", "Restart ", "Restore ", "Limit ", "Prefer ", "Monitor "};

    static String summary(String str, String str2) {
        if (str == null || str.isEmpty() || str2 == null || "Restart needed".equals(str2)) {
            return str;
        }
        boolean z = false;
        for (String str3 : STARTS) {
            if (str2.startsWith(str3)) {
                z = true;
                break;
            }
        }
        return z ? ActionFeedback.punctuate(str) : str;
    }

    private DescriptionCopy() {
    }
}
