package nihil.sunshine.parts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class CpuMonitorService extends Service {
    private CpuMonitor monitor;
    private boolean stopped;
    private final LocalBinder binder = new LocalBinder();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CpuHistory history = new CpuHistory();
    private final List<Observer> observers = new ArrayList();
    private List<CpuPolicy> values = Collections.emptyList();
    private String error = "";

    interface Observer {
        void error(String str);

        void sample(CpuHistory cpuHistory, List<CpuPolicy> list);
    }

    final class LocalBinder extends Binder {
        LocalBinder() {
        }

        CpuMonitorService service() {
            return CpuMonitorService.this;
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        NotificationChannel notificationChannel = new NotificationChannel("cpu_monitor", I18n.text(this, "CPU monitoring"), 2);
        notificationChannel.setSound(null, null);
        notificationChannel.enableVibration(false);
        notificationManager.createNotificationChannel(notificationChannel);
        startForeground(2161, new Notification.Builder(this, "cpu_monitor").setSmallIcon(getResources().getIdentifier("ic_cpu_monitor", "drawable", getPackageName())).setContentTitle(I18n.text(this, "CPU monitoring")).setContentText(I18n.text(this, "Updates every 200 ms")).setCategory("service").setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false).setContentIntent(PendingIntent.getActivity(this, 2161, new Intent(this, (Class<?>) MainActivity.class).putExtra("cpu_monitor", true).addFlags(603979776), 201326592)).build(), 1073741824);
        this.monitor = new CpuMonitor(new AnonymousClass1());
    }

    /* JADX INFO: renamed from: nihil.sunshine.parts.CpuMonitorService$1, reason: invalid class name */
    class AnonymousClass1 implements CpuMonitor.Listener {
        AnonymousClass1() {
        }

        @Override // nihil.sunshine.parts.CpuMonitor.Listener
        public void sample(final long j, final List<CpuPolicy> list) {
            CpuMonitorService.this.main.post(new Runnable() { // from class: nihil.sunshine.parts.CpuMonitorService$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m6lambda$sample$0$nihilsunshinepartsCpuMonitorService$1(list, j);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$sample$0$nihil-sunshine-parts-CpuMonitorService$1, reason: not valid java name */
        /* synthetic */ void m6lambda$sample$0$nihilsunshinepartsCpuMonitorService$1(List list, long j) {
            if (CpuMonitorService.this.stopped) {
                return;
            }
            CpuMonitorService.this.values = list;
            CpuMonitorService.this.history.add(j, list);
            CpuMonitorService.this.error = "";
            Iterator it = new ArrayList(CpuMonitorService.this.observers).iterator();
            while (it.hasNext()) {
                ((Observer) it.next()).sample(CpuMonitorService.this.history, CpuMonitorService.this.values);
            }
        }

        @Override // nihil.sunshine.parts.CpuMonitor.Listener
        public void error(final String str) {
            CpuMonitorService.this.main.post(new Runnable() { // from class: nihil.sunshine.parts.CpuMonitorService$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m5lambda$error$1$nihilsunshinepartsCpuMonitorService$1(str);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$error$1$nihil-sunshine-parts-CpuMonitorService$1, reason: not valid java name */
        /* synthetic */ void m5lambda$error$1$nihilsunshinepartsCpuMonitorService$1(String str) {
            if (CpuMonitorService.this.stopped) {
                return;
            }
            CpuMonitorService.this.error = str;
            Iterator it = new ArrayList(CpuMonitorService.this.observers).iterator();
            while (it.hasNext()) {
                ((Observer) it.next()).error(str);
            }
        }
    }

    void observe(Observer observer) {
        if (!this.observers.contains(observer)) {
            this.observers.add(observer);
        }
        if (!this.values.isEmpty()) {
            observer.sample(this.history, this.values);
        }
        if (this.error.isEmpty()) {
            return;
        }
        observer.error(this.error);
    }

    void remove(Observer observer) {
        this.observers.remove(observer);
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        return 2;
    }

    @Override // android.app.Service
    public void onTaskRemoved(Intent intent) {
        stopReading();
        stopSelf();
        super.onTaskRemoved(intent);
    }

    private void stopReading() {
        this.stopped = true;
        if (this.monitor != null) {
            this.monitor.stop();
            this.monitor = null;
        }
        this.main.removeCallbacksAndMessages(null);
    }

    @Override // android.app.Service
    public void onDestroy() {
        stopReading();
        this.observers.clear();
        stopForeground(1);
        super.onDestroy();
    }
}
