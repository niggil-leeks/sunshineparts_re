package nihil.sunshine.parts;

import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class CpuPlot {
    final long maximum;
    final long minimum;

    CpuPlot(CpuPolicy cpuPolicy) {
        long jMinimum;
        if (cpuPolicy == null) {
            jMinimum = 0;
        } else {
            jMinimum = cpuPolicy.frequencies != null ? cpuPolicy.frequencies.minimum() : cpuPolicy.hardwareMinimum > 0 ? cpuPolicy.hardwareMinimum : cpuPolicy.minimum;
        }
        long jMaximum = cpuPolicy != null ? cpuPolicy.frequencies != null ? cpuPolicy.frequencies.maximum() : Math.max(cpuPolicy.hardware, Math.max(cpuPolicy.maximum, cpuPolicy.current)) : 0L;
        this.minimum = jMinimum;
        this.maximum = jMaximum;
    }

    boolean valid() {
        return this.minimum > 0 && this.maximum > this.minimum;
    }

    float frequency(long j) {
        if (valid()) {
            return Math.max(0.0f, Math.min(1.0f, (j - this.minimum) / (this.maximum - this.minimum)));
        }
        return 0.0f;
    }

    static String number(long j) {
        return String.format(Locale.getDefault(), "%.2f", Double.valueOf(j / 1000000.0d));
    }
}
