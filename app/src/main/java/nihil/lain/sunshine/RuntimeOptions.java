package nihil.lain.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

/* JADX INFO: loaded from: classes2.dex */
final class RuntimeOptions {
    static final String ACTION = "nihil.lain.sunshine.CONTROL";
    static final int ANIM = 2;
    static final int ASYNC = 0;
    static final int COMPOSE_HOST = 4;
    static final int COMPOSE_LAYOUT = 3;
    static final int LAYOUT = 1;
    static final int REFRESH = 5;
    static Runnable changed;
    static boolean registered;
    static Reporter reporter;
    private static final String[] NAMES = {"async_blur", "tile_layout", "tile_anim", "compose_layout", "compose_host", "refresh"};
    private static volatile boolean[] values = defaults();

    interface Reporter {
        String get();
    }

    RuntimeOptions() {
    }

    static boolean[] defaults() {
        return new boolean[]{true, true, true, true, true, false};
    }

    static boolean enabled(int i) {
        return values[i];
    }

    static boolean apply(String str, boolean z) {
        boolean z2;
        boolean[] zArrDefaults = (boolean[]) values.clone();
        boolean zEquals = "defaults".equals(str);
        int i = ASYNC;
        if (zEquals) {
            zArrDefaults = defaults();
            z2 = LAYOUT;
        } else {
            z2 = ASYNC;
        }
        while (true) {
            String[] strArr = NAMES;
            if (i >= strArr.length) {
                break;
            }
            if ("all".equals(str) || strArr[i].equals(str)) {
                zArrDefaults[i] = z;
                z2 = LAYOUT;
            }
            i += LAYOUT;
        }
        if (z2) {
            values = zArrDefaults;
            try {
                Runnable runnable = changed;
                if (runnable != null) {
                    runnable.run();
                }
            } catch (Throwable th) {
                SunshineModule.diagnostic("controle: falha ao restaurar estado: " + th.getClass().getSimpleName());
            }
            SunshineModule.diagnostic("controle: " + report());
        }
        return z2;
    }

    static void register(Context context) {
        if (registered) {
            return;
        }
        registered = true;
        try {
            context.registerReceiver(new BroadcastReceiver() { // from class: nihil.lain.sunshine.RuntimeOptions.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    if (RuntimeOptions.ACTION.equals(intent.getAction())) {
                        if (intent.hasExtra("enabled")) {
                            RuntimeOptions.apply(intent.getStringExtra("feature"), intent.getBooleanExtra("enabled", true));
                        }
                        setResultData(RuntimeOptions.report());
                    }
                }
            }, new IntentFilter(ACTION), "android.permission.DUMP", new Handler(Looper.getMainLooper()), ANIM);
            SunshineModule.diagnostic("controle em tempo real pronto; " + report());
        } catch (Throwable th) {
            SunshineModule.diagnostic("controle indisponivel: " + th.getClass().getSimpleName());
        }
    }

    static String report() {
        StringBuilder sb = new StringBuilder();
        int i = ASYNC;
        while (true) {
            String[] strArr = NAMES;
            if (i < strArr.length) {
                if (i > 0) {
                    sb.append("; ");
                }
                sb.append(strArr[i]);
                sb.append('=');
                sb.append(values[i]);
                i += LAYOUT;
            } else {
                try {
                    break;
                } catch (Throwable unused) {
                }
            }
        }
        if (reporter != null) {
            sb.append("; ");
            sb.append(reporter.get());
        }
        return sb.toString();
    }
}
