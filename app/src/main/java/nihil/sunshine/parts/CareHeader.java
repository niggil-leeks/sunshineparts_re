package nihil.sunshine.parts;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ActionMenuView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;

/* JADX INFO: loaded from: classes.dex */
final class CareHeader extends FrameLayout {
    private ActionMenuView actions;
    private boolean actionsFloating;
    private ImageButton back;
    private boolean backFloating;
    private final Rect clip;
    final int collapsedHeight;
    private TextView compactTitle;
    final int expandedHeight;
    private final TextView expandedTitle;
    private final int floatingColor;
    private final boolean home;
    private Drawable originalActions;
    private Drawable originalBack;
    private int scroll;
    private final String title;
    private final Toolbar toolbar;
    private final int[] touchLocation;

    private int dp(float f) {
        return Math.round(f * getResources().getDisplayMetrics().density);
    }

    CareHeader(Context context, Toolbar toolbar, String str, int i, int i2, int i3) {
        super(context);
        this.clip = new Rect();
        this.touchLocation = new int[2];
        this.toolbar = toolbar;
        this.title = I18n.text(context, str);
        this.home = "Sunshine Parts".equals(str);
        this.floatingColor = i3;
        this.collapsedHeight = dp(64.0f);
        this.expandedHeight = dp(Math.max(180.0f, Math.min(320.0f, ((getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density) - 48.0f) * 0.4f)));
        setBackgroundColor(0);
        setClipChildren(true);
        this.toolbar.setBackgroundColor(0);
        this.expandedTitle = new TextView(context);
        this.expandedTitle.setText(str.equals("Sunshine Parts") ? "Sunshine\nParts" : this.title);
        this.expandedTitle.setTextSize(34.0f);
        this.expandedTitle.setTextColor(i2);
        this.expandedTitle.setTypeface(Typeface.create(Typeface.create("sec", 0), 600, false));
        this.expandedTitle.setGravity(17);
        this.expandedTitle.setMaxLines(2);
        this.expandedTitle.setIncludeFontPadding(false);
        this.expandedTitle.setPadding(dp(30.0f), dp(6.0f), dp(30.0f), dp(6.0f));
        addView(this.expandedTitle, new FrameLayout.LayoutParams(-1, this.expandedHeight - this.collapsedHeight));
        this.toolbar.setPadding(dp(12.0f), 0, dp(12.0f), 0);
        this.toolbar.setTitle(this.title);
        addView(this.toolbar, new FrameLayout.LayoutParams(-1, this.collapsedHeight));
        setImportantForAccessibility(2);
    }

    void scroll(int i) {
        this.scroll = Math.max(0, i);
        update();
    }

    private boolean hit(View view, MotionEvent motionEvent) {
        if (view == null || !view.isShown() || !view.isEnabled() || view.getAlpha() <= 0.0f) {
            return false;
        }
        view.getLocationOnScreen(this.touchLocation);
        return motionEvent.getRawX() >= ((float) this.touchLocation[0]) && motionEvent.getRawX() < ((float) (this.touchLocation[0] + view.getWidth())) && motionEvent.getRawY() >= ((float) this.touchLocation[1]) && motionEvent.getRawY() < ((float) (this.touchLocation[1] + view.getHeight()));
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            if (motionEvent.getX() < this.clip.left || motionEvent.getX() >= this.clip.right || motionEvent.getY() < this.clip.top || motionEvent.getY() >= this.clip.bottom) {
                return false;
            }
            if (!hit(this.back, motionEvent) && !hit(this.actions, motionEvent)) {
                return false;
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        for (int i5 = 0; i5 < this.toolbar.getChildCount(); i5++) {
            View childAt = this.toolbar.getChildAt(i5);
            if (this.compactTitle == null && (childAt instanceof TextView)) {
                TextView textView = (TextView) childAt;
                if (this.title.contentEquals(textView.getText())) {
                    this.compactTitle = textView;
                    this.compactTitle.setTypeface(Typeface.create(Typeface.create("sec", 0), 700, false));
                }
            }
            if ((childAt instanceof ImageButton) && this.back == null) {
                this.back = (ImageButton) childAt;
                this.originalBack = childAt.getBackground();
                this.back.setPadding(dp(16.0f), dp(16.0f), dp(16.0f), dp(16.0f));
                ViewGroup.LayoutParams layoutParams = this.back.getLayoutParams();
                layoutParams.width = dp(48.0f);
                layoutParams.height = dp(48.0f);
                this.back.setLayoutParams(layoutParams);
            }
            if ((childAt instanceof ActionMenuView) && this.actions == null) {
                this.actions = (ActionMenuView) childAt;
                this.originalActions = childAt.getBackground();
            }
        }
        update();
    }

    private Drawable plate(boolean z) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(this.floatingColor);
        gradientDrawable.setCornerRadius(dp(24.0f));
        return new RippleDrawable(ColorStateList.valueOf(587202559), gradientDrawable, null);
    }

    private void update() {
        int i = this.expandedHeight - this.collapsedHeight;
        int iMin = Math.min(this.scroll, i);
        int i2 = this.expandedHeight - iMin;
        float f = i == 0 ? 1.0f : iMin / i;
        this.toolbar.setTranslationY((i2 - this.collapsedHeight) + dp(8.0f));
        this.expandedTitle.setTranslationY((-iMin) * 0.3f);
        this.expandedTitle.setAlpha(i == 0 ? 0.0f : Math.max(0.0f, 1.0f - (1.7f * f)));
        if (this.compactTitle != null) {
            this.compactTitle.setAlpha(Math.max(0.0f, Math.min(1.0f, (f - 0.55f) / 0.45f)) * Math.max(0.0f, Math.min(1.0f, 1.0f - ((this.scroll - i) / dp(56.0f)))));
            this.compactTitle.setImportantForAccessibility((f <= 0.6f || this.scroll >= i + dp(56.0f)) ? 2 : 1);
        }
        this.expandedTitle.setImportantForAccessibility(f <= 0.6f ? 1 : 2);
        boolean z = f > 0.95f;
        if (this.back != null && this.backFloating != z) {
            this.backFloating = z;
            this.back.setBackground(z ? plate(true) : this.originalBack);
        }
        if (this.actions != null && this.actionsFloating != z) {
            this.actionsFloating = z;
            this.actions.setBackground(z ? plate(false) : this.originalActions);
        }
        this.clip.set(0, 0, getWidth(), i2);
        setClipBounds(this.clip);
    }
}
