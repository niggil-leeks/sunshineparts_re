package nihil.sunshine.parts;

import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class CpuPolicy {
    final String cores;
    final long current;
    final CpuFrequencies frequencies;
    final long hardware;
    final long hardwareMinimum;
    final double load;
    final long maximum;
    final long minimum;
    final String name;

    CpuPolicy(String str, long j, long j2, long j3, long j4, String str2) {
        this(str, j, j2, j3, j4, str2, 0L, -1.0d);
    }

    CpuPolicy(String str, long j, long j2, long j3, long j4, String str2, long j5, double d) {
        this(str, j, j2, j3, j4, str2, j5, d, null);
    }

    private CpuPolicy(String str, long j, long j2, long j3, long j4, String str2, long j5, double d, CpuFrequencies cpuFrequencies) {
        this.name = str;
        this.minimum = valid(j);
        this.maximum = valid(j2);
        this.current = valid(j3);
        this.hardware = valid(j4);
        this.cores = str2;
        this.hardwareMinimum = valid(j5);
        this.load = (!Double.isFinite(d) || d < 0.0d || d > 100.0d) ? -1.0d : d;
        this.frequencies = cpuFrequencies;
    }

    CpuPolicy withLoad(double d) {
        return new CpuPolicy(this.name, this.minimum, this.maximum, this.current, this.hardware, this.cores, this.hardwareMinimum, d, this.frequencies);
    }

    CpuPolicy withFrequencies(CpuFrequencies cpuFrequencies) {
        return new CpuPolicy(this.name, this.minimum, this.maximum, this.current, this.hardware, this.cores, this.hardwareMinimum, this.load, cpuFrequencies);
    }

    private static long valid(long j) {
        if (j <= 0 || j > 10000000) {
            return 0L;
        }
        return j;
    }

    float currentFraction() {
        long jMax = this.hardware > 0 ? this.hardware : Math.max(this.maximum, this.current);
        if (jMax > 0) {
            return Math.min(1.0f, this.current / jMax);
        }
        return 0.0f;
    }

    float limitFraction() {
        long jMax = this.hardware > 0 ? this.hardware : Math.max(this.maximum, this.current);
        if (jMax > 0) {
            return Math.min(1.0f, this.maximum / jMax);
        }
        return 0.0f;
    }

    static String ghz(long j) {
        return j > 0 ? String.format(Locale.getDefault(), "%.2f GHz", Float.valueOf(j / 1000000.0f)) : "Not reported";
    }

    String label() {
        return this.cores.isEmpty() ? this.name : "CPU " + this.cores;
    }
}
