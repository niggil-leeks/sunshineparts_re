package nihil.sunshine.parts;

import android.content.Context;
import android.widget.Button;
import android.widget.TextView;

/* JADX INFO: loaded from: classes.dex */
final class LocalizedButton extends Button {
    LocalizedButton(Context context) {
        super(context);
    }

    @Override // android.widget.TextView
    public void setText(CharSequence charSequence, TextView.BufferType bufferType) {
        if (charSequence instanceof String) {
            charSequence = I18n.text(getContext(), (String) charSequence);
        }
        super.setText(charSequence, bufferType);
    }
}
