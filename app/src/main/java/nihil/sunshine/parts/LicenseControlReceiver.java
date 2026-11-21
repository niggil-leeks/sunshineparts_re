package nihil.sunshine.parts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* JADX INFO: loaded from: classes.dex */
public final class LicenseControlReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, final Intent intent) {
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        final Context applicationContext = context.getApplicationContext();
        SerialAccess.worker.execute(new Runnable() { // from class: nihil.sunshine.parts.LicenseControlReceiver$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LicenseControlReceiver.lambda$onReceive$0(applicationContext, intent, pendingResultGoAsync);
            }
        });
    }

    static /* synthetic */ void lambda$onReceive$0(Context context, Intent intent, BroadcastReceiver.PendingResult pendingResult) {
        try {
            SerialAccess.maintain(context, !"expire".equals(intent.getStringExtra("op")));
        } finally {
            pendingResult.finish();
        }
    }
}
