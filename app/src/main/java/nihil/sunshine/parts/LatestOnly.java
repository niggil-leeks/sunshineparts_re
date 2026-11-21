package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class LatestOnly<T> {
    private Work<T> active;
    private T pending;
    private long serial;

    LatestOnly() {
    }

    static final class Work<T> {
        final long serial;
        final T value;

        Work(long j, T t) {
            this.serial = j;
            this.value = t;
        }
    }

    static final class Completion<T> {
        final boolean current;
        final Work<T> next;

        Completion(boolean z, Work<T> work) {
            this.current = z;
            this.next = work;
        }
    }

    synchronized Work<T> offer(T t) {
        try {
            if (t == null) {
                throw new IllegalArgumentException("Null work");
            }
            this.serial++;
            if (this.active != null) {
                this.pending = t;
                return null;
            }
            Work<T> work = new Work<>(this.serial, t);
            this.active = work;
            return work;
        } catch (Throwable th) {
            throw th;
        }
    }

    synchronized boolean current(Work<T> work) {
        return this.active == work && this.serial == work.serial;
    }

    synchronized Completion<T> complete(Work<T> work) {
        boolean z;
        Work<T> work2;
        if (this.active != work) {
            throw new IllegalStateException("Unexpected completion");
        }
        z = this.serial == work.serial;
        work2 = null;
        this.active = null;
        if (this.pending != null) {
            Work<T> work3 = new Work<>(this.serial, this.pending);
            this.active = work3;
            this.pending = null;
            work2 = work3;
        }
        return new Completion<>(z, work2);
    }

    synchronized void invalidate() {
        this.serial++;
        this.pending = null;
    }

    synchronized int pendingCount() {
        return this.pending == null ? 0 : 1;
    }
}
