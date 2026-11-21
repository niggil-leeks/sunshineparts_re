package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class AccessLease {
    static final long HOUR = 3600000;
    final int boot;
    final long issuedWall;
    final long untilElapsed;
    final long untilWall;

    AccessLease(long j, long j2, int i, long j3) {
        this.issuedWall = j;
        this.untilWall = j2;
        this.boot = i;
        this.untilElapsed = j3;
    }

    static AccessLease issue(long j, long j2, int i) {
        return new AccessLease(j, j + HOUR, i, j2 + HOUR);
    }

    long remaining(long j, long j2, int i) {
        if (this.issuedWall <= 0 || this.untilWall <= this.issuedWall || this.untilWall - this.issuedWall != HOUR || this.untilElapsed < HOUR || j < this.issuedWall - 300000) {
            return 0L;
        }
        return Math.max(0L, Math.min(HOUR, (this.boot < 0 || this.boot != i) ? this.untilWall - j : this.untilElapsed - j2));
    }

    String encode() {
        return "1:" + this.issuedWall + ":" + this.untilWall + ":" + this.boot + ":" + this.untilElapsed;
    }

    static AccessLease parse(String str) {
        try {
            String[] strArrSplit = str.split(":", -1);
            if (strArrSplit.length == 5 && strArrSplit[0].equals("1")) {
                AccessLease accessLease = new AccessLease(Long.parseLong(strArrSplit[1]), Long.parseLong(strArrSplit[2]), Integer.parseInt(strArrSplit[3]), Long.parseLong(strArrSplit[4]));
                if (accessLease.issuedWall <= 0 || accessLease.untilWall <= accessLease.issuedWall || accessLease.untilWall - accessLease.issuedWall != HOUR || accessLease.untilElapsed < HOUR || accessLease.boot < -1) {
                    return null;
                }
                return accessLease;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private AccessLease() {
        this(0L, 0L, -1, 0L);
    }
}
