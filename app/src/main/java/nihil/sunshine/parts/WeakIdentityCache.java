package nihil.sunshine.parts;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

/* JADX INFO: loaded from: classes.dex */
final class WeakIdentityCache<V> {
    private final LinkedHashMap<Key, V> entries;
    private long evictions;
    private final int limit;
    private final Lookup lookup;
    private final ReferenceQueue<Object> queue;

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
        Object owner;

        private Lookup() {
        }

        public int hashCode() {
            return System.identityHashCode(this.owner);
        }

        public boolean equals(Object obj) {
            return (obj instanceof Key) && this.owner == ((Key) obj).get();
        }
    }

    WeakIdentityCache(int i) {
        this(i, true);
    }

    WeakIdentityCache(int i, boolean z) {
        this.queue = new ReferenceQueue<>();
        this.lookup = new Lookup();
        if (i < 1) {
            throw new IllegalArgumentException();
        }
        this.limit = i;
        this.entries = new LinkedHashMap<>(16, 0.75f, z);
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

    synchronized V get(Object obj) {
        if (obj == null) {
            return null;
        }
        clean(8);
        this.lookup.owner = obj;
        try {
            V v = this.entries.get(this.lookup);
            this.lookup.owner = null;
            return v;
        } catch (Throwable th) {
            this.lookup.owner = null;
            throw th;
        }
    }

    synchronized void put(Object obj, V v) {
        if (obj == null || v == null) {
            return;
        }
        clean(32);
        this.entries.put(new Key(obj, this.queue), v);
        if (this.entries.size() > this.limit) {
            this.entries.remove(this.entries.keySet().iterator().next());
            this.evictions++;
        }
    }

    synchronized long evictions() {
        return this.evictions;
    }
}
