package nihil.sunshine.parts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/* JADX INFO: loaded from: classes.dex */
public final class BootReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(final Context context, Intent intent) {
        if (!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) || !Config.target(Build.DISPLAY, Config.property("ro.build.version.oneui"), Build.VERSION.SDK_INT)) {
            return;
        }
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        new Thread(new Runnable() { // from class: nihil.sunshine.parts.BootReceiver$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BootReceiver.lambda$onReceive$0(context, pendingResultGoAsync);
            }
        }, "parts-profile-restore").start();
    }

    static /* synthetic */ void lambda$onReceive$0(Context context, BroadcastReceiver.PendingResult pendingResult) {
        AccessLease accessLease;
        try {
            try {
                RootOps rootOps = new RootOps(context);
                if (!PassageGate.hasValidPassage(context, rootOps) && !SerialAccess.verify(rootOps) && ((accessLease = AccessLease.parse(Config.property(PassageGate.KEY_ACCESS))) == null || accessLease.remaining(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(context)) == 0)) {
                    return;
                }
                long jElapsedRealtime = SystemClock.elapsedRealtime() + 8000;
                try {
                    rootOps.ensureReducedLogging(3);
                } catch (Exception e) {
                }
                int iElapsedRealtime = (int) ((jElapsedRealtime - SystemClock.elapsedRealtime()) / 1000);
                if (iElapsedRealtime > 0) {
                    rootOps.applySavedPerformance(iElapsedRealtime);
                }
            } finally {
                pendingResult.finish();
            }
        } catch (Exception e2) {
        }
    }
}
