package nihil.sunshine.parts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

/* JADX INFO: loaded from: classes.dex */
final class SerialAccess {
    private static final String DIR = "/data/adb/sunshineparts-access";
    private static final String STATE = "/data/adb/sunshineparts-access/state";
    static final String URL = "https://raw.githubusercontent.com/haxiswillow/Sunshine/main/serial.txt";
    static volatile AccessLease lastLease;
    private static final Object lock = new Object();
    private static final ScheduledExecutorService timeout = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() { // from class: nihil.sunshine.parts.SerialAccess$$ExternalSyntheticLambda0
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return SerialAccess.lambda$static$0(runnable);
        }
    });
    static final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: nihil.sunshine.parts.SerialAccess$$ExternalSyntheticLambda1
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return SerialAccess.lambda$static$1(runnable);
        }
    });

    static Object mutationLock() {
        return lock;
    }

    static boolean usable(Context context) {
        if (PassageGate.hasValidPassage(context)) {
            return true;
        }
        AccessLease accessLease = lastLease;
        return accessLease != null && accessLease.remaining(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(context)) > 0;
    }

    static /* synthetic */ Thread lambda$static$0(Runnable runnable) {
        Thread thread = new Thread(runnable, "parts-access-timeout");
        thread.setDaemon(true);
        return thread;
    }

    static final class Denied extends Exception {
        Denied() {
            super("Access not verified");
        }
    }

    static /* synthetic */ Thread lambda$static$1(Runnable runnable) {
        return new Thread(runnable, "parts-access");
    }

    static final class Record {
        final String hash;
        final AccessLease lease;

        Record(AccessLease accessLease, String str) {
            this.lease = accessLease;
            this.hash = str;
        }
    }

    private static String hash(String str) throws Exception {
        byte[] bArrDigest = MessageDigest.getInstance("SHA-256").digest(str.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bArrDigest) {
            sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    private static String serial(RootOps rootOps) throws Exception {
        RootOps.Result resultRun = rootOps.run("getprop ro.serialno", 5);
        resultRun.require();
        String strNormalize = SerialList.normalize(resultRun.text);
        if (!strNormalize.isEmpty()) {
            return strNormalize;
        }
        RootOps.Result resultRun2 = rootOps.run("getprop ro.boot.serialno", 5);
        resultRun2.require();
        return SerialList.normalize(resultRun2.text);
    }

    private static Record read(RootOps rootOps) throws Exception {
        RootOps.Result resultRun = rootOps.run("if [ -f /data/adb/sunshineparts-access/state ]; then printf 'PRESENT\\n'; cat /data/adb/sunshineparts-access/state; else printf 'ABSENT'; fi", 5);
        resultRun.require();
        String[] strArrSplit = resultRun.text.split(String.valueOf('\n'), -1);
        if (resultRun.text.equals("ABSENT")) {
            return null;
        }
        if (strArrSplit.length < 3 || !strArrSplit[0].equals("PRESENT")) {
            throw new IOException("Invalid access state");
        }
        AccessLease accessLease = AccessLease.parse(strArrSplit[1].trim());
        String strTrim = strArrSplit[2].trim();
        if (accessLease == null || !strTrim.matches("[a-f0-9]{64}")) {
            throw new IOException("Invalid access state");
        }
        return new Record(accessLease, strTrim);
    }

    private static void write(RootOps rootOps, Record record) throws Exception {
        rootOps.run("umask 077\nmkdir -p /data/adb/sunshineparts-access\nchmod 700 /data/adb/sunshineparts-access\nprintf '%s' " + FlightCommands.quote(record.lease.encode() + "\n" + record.hash + "\n") + " > /data/adb/sunshineparts-access/state.tmp\nchmod 600 /data/adb/sunshineparts-access/state.tmp\nmv /data/adb/sunshineparts-access/state.tmp /data/adb/sunshineparts-access/state\n" + leaseProperty(record.lease), 5).require();
    }

    private static String leaseProperty(AccessLease accessLease) {
        String strEncode = accessLease.encode();
        if (AccessLease.parse(strEncode) == null || strEncode.length() > 90) {
            throw new IllegalArgumentException("Access lease");
        }
        return "if command -v resetprop >/dev/null 2>&1; then resetprop -p " + FlightCommands.quote(PassageGate.KEY_ACCESS) + " " + FlightCommands.quote(strEncode) + "; else setprop " + FlightCommands.quote(PassageGate.KEY_ACCESS) + " " + FlightCommands.quote(strEncode) + "; fi\n[ \"$(getprop " + FlightCommands.quote(PassageGate.KEY_ACCESS) + ")\" = " + FlightCommands.quote(strEncode) + " ] || exit 12";
    }

    static String download() throws Exception {
        final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(URL).openConnection();
        httpsURLConnection.setConnectTimeout(4000);
        httpsURLConnection.setReadTimeout(4000);
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);
        httpsURLConnection.setRequestProperty("Cache-Control", "no-cache");
        httpsURLConnection.setRequestProperty("User-Agent", "Sunshine-Parts/2.31.3-r");
        ScheduledExecutorService scheduledExecutorService = timeout;
        Objects.requireNonNull(httpsURLConnection);
        ScheduledFuture<?> scheduledFutureSchedule = scheduledExecutorService.schedule(new Runnable() { // from class: nihil.sunshine.parts.SerialAccess$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                httpsURLConnection.disconnect();
            }
        }, 10L, TimeUnit.SECONDS);
        try {
            if (httpsURLConnection.getResponseCode() != 200) {
                throw new IOException("Authorization endpoint unavailable");
            }
            if (httpsURLConnection.getContentLengthLong() > 1048576) {
                throw new IOException("Authorization list too large");
            }
            InputStream inputStream = httpsURLConnection.getInputStream();
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    byte[] bArr = new byte[4096];
                    long jElapsedRealtime = SystemClock.elapsedRealtime() + 10000;
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i == -1) {
                            String string = byteArrayOutputStream.toString("UTF-8");
                            byteArrayOutputStream.close();
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            scheduledFutureSchedule.cancel(false);
                            httpsURLConnection.disconnect();
                            return string;
                        }
                        if (SystemClock.elapsedRealtime() > jElapsedRealtime || byteArrayOutputStream.size() + i > 1048576) {
                            break;
                        }
                        byteArrayOutputStream.write(bArr, 0, i);
                    }
                    throw new IOException("Authorization response limit");
                } catch (Throwable th) {
                    try {
                        byteArrayOutputStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                }
                throw th3;
            }
        } catch (Throwable th5) {
            scheduledFutureSchedule.cancel(false);
            httpsURLConnection.disconnect();
            throw th5;
        }
    }

    static boolean verify(RootOps rootOps) throws Exception {
        synchronized (lock) {
            rootOps.restoreProtectedBaseline();
            String strSerial = serial(rootOps);
            String strHash = strSerial.isEmpty() ? "" : hash(strSerial);
            if (System.getenv("TEST_ACCESS_ROOT") == null && !strSerial.isEmpty() && PassageGate.hasValidPassage(rootOps.app, rootOps)) {
                lastLease = new AccessLease(System.currentTimeMillis(), System.currentTimeMillis() + 3600000, LicenseRuntime.boot(rootOps.app), SystemClock.elapsedRealtime() + 3600000);
                return true;
            }
            Record record = read(rootOps);
            if (record == null) {
                record = new Record(AccessLease.issue(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(rootOps.app)), strHash.isEmpty() ? hash("unavailable-device") : strHash);
                write(rootOps, record);
            }
            Record record2 = record;
            boolean z = false;
            try {
                z = !strSerial.isEmpty() && SerialList.contains(download(), strSerial);
            } catch (Exception e) {
            }
            rootOps.snapshotAuthorizationTargets();
            rootOps.checkpointAccessBaseline();
            if (z) {
                record2 = new Record(AccessLease.issue(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(rootOps.app)), strHash);
                write(rootOps, record2);
                try {
                    rootOps.restoreAfterAuthorization();
                } catch (Exception e2) {
                }
                try {
                    PassageGate.grantPassage(rootOps.app, rootOps, strSerial);
                } catch (Exception e3) {
                }
            } else {
                PassageGate.invalidate();
            }
            lastLease = record2.lease;
            publish(rootOps);
            schedule(rootOps.app, record2.lease);
            if (record2.lease.remaining(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(rootOps.app)) == 0) {
                rootOps.suspendForAuthorization();
            }
            return z;
        }
    }

    static void maintain(Context context, boolean z) {
        try {
            RootOps rootOps = new RootOps(context);
            rootOps.restoreProtectedBaseline();
            if (z) {
                verify(rootOps);
                return;
            }
            synchronized (lock) {
                Record record = read(rootOps);
                if (record != null && record.lease.remaining(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(context)) == 0) {
                    rootOps.suspendForAuthorization();
                }
                if (record != null) {
                    lastLease = record.lease;
                    schedule(context, record.lease);
                    publish(rootOps);
                }
            }
        } catch (Exception e) {
            if (!z) {
                try {
                    RootOps rootOps2 = new RootOps(context);
                    rootOps2.restoreProtectedBaseline();
                    rootOps2.suspendForAuthorization();
                } catch (Exception e2) {
                }
            }
        }
    }

    private static void publish(RootOps rootOps) {
        StringBuilder sb = new StringBuilder();
        String[] strArr = {"android", "com.android.systemui", "com.sec.android.app.launcher"};
        for (int i = 0; i < 3; i++) {
            String str = strArr[i];
            sb.append("am broadcast -a ").append(ControlBus.ACTION).append(" -p ").append(str).append(" --es op access_reload --es nonce 23110000000000000000000000000001 >/dev/null 2>&1\n");
            sb.append("am broadcast -a nihil.sunshine.parts.ACCESS_CHANGED -p ").append(str).append(" >/dev/null 2>&1\n");
        }
        sb.append("am broadcast -a nihil.sunshine.parts.ACCESS_CHANGED -p nihil.sunshine.parts >/dev/null 2>&1\n");
        try {
            rootOps.run(sb.toString(), 8);
        } catch (Exception e) {
        }
    }

    private static void schedule(Context context, AccessLease accessLease) {
        if (System.getenv("TEST_ACCESS_ROOT") == null) {
            return;
        }
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
            if (alarmManager == null) {
                return;
            }
            PendingIntent broadcast = PendingIntent.getBroadcast(context, 23111, new Intent(context, (Class<?>) LicenseAlarmReceiver.class), 201326592);
            long jRemaining = accessLease.remaining(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LicenseRuntime.boot(context));
            alarmManager.setAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + (jRemaining > 30000 ? jRemaining - 30000 : 600000L), broadcast);
        } catch (Exception e) {
        }
    }

    private SerialAccess() {
    }
}
