package nihil.sunshine.parts;

import java.util.Iterator;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
final class CpuFrequencies {
    private final long[] values;

    private CpuFrequencies(long[] jArr) {
        this.values = jArr;
    }

    static CpuFrequencies parse(String str) {
        if (str == null || str.length() > 4096 || str.trim().isEmpty()) {
            return null;
        }
        String[] strArrSplit = str.trim().split("\\s+");
        if (strArrSplit.length > 128) {
            return null;
        }
        TreeSet treeSet = new TreeSet();
        try {
            int i = 0;
            for (String str2 : strArrSplit) {
                long j = Long.parseLong(str2);
                if (j > 0 && j <= 10000000) {
                    treeSet.add(Long.valueOf(j));
                }
                return null;
            }
            if (treeSet.size() < 2) {
                return null;
            }
            long[] jArr = new long[treeSet.size()];
            Iterator it = treeSet.iterator();
            while (it.hasNext()) {
                jArr[i] = ((Long) it.next()).longValue();
                i++;
            }
            return new CpuFrequencies(jArr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    long minimum() {
        return this.values[0];
    }

    long maximum() {
        return this.values[this.values.length - 1];
    }

    long[] ticks() {
        TreeSet treeSet = new TreeSet();
        int i = 0;
        int i2 = 0;
        while (i2 < 5) {
            double dMinimum = minimum() + (((maximum() - minimum()) * ((long) i2)) / 4.0d);
            long j = this.values[i];
            long[] jArr = this.values;
            int length = jArr.length;
            int i3 = i;
            while (i3 < length) {
                long j2 = jArr[i3];
                int i4 = i2;
                if (Math.abs(j2 - dMinimum) < Math.abs(j - dMinimum)) {
                    j = j2;
                }
                i3++;
                i2 = i4;
            }
            treeSet.add(Long.valueOf(j));
            i2++;
            i = 0;
        }
        long[] jArr2 = new long[treeSet.size()];
        Iterator it = treeSet.iterator();
        int i5 = 0;
        while (it.hasNext()) {
            jArr2[i5] = ((Long) it.next()).longValue();
            i5++;
        }
        return jArr2;
    }
}
