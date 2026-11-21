package nihil.sunshine.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
final class CpuClusters {
    static String name(List<CpuPolicy> list, CpuPolicy cpuPolicy) {
        if (cpuPolicy == null) {
            return "CPU group";
        }
        if (list.size() < 2 || list.size() > 3) {
            return cpuPolicy.label();
        }
        TreeSet treeSet = new TreeSet();
        for (CpuPolicy cpuPolicy2 : list) {
            if (cpuPolicy2.hardware <= 0) {
                return cpuPolicy.label();
            }
            treeSet.add(Long.valueOf(cpuPolicy2.hardware));
        }
        if (treeSet.size() != list.size()) {
            return cpuPolicy.label();
        }
        int iIndexOf = new ArrayList(treeSet).indexOf(Long.valueOf(cpuPolicy.hardware));
        if (iIndexOf == 0) {
            return "Little";
        }
        return iIndexOf == 2 ? "Prime" : "Big";
    }

    private CpuClusters() {
    }
}
