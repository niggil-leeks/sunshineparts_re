package nihil.sunshine.parts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;

/* JADX INFO: loaded from: classes.dex */
final class AdaptiveWorkload {
    private static boolean installed;
    static long reducedCaptures;
    private static boolean updateMedia;
    static volatile int thermalStatus = -1;
    static volatile int batteryTemperature = -1;

    static int select(int i, int i2) {
        if (i == 0) {
            return 0;
        }
        if (i == 2) {
            return 1;
        }
        if (i == 3) {
            return 2;
        }
        if (i == 4) {
            return 3;
        }
        if (i2 < 0 || i2 > 700) {
            return 1;
        }
        if (i2 >= 420) {
            return 2;
        }
        return i2 >= 350 ? 1 : 0;
    }

    static int level() {
        return select(Config.workloadMode, batteryTemperature);
    }

    static boolean reduced() {
        return level() > 0;
    }

    static void battery(int i) {
        batteryTemperature = i;
    }

    static Bitmap finalBlur(Bitmap bitmap) {
        if (!reduced() || bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        return bitmap;
    }

    static int mediaStep() {
        int iLevel = level();
        if (iLevel == 0) {
            return Config.mediaStep;
        }
        if (iLevel == 3) {
            return 40;
        }
        return iLevel == 2 ? 24 : 8;
    }

    static int blurEdge() {
        if (level() == 3) {
            return 72;
        }
        return level() == 2 ? 240 : 360;
    }

    static int previewWidth() {
        if (level() == 3) {
            return 16;
        }
        return level() == 2 ? 64 : 144;
    }

    static int previewHeight() {
        if (level() == 3) {
            return 48;
        }
        return level() == 2 ? 192 : 320;
    }

    static int[] size(int i, int i2, boolean z) {
        if (i < 1 || i2 < 1) {
            throw new IllegalArgumentException("Bitmap size");
        }
        int iMax = Math.max(i, i2);
        int iBlurEdge = blurEdge();
        if (!z || iMax <= iBlurEdge) {
            return new int[]{i, i2};
        }
        float f = iBlurEdge / iMax;
        return new int[]{Math.max(1, Math.round(i * f)), Math.max(1, Math.round(i2 * f))};
    }

    static Bitmap reduce(Bitmap bitmap) {
        Bitmap bitmapCreateScaledBitmap;
        if (bitmap == null || bitmap.isRecycled() || !reduced()) {
            return bitmap;
        }
        int[] size = size(bitmap.getWidth(), bitmap.getHeight(), true);
        if ((size[0] == bitmap.getWidth() && size[1] == bitmap.getHeight()) || (bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmap, size[0], size[1], true)) == bitmap) {
            return bitmap;
        }
        if (!bitmapCreateScaledBitmap.isMutable()) {
            bitmapCreateScaledBitmap = bitmapCreateScaledBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        if (bitmapCreateScaledBitmap == null) {
            return bitmap;
        }
        reducedCaptures++;
        return bitmapCreateScaledBitmap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void changed(int i) {
        if (!updateMedia || i == level()) {
            return;
        }
        SystemUiLite.workloadChanged();
    }

    static void install(Context context) {
        install(context, true);
    }

    static void install(Context context, boolean z) {
        if (installed) {
            return;
        }
        installed = true;
        updateMedia = z;
        try {
            Intent intentRegisterReceiver = context.registerReceiver(new BroadcastReceiver() { // from class: nihil.sunshine.parts.AdaptiveWorkload.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    int iLevel = AdaptiveWorkload.level();
                    AdaptiveWorkload.battery(intent.getIntExtra("temperature", -1));
                    AdaptiveWorkload.changed(iLevel);
                }
            }, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (intentRegisterReceiver != null) {
                battery(intentRegisterReceiver.getIntExtra("temperature", -1));
            }
        } catch (Throwable th) {
            battery(-1);
        }
        if (updateMedia) {
            SystemUiLite.workloadChanged();
        }
        ControlBus.feature("adaptive_workload", "Event-driven visual workload");
    }

    private AdaptiveWorkload() {
    }
}
