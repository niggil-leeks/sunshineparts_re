package nihil.sunshine.parts;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

/* JADX INFO: loaded from: classes.dex */
final class PanelBackdrop {
    static long cacheHits;
    private static Ready cached;
    private static Bitmap darkFallback;
    static long failures;
    static long immediate;
    private static Bitmap lightFallback;
    private static Handler main;
    private static boolean pending;
    private static long retryAt;
    static long screenshotReady;
    private static UserCheck users;
    static long wallpaperReady;
    private static Handler worker;
    private static final LatestOnly<Warm> queue = new LatestOnly<>();
    static String error = "";

    interface UserCheck {
        int current() throws Exception;
    }

    interface WallpaperSource {
        Bitmap load() throws Exception;
    }

    static boolean isReady() {
        return (cached == null || cached.bitmap.isRecycled()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Warm {
        final Configuration config;
        final Resources resources;
        final WallpaperSource source;
        final int user;

        Warm(Context context, int i, WallpaperSource wallpaperSource) {
            this.resources = context.getResources();
            this.config = new Configuration(this.resources.getConfiguration());
            this.user = i;
            this.source = wallpaperSource;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Ready {
        final Bitmap bitmap;
        final Configuration config;
        final int user;

        Ready(Warm warm, Bitmap bitmap) {
            this.config = warm.config;
            this.user = warm.user;
            this.bitmap = bitmap;
        }
    }

    static void prepare(Context context, int i, WallpaperSource wallpaperSource, UserCheck userCheck) {
        if (pending || SystemClock.uptimeMillis() < retryAt) {
            return;
        }
        Warm warm = new Warm(context, i, wallpaperSource);
        if (main == null) {
            main = new Handler(Looper.getMainLooper());
        }
        pending = true;
        users = userCheck;
        LatestOnly.Work<Warm> workOffer = queue.offer(warm);
        if (workOffer != null) {
            run(workOffer);
        }
    }

    private static void run(final LatestOnly.Work<Warm> work) {
        if (worker == null) {
            HandlerThread handlerThread = new HandlerThread("SunshineBlurPreview", 10);
            handlerThread.start();
            worker = new Handler(handlerThread.getLooper());
        }
        worker.post(new Runnable() { // from class: nihil.sunshine.parts.PanelBackdrop$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                PanelBackdrop.lambda$run$2(work);
            }
        });
    }

    /* JADX WARN: Multi-variable type inference failed */
    static /* synthetic */ void lambda$run$2(final LatestOnly.Work work) {
        final Bitmap bitmapRaster = null;
        try {
            if (queue.current(work)) {
                Bitmap bitmapLoad = ((Warm) work.value).source.load();
                if (bitmapLoad == null) {
                    throw new IllegalStateException("Wallpaper capture unavailable");
                }
                bitmapRaster = raster(bitmapLoad, ((Warm) work.value).resources);
            }
        } catch (Throwable th) {
            main.post(new Runnable() { // from class: nihil.sunshine.parts.PanelBackdrop$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PanelBackdrop.lambda$run$0(work, th);
                }
            });
        }
        main.post(new Runnable() { // from class: nihil.sunshine.parts.PanelBackdrop$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                PanelBackdrop.lambda$run$1(work, bitmapRaster);
            }
        });
    }

    static /* synthetic */ void lambda$run$0(LatestOnly.Work work, Throwable th) {
        if (queue.current(work)) {
            failures++;
            retryAt = SystemClock.uptimeMillis() + 30000;
            while (th.getCause() != null) {
                th = th.getCause();
            }
            error = th.getClass().getSimpleName() + (th.getMessage() == null ? "" : ": " + th.getMessage().substring(0, Math.min(120, th.getMessage().length())));
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    static /* synthetic */ void lambda$run$1(LatestOnly.Work work, Bitmap bitmap) {
        LatestOnly.Completion<Warm> completionComplete = queue.complete(work);
        pending = completionComplete.next != null;
        try {
            Warm warm = (Warm) work.value;
            if (completionComplete.current && bitmap != null && VisualPolicy.nativeBlur() && VisualPolicy.preparedBlur() && users.current() == warm.user && warm.config.equals(warm.resources.getConfiguration())) {
                cached = new Ready(warm, bitmap);
                wallpaperReady++;
                error = "";
                retryAt = 0L;
                PreparedBlur.previewReady();
            }
        } catch (Throwable th) {
            failures++;
        }
        if (completionComplete.next != null) {
            run(completionComplete.next);
        }
    }

    static BitmapDrawable opening(Resources resources, int i) {
        immediate++;
        Ready ready = cached;
        if (ready != null && ready.user == i && ready.config.equals(resources.getConfiguration()) && !ready.bitmap.isRecycled()) {
            cacheHits++;
            return drawable(resources, ready.bitmap);
        }
        boolean z = (resources.getConfiguration().uiMode & 48) == 32;
        Bitmap bitmapCreateBitmap = z ? darkFallback : lightFallback;
        if (bitmapCreateBitmap == null) {
            bitmapCreateBitmap = Bitmap.createBitmap(new int[]{z ? LiteStyle.DARK[1] : LiteStyle.LIGHT[1]}, 1, 1, Bitmap.Config.ARGB_8888);
            bitmapCreateBitmap.setDensity(0);
            if (z) {
                darkFallback = bitmapCreateBitmap;
            } else {
                lightFallback = bitmapCreateBitmap;
            }
        }
        return drawable(resources, bitmapCreateBitmap);
    }

    static BitmapDrawable screenshot(Bitmap bitmap, Resources resources) {
        return drawable(resources, raster(bitmap, resources));
    }

    private static Bitmap raster(Bitmap bitmap, Resources resources) {
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException("Recycled source");
        }
        int[] iArrPreviewSize = BlurPixels.previewSize(bitmap.getWidth(), bitmap.getHeight(), AdaptiveWorkload.previewWidth(), AdaptiveWorkload.previewHeight());
        int i = iArrPreviewSize[0];
        int i2 = iArrPreviewSize[1];
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmap, i, i2, true);
        if (bitmapCreateScaledBitmap.getConfig() == Bitmap.Config.HARDWARE) {
            bitmapCreateScaledBitmap = bitmapCreateScaledBitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        int[] iArr = new int[i * i2];
        bitmapCreateScaledBitmap.getPixels(iArr, 0, i, 0, 0, i, i2);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(BlurPixels.soften(iArr, i, i2, (resources.getConfiguration().uiMode & 48) == 32), i, i2, Bitmap.Config.ARGB_8888);
        bitmapCreateBitmap.setDensity(0);
        bitmapCreateBitmap.prepareToDraw();
        return bitmapCreateBitmap;
    }

    private static BitmapDrawable drawable(Resources resources, Bitmap bitmap) {
        BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
        bitmapDrawable.setGravity(119);
        bitmapDrawable.setFilterBitmap(true);
        return bitmapDrawable;
    }

    static void reset() {
        queue.invalidate();
        cached = null;
        pending = false;
        retryAt = 0L;
    }

    private PanelBackdrop() {
    }
}
