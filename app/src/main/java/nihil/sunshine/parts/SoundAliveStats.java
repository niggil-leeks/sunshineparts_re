package nihil.sunshine.parts;

import android.os.Trace;
import java.lang.reflect.Array;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class SoundAliveStats {
    static final int APPLY = 3;
    private static final int LIMIT = 64;
    private static final String[] NAMES = {"update_main", "query_main", "query_worker", "apply_main"};
    static final int QUERY_MAIN = 1;
    static final int QUERY_WORKER = 2;
    static final int UPDATE = 0;
    private long epoch;
    private boolean mode;
    private final long[][][] times = (long[][][]) Array.newInstance((Class<?>) Long.TYPE, QUERY_WORKER, 4, LIMIT);
    private final int[][] count = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, QUERY_WORKER, 4);
    private final long[] cached = new long[QUERY_WORKER];
    private final long[] deferred = new long[QUERY_WORKER];
    private final long[] failures = new long[QUERY_WORKER];
    private final long[] reads = new long[QUERY_WORKER];

    SoundAliveStats() {
    }

    synchronized long begin(int i) {
        if (this.count[this.mode ? (char) 1 : (char) 0][i] >= LIMIT) {
            return 0L;
        }
        Trace.beginSection("SunshineSA/" + NAMES[i]);
        return System.nanoTime();
    }

    synchronized long begin(int i, long j) {
        return j == this.epoch ? begin(i) : 0L;
    }

    synchronized void end(int i, long j, long j2) {
        boolean z;
        if (j != 0) {
            Trace.endSection();
            z = this.mode;
            if (j != 0 && j2 == this.epoch && this.count[z ? 1 : 0][i] < LIMIT) {
                long[] jArr = this.times[z ? 1 : 0][i];
                int[] iArr = this.count[z ? 1 : 0];
                int i2 = iArr[i];
                iArr[i] = i2 + QUERY_MAIN;
                jArr[i2] = Math.max(0L, System.nanoTime() - j);
            }
        } else {
            z = this.mode;
            if (j != 0) {
                long[] jArr2 = this.times[z ? 1 : 0][i];
                int[] iArr2 = this.count[z ? 1 : 0];
                int i3 = iArr2[i];
                iArr2[i] = i3 + QUERY_MAIN;
                jArr2[i3] = Math.max(0L, System.nanoTime() - j);
            }
        }
        throw th;
    }

    synchronized long epoch() {
        return this.epoch;
    }

    synchronized void mode(boolean z) {
        if (this.mode != z) {
            this.epoch++;
            this.mode = z;
            char c = z ? (char) 1 : (char) 0;
            Arrays.fill(this.count[c], 0);
            long[][] jArr = this.times[c];
            int length = jArr.length;
            for (int i = 0; i < length; i += QUERY_MAIN) {
                Arrays.fill(jArr[i], 0L);
            }
            long[] jArr2 = this.cached;
            long[] jArr3 = this.deferred;
            long[] jArr4 = this.failures;
            this.reads[c] = 0;
            jArr4[c] = 0;
            jArr3[c] = 0;
            jArr2[c] = 0;
        }
    }

    synchronized void cache() {
        long[] jArr = this.cached;
        char c = this.mode ? (char) 1 : (char) 0;
        jArr[c] = jArr[c] + 1;
    }

    synchronized void defer() {
        long[] jArr = this.deferred;
        char c = this.mode ? (char) 1 : (char) 0;
        jArr[c] = jArr[c] + 1;
    }

    synchronized void failure() {
        long[] jArr = this.failures;
        char c = this.mode ? (char) 1 : (char) 0;
        jArr[c] = jArr[c] + 1;
    }

    synchronized void read(long j) {
        if (j == this.epoch) {
            long[] jArr = this.reads;
            char c = this.mode ? (char) 1 : (char) 0;
            jArr[c] = jArr[c] + 1;
        }
    }

    synchronized void failure(long j) {
        if (j == this.epoch) {
            long[] jArr = this.failures;
            char c = this.mode ? (char) 1 : (char) 0;
            jArr[c] = jArr[c] + 1;
        }
    }

    synchronized JSONObject snapshot(boolean z) throws JSONException {
        JSONObject jSONObject;
        jSONObject = new JSONObject();
        jSONObject.put("matched", z);
        jSONObject.put("enabled", this.mode);
        jSONObject.put("method", "SoundCraftViewModel.updateModel");
        jSONObject.put("authority", "com.sec.android.app.soundalive.compatibility.SAContentProvider");
        jSONObject.put("measurement_limit_per_path", LIMIT);
        jSONObject.put("time_basis", "elapsed_ns");
        jSONObject.put("trace_prefix", "SunshineSA/");
        int i = 0;
        while (i < QUERY_WORKER) {
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("cached_reads", this.cached[i]);
            jSONObject2.put("deferred_updates", this.deferred[i]);
            jSONObject2.put("read_failures", this.failures[i]);
            jSONObject2.put("worker_reads", this.reads[i]);
            for (int i2 = 0; i2 < 4; i2 += QUERY_MAIN) {
                long[] jArrCopyOf = Arrays.copyOf(this.times[i][i2], this.count[i][i2]);
                Arrays.sort(jArrCopyOf);
                JSONObject jSONObject3 = new JSONObject();
                jSONObject3.put("samples", jArrCopyOf.length);
                jSONObject3.put("median_ns", jArrCopyOf.length == 0 ? 0L : jArrCopyOf[(jArrCopyOf.length - 1) / QUERY_WORKER]);
                jSONObject3.put("p95_ns", jArrCopyOf.length == 0 ? 0L : jArrCopyOf[((int) Math.ceil(((double) jArrCopyOf.length) * 0.95d)) - 1]);
                jSONObject3.put("max_ns", jArrCopyOf.length != 0 ? jArrCopyOf[jArrCopyOf.length - 1] : 0L);
                jSONObject2.put(NAMES[i2], jSONObject3);
            }
            jSONObject.put(i == 0 ? "original" : "async", jSONObject2);
            i += QUERY_MAIN;
        }
        return jSONObject;
    }
}
