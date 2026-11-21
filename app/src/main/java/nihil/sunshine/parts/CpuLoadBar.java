package nihil.sunshine.parts;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/* JADX INFO: loaded from: classes.dex */
final class CpuLoadBar extends View {
    private final int accent;
    private ValueAnimator animator;
    private float current;
    private boolean initialized;
    private final Paint paint;
    private float target;
    private final int track;

    CpuLoadBar(Context context, int i, int i2) {
        super(context);
        this.paint = new Paint(1);
        this.accent = i;
        this.track = i2;
        setImportantForAccessibility(2);
    }

    void reading(double d, boolean z) {
        if (!z || !Double.isFinite(d)) {
            stop();
            this.initialized = false;
            this.target = 0.0f;
            this.current = 0.0f;
            invalidate();
            return;
        }
        float fMax = (float) Math.max(0.0d, Math.min(1.0d, d / 100.0d));
        if (this.initialized && fMax == this.target) {
            return;
        }
        this.target = fMax;
        stop();
        if (!this.initialized || !isAttachedToWindow() || !isShown() || getWindowVisibility() != 0 || !ValueAnimator.areAnimatorsEnabled()) {
            this.initialized = true;
            this.current = this.target;
            invalidate();
        } else {
            this.animator = ValueAnimator.ofFloat(this.current, this.target);
            this.animator.setDuration(180L);
            this.animator.setInterpolator(new DecelerateInterpolator());
            this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: nihil.sunshine.parts.CpuLoadBar$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    this.f$0.m3lambda$reading$0$nihilsunshinepartsCpuLoadBar(valueAnimator);
                }
            });
            this.animator.start();
        }
    }

    /* JADX INFO: renamed from: lambda$reading$0$nihil-sunshine-parts-CpuLoadBar, reason: not valid java name */
    /* synthetic */ void m3lambda$reading$0$nihilsunshinepartsCpuLoadBar(ValueAnimator valueAnimator) {
        this.current = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    private void stop() {
        if (this.animator != null) {
            this.animator.cancel();
            this.animator = null;
        }
    }

    private void settle() {
        stop();
        this.current = this.target;
        invalidate();
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        settle();
        super.onDetachedFromWindow();
    }

    @Override // android.view.View
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        if (i != 0) {
            settle();
        }
    }

    @Override // android.view.View
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (i != 0) {
            settle();
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float f = height / 2.0f;
        this.paint.setColor(this.track);
        canvas.drawRoundRect(0.0f, 0.0f, width, height, f, f, this.paint);
        if (this.current > 0.0f) {
            this.paint.setColor(this.accent);
            canvas.drawRoundRect(0.0f, 0.0f, Math.min(width, Math.max(height, this.current * width)), height, f, f, this.paint);
        }
    }
}
