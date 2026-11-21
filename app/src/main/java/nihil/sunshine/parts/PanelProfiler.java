package nihil.sunshine.parts;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class PanelProfiler {
    private static Handler handler;
    private static boolean owned;
    private static String state = "idle";
    private static String path = "";
    private static String error = "";
    private static final Runnable watchdog = new Runnable() { // from class: nihil.sunshine.parts.PanelProfiler$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            PanelProfiler.stop();
        }
    };

    static void start(Context context) {
        if (owned) {
            return;
        }
        error = "";
        try {
            Method declaredMethod = Class.forName("dalvik.system.VMDebug").getDeclaredMethod("getMethodTracingMode", new Class[0]);
            declaredMethod.setAccessible(true);
            if (((Number) declaredMethod.invoke(null, new Object[0])).intValue() != 0) {
                throw new IllegalStateException("An external method profiler is already running");
            }
            path = new File(context.getCacheDir(), "sunshine-qs-" + SystemClock.uptimeMillis() + ".trace").getAbsolutePath();
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            Debug.startMethodTracingSampling(path, 16777216, 20000);
            owned = true;
            state = "recording";
            if (!handler.postDelayed(watchdog, 18000L)) {
                stop();
            }
        } catch (Throwable th) {
            state = "unavailable";
            error = th.getClass().getSimpleName() + ": " + String.valueOf(th.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void stop() {
        if (owned) {
            owned = false;
            handler.removeCallbacks(watchdog);
            try {
                Debug.stopMethodTracing();
                state = "finished";
            } catch (Throwable th) {
                state = "stop_failed";
                error = th.getClass().getSimpleName() + ": " + String.valueOf(th.getMessage());
            }
        }
    }

    static String state() {
        return state;
    }

    static String path() {
        return path;
    }

    static String error() {
        return error;
    }

    private PanelProfiler() {
    }
}
