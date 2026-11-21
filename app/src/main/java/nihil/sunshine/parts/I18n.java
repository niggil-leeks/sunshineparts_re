package nihil.sunshine.parts;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/* JADX INFO: loaded from: classes.dex */
final class I18n {
    private static MessageCatalog catalog;
    private static String language = "";

    private static synchronized MessageCatalog get(Context context) {
        Resources resources = context.getResources();
        String language2 = resources.getConfiguration().getLocales().get(0).getLanguage();
        if (catalog == null || !language2.equals(language)) {
            catalog = new MessageCatalog(resources.getStringArray(resources.getIdentifier("parts_source", "array", context.getPackageName())), resources.getStringArray(resources.getIdentifier("parts_translation", "array", context.getPackageName())), resources.getConfiguration().getLocales().get(0));
            language = language2;
        }
        return catalog;
    }

    static String text(Context context, String str) {
        return get(context).text(str).replace("SystemUI", "System UI");
    }

    static String format(Context context, String str, Object... objArr) {
        return get(context).format(str, objArr).replace("SystemUI", "System UI");
    }

    static String inline(Context context, String str) {
        String strText = text(context, str);
        return (strText.length() < 2 || Character.isUpperCase(strText.charAt(1))) ? strText : strText.substring(0, 1).toLowerCase(context.getResources().getConfiguration().getLocales().get(0)) + strText.substring(1);
    }

    static String[] labels(Context context, String[] strArr) {
        String[] strArr2 = (String[]) strArr.clone();
        for (int i = 0; i < strArr2.length; i++) {
            strArr2[i] = text(context, strArr2[i]);
        }
        return strArr2;
    }

    static void apply(Context context, View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CharSequence text = textView.getText();
            if (text instanceof String) {
                String strText = text(context, (String) text);
                if (!strText.contentEquals(text)) {
                    textView.setText(strText);
                }
            }
        }
        if (view.getContentDescription() != null) {
            view.setContentDescription(text(context, view.getContentDescription().toString()));
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                apply(context, viewGroup.getChildAt(i));
            }
        }
    }

    private I18n() {
    }
}
