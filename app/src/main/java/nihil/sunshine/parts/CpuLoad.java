package nihil.sunshine.parts;

import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class CpuLoad {
    private static final int CORES = 256;
    private final long[] totals = new long[CORES];
    private final long[] idles = new long[CORES];
    private final long[] elapsed = new long[CORES];
    private final long[] busy = new long[CORES];
    private final boolean[] baseline = new boolean[CORES];
    private final boolean[] seen = new boolean[CORES];
    private final boolean[] valid = new boolean[CORES];

    CpuLoad() {
    }

    void accept(String str) {
        if (str == null || !str.startsWith("stat|cpu") || str.length() > 512) {
            return;
        }
        String[] strArrSplit = str.split("\\|", -1);
        if (strArrSplit.length != 10) {
            return;
        }
        try {
            int i = Integer.parseInt(strArrSplit[1].substring(3));
            if (i >= 0 && i < CORES && !this.seen[i]) {
                long j = 0;
                long j2 = 0;
                for (int i2 = 2; i2 < 10; i2++) {
                    long j3 = Long.parseLong(strArrSplit[i2]);
                    if (j3 >= 0 && j3 <= 576460752303423487L) {
                        j += j3;
                        if (i2 == 5 || i2 == 6) {
                            j2 += j3;
                        }
                    }
                    return;
                }
                this.seen[i] = true;
                long j4 = j - this.totals[i];
                long j5 = j2 - this.idles[i];
                this.valid[i] = this.baseline[i] && j4 > 0 && j5 >= 0 && j5 <= j4;
                if (this.valid[i]) {
                    this.elapsed[i] = j4;
                    this.busy[i] = j4 - j5;
                }
                this.totals[i] = j;
                this.idles[i] = j2;
                this.baseline[i] = true;
            }
        } catch (NumberFormatException e) {
        }
    }

    private double load(CpuPolicy cpuPolicy) {
        boolean[] zArr = new boolean[CORES];
        double d = -1.0d;
        if (cpuPolicy.cores.isEmpty()) {
            return -1.0d;
        }
        try {
            String[] strArrSplit = cpuPolicy.cores.replace(',', ' ').trim().split("\\s+");
            int length = strArrSplit.length;
            char c = 0;
            int i = 0;
            long j = 0;
            long j2 = 0;
            while (i < length) {
                double d2 = d;
                try {
                    String[] strArrSplit2 = strArrSplit[i].split("-", -1);
                    if (strArrSplit2.length > 2) {
                        return d2;
                    }
                    int i2 = Integer.parseInt(strArrSplit2[c]);
                    int i3 = strArrSplit2.length == 2 ? Integer.parseInt(strArrSplit2[1]) : i2;
                    if (i2 >= 0 && i3 < CORES && i3 >= i2) {
                        while (i2 <= i3) {
                            if (!zArr[i2] && this.seen[i2]) {
                                zArr[i2] = true;
                                if (!this.valid[i2]) {
                                    return d2;
                                }
                                j += this.elapsed[i2];
                                j2 += this.busy[i2];
                            }
                            i2++;
                        }
                        i++;
                        d = d2;
                        c = 0;
                    }
                    return d2;
                } catch (NumberFormatException e) {
                    return d2;
                }
            }
            return j > 0 ? (j2 * 100.0d) / j : d;
        } catch (NumberFormatException e2) {
            return d;
        }
    }

    List<CpuPolicy> finish(List<CpuPolicy> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (CpuPolicy cpuPolicy : list) {
            arrayList.add(cpuPolicy.withLoad(load(cpuPolicy)));
        }
        for (int i = 0; i < CORES; i++) {
            if (!this.seen[i]) {
                this.baseline[i] = false;
            }
            this.seen[i] = false;
            this.valid[i] = false;
        }
        return arrayList;
    }
}
