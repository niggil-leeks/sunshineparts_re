package nihil.sunshine.parts;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.widget.Switch;

/* JADX INFO: loaded from: classes.dex */
final class CareSwitch extends Switch {
    /* JADX INFO: Access modifiers changed from: private */
    public int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    CareSwitch(Context context, int i, int i2, int i3) {
        super(context);
        setShowText(false);
        setSplitTrack(false);
        setSwitchMinWidth(dp(32));
        setSwitchPadding(0);
        boolean z = (getResources().getConfiguration().uiMode & 48) == 32;
        try {
            i = context.getColor(android.R.color.system_accent1_400);
        } catch (Exception e) {
        }
        GradientDrawable gradientDrawable = new GradientDrawable() { // from class: nihil.sunshine.parts.CareSwitch.1
            @Override // android.graphics.drawable.GradientDrawable, android.graphics.drawable.Drawable
            public boolean getPadding(Rect rect) {
                rect.set(CareSwitch.this.dp(4), CareSwitch.this.dp(2), CareSwitch.this.dp(4), CareSwitch.this.dp(2));
                return true;
            }
        };
        gradientDrawable.setCornerRadius(dp(10));
        gradientDrawable.setSize(dp(32), dp(20));
        gradientDrawable.setColor(-1);
        setTrackDrawable(gradientDrawable);
        setTrackTintList(new ColorStateList(new int[][]{new int[]{-16842910}, new int[]{android.R.attr.state_checked}, new int[0]}, new int[]{z ? -12434873 : -2697510, i, z ? -10329497 : -4671299}));
        GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setShape(1);
        gradientDrawable2.setSize(dp(16), dp(16));
        gradientDrawable2.setColor(-1);
        setThumbDrawable(new InsetDrawable(gradientDrawable2, 0, dp(2), 0, dp(2)) { // from class: nihil.sunshine.parts.CareSwitch.2
            @Override // android.graphics.drawable.InsetDrawable, android.graphics.drawable.DrawableWrapper, android.graphics.drawable.Drawable
            public boolean getPadding(Rect rect) {
                rect.set(CareSwitch.this.dp(2), CareSwitch.this.dp(2), CareSwitch.this.dp(2), CareSwitch.this.dp(2));
                return true;
            }
        });
        setThumbTintList(ColorStateList.valueOf(-1));
        setMinimumHeight(dp(48));
        setMinimumWidth(dp(48));
    }
}
