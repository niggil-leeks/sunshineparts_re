package nihil.sunshine.parts;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;

/* JADX INFO: loaded from: classes.dex */
final class CareProgress extends View {
    static final int BROOM = 5;
    static final int DONE = 2;
    static final int ERROR = 3;
    static final int SKIPPED = 4;
    static final int WAITING = 0;
    static final int WORKING = 1;
    private ValueAnimator animator;
    private boolean attached;
    private final Path broom;
    private final Path check;
    private final Paint paint;
    private float phase;
    private int stage;
    private final int tone;

    CareProgress(Context context, int i, int i2) {
        super(context);
        this.paint = new Paint(WORKING);
        this.broom = new Path();
        this.check = new Path();
        this.stage = i;
        this.tone = i2;
        this.check.moveTo(4.0f, 12.0f);
        this.check.lineTo(9.0f, 17.0f);
        this.check.lineTo(20.0f, 6.0f);
        this.broom.moveTo(5.0f, 13.0f);
        this.broom.lineTo(19.0f, 13.0f);
        this.broom.lineTo(21.0f, 22.0f);
        this.broom.lineTo(3.0f, 22.0f);
        this.broom.close();
        setImportantForAccessibility(DONE);
    }

    void stage(int i) {
        this.stage = i;
        animateIfVisible();
        invalidate();
    }

    /* JADX WARN: Code duplicated, block: B:15:0x0021  */
    private void animateIfVisible() {
        boolean z;
        if (this.attached && getWindowVisibility() == 0 && isShown()) {
            z = (this.stage == WORKING || this.stage == BROOM) && ValueAnimator.areAnimatorsEnabled();
        }
        if (!z) {
            if (this.animator != null) {
                this.animator.cancel();
                this.animator = null;
                return;
            }
            return;
        }
        if (this.animator != null) {
            return;
        }
        this.animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.animator.setDuration(1400L);
        this.animator.setRepeatCount(-1);
        this.animator.setInterpolator(new LinearInterpolator());
        this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: nihil.sunshine.parts.CareProgress$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.m2lambda$animateIfVisible$0$nihilsunshinepartsCareProgress(valueAnimator);
            }
        });
        this.animator.start();
    }

    /* JADX INFO: renamed from: lambda$animateIfVisible$0$nihil-sunshine-parts-CareProgress, reason: not valid java name */
    /* synthetic */ void m2lambda$animateIfVisible$0$nihilsunshinepartsCareProgress(ValueAnimator valueAnimator) {
        this.phase = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.attached = true;
        animateIfVisible();
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        this.attached = false;
        animateIfVisible();
        super.onDetachedFromWindow();
    }

    @Override // android.view.View
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        animateIfVisible();
    }

    @Override // android.view.View
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        animateIfVisible();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int iSave = canvas.save();
        canvas.scale(getWidth() / 24.0f, getHeight() / 24.0f);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(this.tone);
        if (this.stage == BROOM) {
            canvas.scale(0.82f, 0.82f, 12.0f, 12.0f);
            canvas.rotate(this.phase * 360.0f, 12.0f, 12.0f);
            canvas.drawRoundRect(10.5f, 1.0f, 13.5f, 12.0f, 1.5f, 1.5f, this.paint);
            canvas.drawRoundRect(5.0f, 10.0f, 19.0f, 14.0f, 2.0f, 2.0f, this.paint);
            canvas.drawPath(this.broom, this.paint);
            this.paint.setColor(CareArt.blend(this.tone, -16777216, 0.24f));
            this.paint.setStrokeWidth(1.2f);
            canvas.drawLine(8.0f, 16.0f, 7.0f, 21.0f, this.paint);
            canvas.drawLine(12.0f, 16.0f, 12.0f, 21.0f, this.paint);
            canvas.drawLine(16.0f, 16.0f, 17.0f, 21.0f, this.paint);
        } else if (this.stage == DONE) {
            this.paint.setColor(-14638741);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(2.0f);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(this.check, this.paint);
        } else if (this.stage == WORKING) {
            double d = ((double) this.phase) * 3.141592653589793d * 2.0d;
            float fCos = ((float) Math.cos(d)) * 4.4f;
            float fSin = ((float) Math.sin(d)) * 2.2f;
            this.paint.setColor(-13064491);
            canvas.drawCircle(fCos + 12.0f, fSin + 12.0f, 2.4f, this.paint);
            this.paint.setColor(-14567782);
            canvas.drawCircle(12.0f - fCos, 12.0f - fSin, 2.4f, this.paint);
        } else if (this.stage == ERROR) {
            this.paint.setColor(-21166);
            canvas.drawRoundRect(11.0f, 5.0f, 13.0f, 14.0f, 1.0f, 1.0f, this.paint);
            canvas.drawCircle(12.0f, 18.0f, 1.2f, this.paint);
        } else if (this.stage == SKIPPED) {
            this.paint.setStrokeWidth(1.5f);
            canvas.drawLine(7.0f, 12.0f, 17.0f, 12.0f, this.paint);
        } else {
            this.paint.setAlpha(80);
            canvas.drawCircle(9.0f, 12.0f, 1.5f, this.paint);
            canvas.drawCircle(15.0f, 12.0f, 1.5f, this.paint);
        }
        canvas.restoreToCount(iSave);
    }
}
