package nihil.sunshine.parts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;

/* JADX INFO: loaded from: classes.dex */
public final class LicenseRuntime {
    static final String ACTION = "nihil.sunshine.parts.ACCESS_CONTROL";
    static final String KEY = "persist.sys.sunshineparts.access";
    private static Context context;
    private static long expiresAt;
    private static boolean initialized;
    private static Handler main;
    private static String scope = "";
    private static final Runnable expire = new Runnable() { // from class: nihil.sunshine.parts.LicenseRuntime$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            LicenseRuntime.lambda$static$0();
        }
    };
    private static final Runnable renew = new Runnable() { // from class: nihil.sunshine.parts.LicenseRuntime$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            LicenseRuntime.request("check");
        }
    };

    static /* synthetic */ void lambda$static$0() {
        reload();
        if (AccessState.blocked) {
            request("expire");
        }
    }

    static boolean blocked() {
        return AccessState.blocked;
    }

    static int boot(Context context2) {
        try {
            return Settings.Global.getInt(context2.getContentResolver(), "boot_count", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    static void attach(Context context2, String str) {
        context = context2;
        scope = str;
        if (main == null) {
            main = new Handler(Looper.getMainLooper());
        }
        context2.registerReceiver(new BroadcastReceiver() { // from class: nihil.sunshine.parts.LicenseRuntime.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context3, Intent intent) {
                LicenseRuntime.reload();
            }
        }, new IntentFilter("nihil.sunshine.parts.ACCESS_CHANGED"), "android.permission.DUMP", main, 2);
        reload();
    }

    private static AccessLease fallback() {
        try {
            long j = context.getPackageManager().getPackageInfo("nihil.sunshine.parts", 0).lastUpdateTime;
            long j2 = j + 3600000;
            return new AccessLease(j, j2, boot(context), SystemClock.elapsedRealtime() + Math.max(0L, Math.min(3600000L, j2 - System.currentTimeMillis())));
        } catch (Exception e) {
            return null;
        }
    }

    static void reload() {
        if (context == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(new Runnable() { // from class: nihil.sunshine.parts.LicenseRuntime$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LicenseRuntime.reload();
                }
            });
            return;
        }
        if (PassageGate.hasValidPassage(context)) {
            main.removeCallbacks(expire);
            main.removeCallbacks(renew);
            boolean z = !initialized || AccessState.blocked;
            initialized = true;
            AccessState.blocked = false;
            if (z) {
                Config.load();
                try {
                    RuntimeHooks.resume();
                    applySelection();
                    return;
                } catch (Throwable th) {
                    AccessState.blocked = true;
                    Config.nativeOnly();
                    RuntimeHooks.suspend();
                    applyNative();
                    return;
                }
            }
            return;
        }
        AccessLease accessLeaseFallback = AccessLease.parse(Config.property("persist.sys.sunshineparts.access"));
        if (accessLeaseFallback == null) {
            accessLeaseFallback = fallback();
        }
        AccessLease accessLease = accessLeaseFallback;
        long jRemaining = accessLease == null ? 0L : accessLease.remaining(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(context));
        boolean z2 = jRemaining == 0;
        main.removeCallbacks(expire);
        main.removeCallbacks(renew);
        expiresAt = SystemClock.elapsedRealtime() + jRemaining;
        boolean z3 = (initialized && z2 == AccessState.blocked) ? false : true;
        initialized = true;
        AccessState.blocked = z2;
        if (z3) {
            Config.load();
            if (z2) {
                RuntimeHooks.suspend();
                applyNative();
                request("expire");
            } else {
                try {
                    RuntimeHooks.resume();
                    applySelection();
                } catch (Throwable th2) {
                    AccessState.blocked = true;
                    Config.nativeOnly();
                    RuntimeHooks.suspend();
                    applyNative();
                }
            }
        }
        if (!AccessState.blocked) {
            main.postDelayed(expire, jRemaining);
            main.postDelayed(renew, Math.max(1000L, jRemaining - 30000));
        }
    }

    private static void applyNative() {
        try {
            TransitionEffects.reload();
        } catch (Throwable th) {
        }
        if ("android".equals(scope)) {
            RefreshHook.reload();
        } else if ("com.android.systemui".equals(scope)) {
            try {
                SmartRam.configure();
                SoundAliveWork.configure();
                SystemUiLite.releaseRuntime();
            } catch (Throwable th2) {
            }
        }
    }

    private static void applySelection() {
        if (RuntimeProfile.recovery()) {
            return;
        }
        try {
            TransitionEffects.reload();
        } catch (Throwable th) {
        }
        if ("android".equals(scope)) {
            RefreshHook.reload();
            return;
        }
        if ("com.android.systemui".equals(scope)) {
            try {
                SmartRam.configure();
                SoundAliveWork.configure();
                SystemUiLite.workloadChanged();
                ControlBus.syncLegacy();
            } catch (Throwable th2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void request(String str) {
        try {
            context.sendBroadcast(new Intent(ACTION).addFlags(32).setComponent(new ComponentName("nihil.sunshine.parts", "nihil.sunshine.parts.LicenseControlReceiver")).putExtra("op", str));
        } catch (Throwable th) {
        }
    }

    private LicenseRuntime() {
    }
}
