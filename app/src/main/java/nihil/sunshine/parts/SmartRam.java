package nihil.sunshine.parts;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

/* JADX INFO: loaded from: classes.dex */
final class SmartRam {
    static volatile long checks;
    private static Handler main;
    private static ActivityManager manager;
    private static boolean pending;
    static volatile boolean ready;
    static volatile long trims;
    private static Handler worker;
    static volatile String error = "";
    private static final ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
    private static final SmartRamPolicy policy = new SmartRamPolicy();
    private static final Runnable tick = new Runnable() { // from class: nihil.sunshine.parts.SmartRam$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            SmartRam.lambda$static$3();
        }
    };

    static /* synthetic */ void lambda$static$3() {
        pending = false;
        if (!Config.smartRam || RuntimeProfile.recovery()) {
            return;
        }
        if (worker == null) {
            HandlerThread handlerThread = new HandlerThread("SunshineMemory", 10);
            handlerThread.start();
            worker = new Handler(handlerThread.getLooper());
        }
        worker.post(new Runnable() { // from class: nihil.sunshine.parts.SmartRam$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SmartRam.lambda$static$2();
            }
        });
    }

    static /* synthetic */ void lambda$static$2() {
        try {
            manager.getMemoryInfo(memory);
            checks++;
            if (policy.trim(SystemClock.elapsedRealtime(), Config.smartRam, memory.availMem, memory.totalMem, memory.threshold, memory.lowMemory, PreparedBlur.panelVisible)) {
                main.post(new Runnable() { // from class: nihil.sunshine.parts.SmartRam$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        SmartRam.lambda$static$0();
                    }
                });
            }
        } catch (Throwable th) {
            error = th.getClass().getSimpleName();
        }
        main.post(new Runnable() { // from class: nihil.sunshine.parts.SmartRam$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                SmartRam.schedule(60000L);
            }
        });
    }

    static /* synthetic */ void lambda$static$0() {
        if (!Config.smartRam || PreparedBlur.panelVisible) {
            return;
        }
        PreparedBlur.trimIdle();
        trims++;
    }

    static void install(Context context) {
        if (ready) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) {
            throw new IllegalStateException("Application is not initialized");
        }
        ActivityManager activityManager = (ActivityManager) applicationContext.getSystemService(ActivityManager.class);
        if (activityManager == null) {
            throw new IllegalStateException("ActivityManager unavailable");
        }
        applicationContext.registerComponentCallbacks(new ComponentCallbacks2() { // from class: nihil.sunshine.parts.SmartRam.1
            @Override // android.content.ComponentCallbacks
            public void onConfigurationChanged(Configuration configuration) {
            }

            @Override // android.content.ComponentCallbacks
            public void onLowMemory() {
            }

            @Override // android.content.ComponentCallbacks2
            public void onTrimMemory(int i) {
                if (i >= 40) {
                    SmartRam.schedule(20000L);
                }
            }
        });
        applicationContext.registerReceiver(new BroadcastReceiver() { // from class: nihil.sunshine.parts.SmartRam.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                SmartRam.schedule(20000L);
            }
        }, new IntentFilter("android.intent.action.SCREEN_OFF"), 4);
        manager = activityManager;
        main = new Handler(Looper.getMainLooper());
        ready = true;
        error = "";
        ControlBus.feature("smart_ram_error", "");
        configure();
        ControlBus.feature("smart_ram", "Pressure-aware module buffer maintenance; no forced GC or app kills");
    }

    static void configure() {
        if (main == null) {
            return;
        }
        main.removeCallbacks(tick);
        pending = false;
        if (Config.smartRam) {
            schedule(20000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void schedule(long j) {
        if (main == null || !Config.smartRam || pending) {
            return;
        }
        pending = true;
        main.postDelayed(tick, j);
    }

    private SmartRam() {
    }
}
