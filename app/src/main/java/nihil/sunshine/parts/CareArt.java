package nihil.sunshine.parts;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.PathInterpolator;

/* JADX INFO: loaded from: classes.dex */
final class CareArt {
    static final PathInterpolator ENTER = new PathInterpolator(0.117f, 0.126f, 0.312f, 1.0f);
    static final PathInterpolator SETTLE = new PathInterpolator(0.344f, 0.83f, 0.605f, 1.0f);

    static int blend(int i, int i2, float f) {
        float f2 = 1.0f - f;
        return Color.rgb(Math.round((Color.red(i) * f2) + (Color.red(i2) * f)), Math.round((Color.green(i) * f2) + (Color.green(i2) * f)), Math.round((Color.blue(i) * f2) + (Color.blue(i2) * f)));
    }

    static void enter(View view, boolean z) {
        if (ValueAnimator.areAnimatorsEnabled()) {
            view.setAlpha(0.0f);
            view.setTranslationX(view.getResources().getDisplayMetrics().density * (z ? -20 : 20));
            view.animate().alpha(1.0f).translationX(0.0f).setDuration(250L).setInterpolator(ENTER).start();
        }
    }

    static final class Ambient extends Drawable {
        final int base;
        float centerX;
        float centerY;
        Shader halo;
        float radius;
        final Paint paint = new Paint(1);
        final Matrix transform = new Matrix();
        int status = -12019201;

        Ambient(int i, int i2) {
            this.base = i;
        }

        void focus(float f, float f2, float f3) {
            if (this.centerX == f && this.centerY == f2 && this.radius == f3) {
                return;
            }
            this.centerX = f;
            this.centerY = f2;
            this.radius = f3;
            position();
        }

        private void rebuild() {
            this.halo = new RadialGradient(0.0f, 0.0f, 1.0f, new int[]{Color.argb(50, Color.red(this.status), Color.green(this.status), Color.blue(this.status)), 0}, (float[]) null, Shader.TileMode.CLAMP);
            position();
        }

        private void position() {
            if (this.halo == null) {
                rebuild();
                return;
            }
            this.transform.reset();
            this.transform.setScale(Math.max(1.0f, this.radius), Math.max(1.0f, this.radius));
            this.transform.postTranslate(this.centerX, this.centerY);
            this.halo.setLocalMatrix(this.transform);
            invalidateSelf();
        }

        void status(int i) {
            if (this.status != i) {
                this.status = i;
                rebuild();
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            canvas.drawColor(this.base);
            if (this.halo != null) {
                this.paint.setShader(this.halo);
                canvas.drawRect(getBounds(), this.paint);
                this.paint.setShader(null);
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -1;
        }
    }

    static final class HealthMaterial extends Drawable {
        Shader glow;
        final float radius;
        final int surface;
        final Paint paint = new Paint(1);
        final Path clip = new Path();
        final RectF rect = new RectF();
        int status = -12019201;

        HealthMaterial(int i, float f) {
            this.surface = i;
            this.radius = f;
        }

        @Override // android.graphics.drawable.Drawable
        protected void onBoundsChange(Rect rect) {
            this.rect.set(rect);
            this.clip.reset();
            this.clip.addRoundRect(this.rect, this.radius, this.radius, Path.Direction.CW);
            rebuild();
        }

        private void rebuild() {
            Rect bounds = getBounds();
            if (bounds.width() > 0) {
                this.glow = new RadialGradient(bounds.exactCenterX(), (bounds.height() * 0.72f) + bounds.top, bounds.width() * 0.58f, new int[]{Color.argb(24, Color.red(this.status), Color.green(this.status), Color.blue(this.status)), 0}, (float[]) null, Shader.TileMode.CLAMP);
            }
            invalidateSelf();
        }

        void status(int i) {
            if (this.status != i) {
                this.status = i;
                rebuild();
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            this.paint.setColor((this.surface & 16777215) | (-301989888));
            canvas.drawRoundRect(this.rect, this.radius, this.radius, this.paint);
            if (this.glow != null) {
                int iSave = canvas.save();
                canvas.clipPath(this.clip);
                this.paint.setShader(this.glow);
                canvas.drawRect(this.rect, this.paint);
                this.paint.setShader(null);
                canvas.restoreToCount(iSave);
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }
    }

    static final class Glyph extends Drawable {
        final int color;
        final int kind;
        final Paint paint;
        final int size;

        Glyph(int i, int i2) {
            this(i, i2, 24);
        }

        Glyph(int i, int i2, int i3) {
            this.paint = new Paint(1);
            this.kind = i;
            this.color = i2;
            this.size = i3;
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.size;
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.size;
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            canvas.save();
            canvas.translate(bounds.left, bounds.top);
            canvas.scale(bounds.width() / 24.0f, bounds.height() / 24.0f);
            this.paint.setColor(this.color);
            this.paint.setStyle(Paint.Style.FILL);
            if (this.kind == 4) {
                canvas.drawCircle(12.0f, 5.0f, 1.6f, this.paint);
                canvas.drawCircle(12.0f, 12.0f, 1.6f, this.paint);
                canvas.drawCircle(12.0f, 19.0f, 1.6f, this.paint);
            } else if (this.kind == 3) {
                canvas.drawRoundRect(3.0f, 13.0f, 6.0f, 22.0f, 1.5f, 1.5f, this.paint);
                canvas.drawRoundRect(10.0f, 7.0f, 13.0f, 22.0f, 1.5f, 1.5f, this.paint);
                canvas.drawRoundRect(17.0f, 1.0f, 20.0f, 22.0f, 1.5f, 1.5f, this.paint);
            } else if (this.kind == 0) {
                this.paint.setStyle(Paint.Style.STROKE);
                this.paint.setStrokeWidth(1.8f);
                this.paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawArc(3.0f, 4.0f, 21.0f, 23.0f, 180.0f, 180.0f, false, this.paint);
                canvas.drawLine(12.0f, 13.0f, 17.0f, 7.0f, this.paint);
                this.paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(12.0f, 13.0f, 1.6f, this.paint);
            } else if (this.kind == 1) {
                this.paint.setStyle(Paint.Style.STROKE);
                this.paint.setStrokeWidth(1.8f);
                this.paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(4.0f, 6.0f, 20.0f, 6.0f, this.paint);
                canvas.drawLine(4.0f, 12.0f, 20.0f, 12.0f, this.paint);
                canvas.drawLine(4.0f, 18.0f, 20.0f, 18.0f, this.paint);
                this.paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(9.0f, 6.0f, 2.5f, this.paint);
                canvas.drawCircle(16.0f, 12.0f, 2.5f, this.paint);
                canvas.drawCircle(7.0f, 18.0f, 2.5f, this.paint);
            } else {
                canvas.drawRoundRect(4.0f, 4.0f, 20.0f, 20.0f, 3.0f, 3.0f, this.paint);
                for (int i = 7; i <= 17; i += 5) {
                    float f = i;
                    float f2 = i + 1;
                    canvas.drawRect(f, 1.0f, f2, 4.0f, this.paint);
                    canvas.drawRect(f, 20.0f, f2, 23.0f, this.paint);
                    canvas.drawRect(1.0f, f, 4.0f, f2, this.paint);
                    canvas.drawRect(20.0f, f, 23.0f, f2, this.paint);
                }
            }
            canvas.restore();
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
            this.paint.setAlpha(i);
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.paint.setColorFilter(colorFilter);
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }
    }

    static final class Meter extends View {
        ValueAnimator animator;
        final int color;
        float fraction;
        final Paint paint;
        final float target;
        final int track;

        Meter(Context context, float f, int i, int i2) {
            super(context);
            this.paint = new Paint(1);
            this.target = Math.max(0.0f, Math.min(1.0f, f));
            this.color = i;
            this.track = CareArt.blend(i2, i, 0.34f);
            this.fraction = this.target;
            setImportantForAccessibility(2);
        }

        @Override // android.view.View
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (ValueAnimator.areAnimatorsEnabled()) {
                this.fraction = 0.0f;
                this.animator = ValueAnimator.ofFloat(0.0f, this.target);
                this.animator.setDuration(650L);
                this.animator.setInterpolator(CareArt.SETTLE);
                this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: nihil.sunshine.parts.CareArt$Meter$$ExternalSyntheticLambda0
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        this.f$0.m1lambda$onAttachedToWindow$0$nihilsunshinepartsCareArt$Meter(valueAnimator);
                    }
                });
                this.animator.start();
            }
        }

        /* JADX INFO: renamed from: lambda$onAttachedToWindow$0$nihil-sunshine-parts-CareArt$Meter, reason: not valid java name */
        /* synthetic */ void m1lambda$onAttachedToWindow$0$nihilsunshinepartsCareArt$Meter(ValueAnimator valueAnimator) {
            this.fraction = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            invalidate();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            float height = getHeight() / 2.0f;
            this.paint.setColor(this.track);
            canvas.drawRoundRect(0.0f, 0.0f, getWidth(), getHeight(), height, height, this.paint);
            this.paint.setColor(this.color);
            canvas.drawRoundRect(0.0f, 0.0f, getWidth() * this.fraction, getHeight(), height, height, this.paint);
        }

        @Override // android.view.View
        protected void onDetachedFromWindow() {
            if (this.animator != null) {
                this.animator.cancel();
                this.animator = null;
            }
            super.onDetachedFromWindow();
        }
    }

    private CareArt() {
    }
}
