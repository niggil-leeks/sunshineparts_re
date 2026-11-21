package nihil.sunshine.parts;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class UiNumbers {
    static String decimal(float f, Locale locale) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            return "—";
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale));
        decimalFormat.setGroupingUsed(false);
        return decimalFormat.format(f);
    }

    private UiNumbers() {
    }
}
