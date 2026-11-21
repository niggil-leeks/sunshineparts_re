package nihil.sunshine.parts;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/* JADX INFO: loaded from: classes.dex */
final class GpuPanelBlur extends BitmapDrawable {
    static volatile long created;
    private static Handler fallbackWorker;
    static volatile long fallbacks;
    static volatile long hardwareDraws;
    private static boolean recognized;
    private static volatile boolean unavailable;
    private final int height;
    private final RenderNode node;
    private boolean pending;
    private final Resources resources;
    private BitmapDrawable software;
    private final Bitmap source;
    private final int width;
    static volatile String error = "";
    private static final Handler main = new Handler(Looper.getMainLooper());

    static GpuPanelBlur create(Bitmap bitmap, Resources resources) {
        if (!recognized || !Config.gpuBlur || !AdaptiveWorkload.reduced() || unavailable || bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        try {
            return new GpuPanelBlur(bitmap, resources);
        } catch (Throwable th) {
            fail(th);
            return null;
        }
    }

    static void install(String str) {
        try {
            recognized = DensityQueries.pinned(str) && IconDimensions.pinned(str);
        } catch (Throwable th) {
            recognized = false;
        }
        ControlBus.feature("gpu_panel_blur", recognized ? "Hardware drawable ready; awaiting a reduced capture" : "Native fallback: target not recognized");
    }

    private static Bitmap placeholder(Resources resources) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(new int[]{(resources.getConfiguration().uiMode & 48) == 32 ? LiteStyle.DARK[1] : LiteStyle.LIGHT[1]}, 1, 1, Bitmap.Config.ARGB_8888);
        bitmapCreateBitmap.setDensity(0);
        return bitmapCreateBitmap;
    }

    private GpuPanelBlur(Bitmap bitmap, Resources resources) {
        super(resources, placeholder(resources));
        this.resources = resources;
        int[] size = BlurPlan.size(bitmap.getWidth(), bitmap.getHeight(), AdaptiveWorkload.previewWidth(), AdaptiveWorkload.previewHeight());
        this.width = size[0];
        this.height = size[1];
        if (bitmap.getConfig() != Bitmap.Config.HARDWARE) {
            this.source = Bitmap.createScaledBitmap(bitmap, this.width, this.height, true).copy(Bitmap.Config.ARGB_8888, false);
        } else {
            HardwareBuffer hardwareBuffer = bitmap.getHardwareBuffer();
            try {
                this.source = Bitmap.wrapHardwareBuffer(hardwareBuffer, bitmap.getColorSpace());
                if (hardwareBuffer != null) {
                    hardwareBuffer.close();
                }
            } catch (Throwable th) {
                if (hardwareBuffer != null) {
                    try {
                        hardwareBuffer.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (this.source != null) {
            this.source.setDensity(0);
            this.node = new RenderNode("SunshinePanelBlur");
            this.node.setPosition(0, 0, this.width, this.height);
            this.node.setClipToBounds(true);
            this.node.setRenderEffect(RenderEffect.createBlurEffect(6.5f, 6.5f, Shader.TileMode.CLAMP));
            Paint paint = new Paint(3);
            paint.setColorFilter(new ColorMatrixColorFilter(BlurPlan.color((resources.getConfiguration().uiMode & 48) == 32)));
            try {
                this.node.beginRecording(this.width, this.height).drawBitmap(this.source, (Rect) null, new Rect(0, 0, this.width, this.height), paint);
                this.node.endRecording();
                setGravity(119);
                setFilterBitmap(true);
                created++;
                return;
            } catch (Throwable th3) {
                this.node.endRecording();
                throw th3;
            }
        }
        throw new IllegalStateException("Owned blur source");
    }

    @Override // android.graphics.drawable.BitmapDrawable, android.graphics.drawable.Drawable
    public int getIntrinsicWidth() {
        return this.width;
    }

    @Override // android.graphics.drawable.BitmapDrawable, android.graphics.drawable.Drawable
    public int getIntrinsicHeight() {
        return this.height;
    }

    @Override // android.graphics.drawable.BitmapDrawable, android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Canvas canvas2;
        if (this.software != null) {
            this.software.setBounds(getBounds());
            this.software.setAlpha(getAlpha());
            this.software.setColorFilter(getColorFilter());
            this.software.draw(canvas);
            return;
        }
        if (!canvas.isHardwareAccelerated() || unavailable) {
            canvas2 = canvas;
        } else {
            int iSave = canvas.save();
            try {
                Rect bounds = getBounds();
                if (!bounds.isEmpty()) {
                    canvas.translate(bounds.left, bounds.top);
                    canvas.scale(bounds.width() / this.width, bounds.height() / this.height);
                    if (getAlpha() == 255 && getColorFilter() == null) {
                        canvas2 = canvas;
                    } else {
                        canvas2 = canvas;
                        try {
                            canvas2.saveLayer(0.0f, 0.0f, this.width, this.height, getPaint());
                        } catch (Throwable th) {
                            th = th;
                        }
                    }
                    canvas2.drawRenderNode(this.node);
                    hardwareDraws++;
                    canvas2.restoreToCount(iSave);
                    return;
                }
                canvas.restoreToCount(iSave);
                return;
            } catch (Throwable th2) {
                th = th2;
                canvas2 = canvas;
            }
            try {
                fail(th);
                canvas2.restoreToCount(iSave);
            } catch (Throwable th3) {
                canvas2.restoreToCount(iSave);
                throw th3;
            }
        }
        super.draw(canvas2);
        prepareFallback();
    }

    private void prepareFallback() {
        if (this.pending) {
            return;
        }
        this.pending = true;
        synchronized (GpuPanelBlur.class) {
            if (fallbackWorker == null) {
                HandlerThread handlerThread = new HandlerThread("SunshineBlurFallback", 10);
                handlerThread.start();
                fallbackWorker = new Handler(handlerThread.getLooper());
            }
        }
        fallbackWorker.post(new Runnable() { // from class: nihil.sunshine.parts.GpuPanelBlur$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m8lambda$prepareFallback$1$nihilsunshinepartsGpuPanelBlur();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$prepareFallback$1$nihil-sunshine-parts-GpuPanelBlur, reason: not valid java name */
    /* synthetic */ void m8lambda$prepareFallback$1$nihilsunshinepartsGpuPanelBlur() {
        try {
            final BitmapDrawable bitmapDrawableScreenshot = PanelBackdrop.screenshot(this.source, this.resources);
            main.post(new Runnable() { // from class: nihil.sunshine.parts.GpuPanelBlur$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m7lambda$prepareFallback$0$nihilsunshinepartsGpuPanelBlur(bitmapDrawableScreenshot);
                }
            });
        } catch (Throwable th) {
            fail(th);
        }
    }

    /* JADX INFO: renamed from: lambda$prepareFallback$0$nihil-sunshine-parts-GpuPanelBlur, reason: not valid java name */
    /* synthetic */ void m7lambda$prepareFallback$0$nihilsunshinepartsGpuPanelBlur(BitmapDrawable bitmapDrawable) {
        this.software = bitmapDrawable;
        fallbacks++;
        invalidateSelf();
    }

    private static void fail(Throwable th) {
        unavailable = true;
        error = th.getClass().getSimpleName();
    }
}
