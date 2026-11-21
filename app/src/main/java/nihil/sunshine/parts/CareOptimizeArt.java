package nihil.sunshine.parts;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.Choreographer;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
final class CareOptimizeArt extends View {
    private static final int COUNT = 20;
    private final int accent;
    private boolean attached;
    private final Path check;
    private boolean complete;
    private final int disc;
    private boolean failed;
    private float finish;
    private float formed;
    private final Choreographer.FrameCallback frame;
    private final float[] fromX;
    private final float[] fromY;
    private long last;
    private final Paint paint;
    private boolean scheduled;
    private float target;
    private Shader texture;
    private final float[] toX;
    private final float[] toY;
    private float wave;

    CareOptimizeArt(Context context, int i, int i2, boolean z) {
        super(context);
        this.paint = new Paint(1);
        this.check = new Path();
        this.fromX = new float[COUNT];
        this.fromY = new float[COUNT];
        this.toX = new float[COUNT];
        this.toY = new float[COUNT];
        this.frame = new Choreographer.FrameCallback() { // from class: nihil.sunshine.parts.CareOptimizeArt.1
            @Override // android.view.Choreographer.FrameCallback
            public void doFrame(long j) {
                CareOptimizeArt.this.scheduled = false;
                if (!CareOptimizeArt.this.running()) {
                    CareOptimizeArt.this.wake();
                    return;
                }
                float fMin = CareOptimizeArt.this.last == 0 ? 0.0f : Math.min(0.08f, Math.max(0L, j - CareOptimizeArt.this.last) / 1.0E9f);
                CareOptimizeArt.this.last = j;
                CareOptimizeArt.this.formed = Math.min(CareOptimizeArt.this.target, CareOptimizeArt.this.formed + (0.48f * fMin));
                if (CareOptimizeArt.this.complete && (CareOptimizeArt.this.failed || CareOptimizeArt.this.formed >= 0.999f)) {
                    CareOptimizeArt.this.finish = Math.min(1.0f, CareOptimizeArt.this.finish + (fMin * 1.8f));
                }
                CareOptimizeArt.this.wave = (j % 1800000000) / 1.8E9f;
                CareOptimizeArt.this.invalidate();
                if (CareOptimizeArt.this.running()) {
                    CareOptimizeArt.this.scheduled = true;
                    Choreographer.getInstance().postFrameCallback(this);
                } else {
                    CareOptimizeArt.this.setRequestedFrameRate(0.0f);
                }
            }
        };
        this.accent = i;
        this.disc = CareArt.blend(i2, -1, z ? 0.08f : 0.62f);
        for (int i3 = 0; i3 < COUNT; i3++) {
            double d = (((double) i3) * 2.39996323d) + 0.45d;
            float f = ((i3 % 3) * 3) + 53;
            this.fromX[i3] = (((float) Math.cos(d)) * f) + 64.0f;
            this.fromY[i3] = (((float) Math.sin(d)) * f) + 64.0f;
            float f2 = i3 / 19.0f;
            if (f2 < 0.35f) {
                float f3 = (f2 / 0.35f) * 14.0f;
                this.toX[i3] = 43.0f + f3;
                this.toY[i3] = f3 + 64.0f;
            } else {
                float f4 = (f2 - 0.35f) / 0.65f;
                this.toX[i3] = (29.0f * f4) + 57.0f;
                this.toY[i3] = 78.0f - (f4 * 30.0f);
            }
        }
        setImportantForAccessibility(2);
    }

    void progress(float f) {
        this.target = Math.max(this.target, Math.min(1.0f, f));
        wake();
    }

    void complete(boolean z) {
        this.complete = true;
        this.failed = false;
        this.target = 1.0f;
        wake();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean running() {
        return this.attached && isShown() && getWindowVisibility() == 0 && ValueAnimator.areAnimatorsEnabled() && (!this.complete || this.finish < 1.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void wake() {
        if (this.frame == null) {
            return;
        }
        if (running()) {
            setRequestedFrameRate(120.0f);
            if (!this.scheduled) {
                this.last = 0L;
                this.scheduled = true;
                Choreographer.getInstance().postFrameCallback(this.frame);
                return;
            }
            return;
        }
        Choreographer.getInstance().removeFrameCallback(this.frame);
        this.scheduled = false;
        this.last = 0L;
        setRequestedFrameRate(0.0f);
        this.formed = this.target;
        this.finish = this.complete ? 1.0f : 0.0f;
        invalidate();
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.attached = true;
        wake();
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        this.attached = false;
        wake();
        super.onDetachedFromWindow();
    }

    @Override // android.view.View
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        wake();
    }

    @Override // android.view.View
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        wake();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.texture = new LinearGradient(42.0f, 48.0f, 87.0f, 79.0f, CareArt.blend(this.accent, -1, 0.23f), this.accent, Shader.TileMode.CLAMP);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int iSave = canvas.save();
        float fMin = Math.min(getWidth(), getHeight()) / 128.0f;
        float f = 128.0f * fMin;
        canvas.translate((getWidth() - f) / 2.0f, (getHeight() - f) / 2.0f);
        canvas.scale(fMin, fMin);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setShader(null);
        this.paint.setColor(this.accent);
        this.paint.setAlpha(9);
        canvas.drawCircle(64.0f, 64.0f, (this.finish * 3.0f) + 51.0f, this.paint);
        this.paint.setAlpha(16);
        canvas.drawCircle(64.0f, 64.0f, (this.finish * 2.0f) + 46.0f, this.paint);
        this.paint.setColor(this.disc);
        canvas.drawCircle(64.0f, 64.0f, 42.0f, this.paint);
        this.paint.setColor(this.accent);
        this.paint.setAlpha(28);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(0.65f);
        canvas.drawCircle(64.0f, 64.0f, 42.0f, this.paint);
        this.check.reset();
        float f2 = this.formed * 19.0f;
        int i = (int) f2;
        if (i > 0) {
            this.check.moveTo(this.toX[0], this.toY[0]);
            for (int i2 = 1; i2 <= i; i2++) {
                this.check.lineTo(this.toX[i2], this.toY[i2]);
            }
            if (i < 19) {
                float f3 = f2 - i;
                int i3 = i + 1;
                this.check.lineTo(this.toX[i] + ((this.toX[i3] - this.toX[i]) * f3), this.toY[i] + ((this.toY[i3] - this.toY[i]) * f3));
            }
        }
        this.paint.setShader(this.texture);
        this.paint.setAlpha(this.failed ? 80 : Math.round((this.finish * 175.0f) + 80.0f));
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(5.8f);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(this.check, this.paint);
        this.paint.setShader(null);
        this.paint.setStyle(Paint.Style.FILL);
        for (int i4 = 0; i4 < COUNT; i4++) {
            float f4 = i4;
            float f5 = f4 / 20.0f;
            if (f5 < this.formed) {
                if (this.finish < 1.0f) {
                    this.paint.setColor(this.accent);
                    this.paint.setAlpha(Math.round((1.0f - this.finish) * 135.0f));
                    canvas.drawCircle(this.toX[i4], this.toY[i4], 1.3f, this.paint);
                }
            } else if (!this.complete && f5 <= this.target + 0.16f) {
                float f6 = (this.wave + (f4 * 0.137f)) % 1.0f;
                float f7 = f6 * f6 * (3.0f - (f6 * 2.0f));
                double d = ((double) f6) * 3.141592653589793d;
                float fSin = this.fromX[i4] + ((this.toX[i4] - this.fromX[i4]) * f7) + (((float) Math.sin(d)) * 5.0f * (i4 % 2 == 0 ? 1 : -1));
                float f8 = this.fromY[i4] + ((this.toY[i4] - this.fromY[i4]) * f7);
                this.paint.setColor(this.accent);
                this.paint.setAlpha(Math.round(((float) Math.sin(d)) * 150.0f));
                canvas.drawCircle(fSin, f8, ((i4 % 3) * 0.35f) + 1.1f, this.paint);
            }
        }
        canvas.restoreToCount(iSave);
    }
}
