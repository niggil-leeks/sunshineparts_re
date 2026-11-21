package nihil.sunshine.parts;

import android.content.Context;
import android.widget.ScrollView;

/* JADX INFO: loaded from: classes.dex */
final class CareScrollView extends ScrollView {
    private final int collapseRange;

    CareScrollView(Context context, int i) {
        super(context);
        this.collapseRange = i;
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
    }

    @Override // android.widget.ScrollView, android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int iMax;
        super.onMeasure(i, i2);
        if (getChildCount() <= 0 || getChildAt(0).getMinimumHeight() == (iMax = Math.max(0, ((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom()) + this.collapseRange))) {
            return;
        }
        getChildAt(0).setMinimumHeight(iMax);
        super.onMeasure(i, i2);
    }
}
