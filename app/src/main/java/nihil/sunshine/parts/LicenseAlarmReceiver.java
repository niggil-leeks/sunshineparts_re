package nihil.sunshine.parts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* JADX INFO: loaded from: classes.dex */
public final class LicenseAlarmReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        final Context applicationContext = context.getApplicationContext();
        SerialAccess.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.LicenseAlarmReceiver$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LicenseAlarmReceiver.lambda$onReceive$0(applicationContext, pendingResultGoAsync);
            }
        });
    }

    static /* synthetic */ void lambda$onReceive$0(Context context, BroadcastReceiver.PendingResult pendingResult) {
        try {
            SerialAccess.maintain(context, true);
        } finally {
            pendingResult.finish();
        }
    }
}
