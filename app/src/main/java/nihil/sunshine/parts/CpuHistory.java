package nihil.sunshine.parts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class CpuHistory {
    static final int CAPACITY = 301;
    private final ArrayDeque<Point> points = new ArrayDeque<>();

    CpuHistory() {
    }

    static final class Point {
        final long at;
        final List<CpuPolicy> policies;

        Point(long j, List<CpuPolicy> list) {
            this.at = j;
            this.policies = Collections.unmodifiableList(new ArrayList(list));
        }
    }

    void add(long j, List<CpuPolicy> list) {
        if (list.isEmpty()) {
            return;
        }
        if (!this.points.isEmpty() && j <= this.points.getLast().at) {
            this.points.clear();
        }
        this.points.addLast(new Point(j, list));
        while (true) {
            if (this.points.size() > CAPACITY || (!this.points.isEmpty() && j - this.points.getFirst().at > 60000)) {
                this.points.removeFirst();
            } else {
                return;
            }
        }
    }

    List<Point> points() {
        return new ArrayList(this.points);
    }

    static CpuPolicy policy(Point point, String str) {
        for (int i = 0; i < point.policies.size(); i++) {
            CpuPolicy cpuPolicy = point.policies.get(i);
            if (cpuPolicy.name.equals(str)) {
                return cpuPolicy;
            }
        }
        return null;
    }

    static CpuPolicy parse(String str) {
        if (str == null || str.length() > 1024) {
            return null;
        }
        String[] strArrSplit = str.split("\\|", -1);
        if ((strArrSplit.length != 6 && strArrSplit.length != 7) || !strArrSplit[0].matches("policy[0-9]{1,3}") || !strArrSplit[5].matches("[0-9 ,\\-]*")) {
            return null;
        }
        try {
            return new CpuPolicy(strArrSplit[0], Long.parseLong(strArrSplit[1]), Long.parseLong(strArrSplit[2]), Long.parseLong(strArrSplit[3]), Long.parseLong(strArrSplit[4]), strArrSplit[5], strArrSplit.length == 7 ? Long.parseLong(strArrSplit[6]) : 0L, -1.0d);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
