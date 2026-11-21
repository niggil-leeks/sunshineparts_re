package nihil.sunshine.parts;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
final class WeakIdentitySet {
    private final int limit;
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final HashMap<Key, Boolean> entries = new HashMap<>();
    private final Lookup lookup = new Lookup();

    private static final class Key extends WeakReference<Object> {
        final int hash;

        Key(Object obj, ReferenceQueue<Object> referenceQueue) {
            super(obj, referenceQueue);
            this.hash = System.identityHashCode(obj);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            Object obj2 = get();
            return obj2 != null && (obj instanceof Key) && obj2 == ((Key) obj).get();
        }
    }

    private static final class Lookup {
        Object value;

        private Lookup() {
        }

        public int hashCode() {
            return System.identityHashCode(this.value);
        }

        public boolean equals(Object obj) {
            return (obj instanceof Key) && this.value == ((Key) obj).get();
        }
    }

    WeakIdentitySet(int i) {
        if (i < 1) {
            throw new IllegalArgumentException();
        }
        this.limit = i;
    }

    private void clean(int i) {
        for (int i2 = 0; i2 < i; i2++) {
            Reference<? extends Object> referencePoll = this.queue.poll();
            if (referencePoll == null) {
                return;
            }
            this.entries.remove(referencePoll);
        }
    }

    synchronized boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        clean(8);
        this.lookup.value = obj;
        try {
            boolean zContainsKey = this.entries.containsKey(this.lookup);
            this.lookup.value = null;
            return zContainsKey;
        } catch (Throwable th) {
            this.lookup.value = null;
            throw th;
        }
    }

    synchronized boolean add(Object obj) {
        if (obj == null) {
            return false;
        }
        clean(32);
        this.lookup.value = obj;
        try {
            if (this.entries.containsKey(this.lookup) || this.entries.size() >= this.limit) {
                this.lookup.value = null;
                return false;
            }
            this.lookup.value = null;
            this.entries.put(new Key(obj, this.queue), Boolean.TRUE);
            return true;
        } catch (Throwable th) {
            this.lookup.value = null;
            throw th;
        }
    }
}
