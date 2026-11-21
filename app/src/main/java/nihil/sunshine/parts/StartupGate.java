package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class StartupGate {
    static final int CONNECTED = 2;
    static final int MODULE_OFFLINE = 3;
    static final int ROOT_FAILED = 0;
    static final int UNSUPPORTED = 1;

    interface Check {
        void run() throws Exception;
    }

    static final class Result {
        final Exception error;
        final int state;

        Result(int i, Exception exc) {
            this.state = i;
            this.error = exc;
        }

        boolean canEnter() {
            return this.state == StartupGate.CONNECTED || this.state == StartupGate.MODULE_OFFLINE;
        }
    }

    static Result check(Check check, Check check2, Check check3) {
        try {
            check.run();
            try {
                check2.run();
                try {
                    check3.run();
                    return new Result(CONNECTED, null);
                } catch (Exception e) {
                    return new Result(MODULE_OFFLINE, e);
                }
            } catch (Exception e2) {
                return new Result(UNSUPPORTED, e2);
            }
        } catch (Exception e3) {
            return new Result(ROOT_FAILED, e3);
        }
    }

    private StartupGate() {
    }
}
