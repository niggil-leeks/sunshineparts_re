package nihil.sunshine.parts;

import java.util.ArrayDeque;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
final class SoundAliveQueue<K, V> {
    private final Backend<K, V> backend;
    private final Dispatch dispatch;
    private final ArrayDeque<Entry<K, V>> pending = new ArrayDeque<>();
    private Job<K, V> running;

    interface Dispatch {
        void main(Runnable runnable);

        long now();

        void worker(Runnable runnable);
    }

    interface Backend<K, V> {
        void publish(Entry<K, V> entry);

        V read(K k) throws Exception;

        default boolean current(Entry<K, V> entry, K k) {
            return true;
        }
    }

    static final class Entry<K, V> {
        boolean active;
        boolean dirty = true;
        long generation;
        K key;
        boolean queued;
        long retryAfter;
        V snapshot;

        Entry() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Job<K, V> {
        final Entry<K, V> entry;
        final long generation;
        final K key;

        Job(Entry<K, V> entry) {
            this.entry = entry;
            this.key = entry.key;
            this.generation = entry.generation;
        }
    }

    SoundAliveQueue(Backend<K, V> backend, Dispatch dispatch) {
        this.backend = backend;
        this.dispatch = dispatch;
    }

    void open(Entry<K, V> entry, K k) {
        boolean zEquals = Objects.equals(entry.key, k);
        if (!entry.active || !zEquals) {
            entry.generation++;
            entry.dirty = true;
            entry.retryAfter = 0L;
        }
        if (!zEquals) {
            entry.snapshot = null;
        }
        entry.key = k;
        entry.active = true;
    }

    void close(Entry<K, V> entry) {
        entry.active = false;
        entry.generation++;
        entry.dirty = true;
        entry.queued = false;
        this.pending.remove(entry);
    }

    void invalidate(Entry<K, V> entry, boolean z) {
        entry.generation++;
        entry.dirty = true;
        entry.retryAfter = 0L;
        if (z) {
            entry.snapshot = null;
        }
    }

    void request(Entry<K, V> entry) {
        if (!entry.active || entry.key == null || !entry.dirty || this.dispatch.now() < entry.retryAfter) {
            return;
        }
        if (this.running != null && this.running.entry == entry && this.running.generation == entry.generation) {
            return;
        }
        if (!entry.queued) {
            if (this.pending.size() >= 8) {
                return;
            }
            entry.queued = true;
            this.pending.add(entry);
        }
        pump();
    }

    private void pump() {
        if (this.running != null) {
            return;
        }
        while (!this.pending.isEmpty()) {
            Entry<K, V> entryRemoveFirst = this.pending.removeFirst();
            entryRemoveFirst.queued = false;
            if (entryRemoveFirst.active && entryRemoveFirst.dirty && this.dispatch.now() >= entryRemoveFirst.retryAfter) {
                final Job<K, V> job = new Job<>(entryRemoveFirst);
                this.running = job;
                this.dispatch.worker(new Runnable() { // from class: nihil.sunshine.parts.SoundAliveQueue$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m111lambda$pump$1$nihilsunshinepartsSoundAliveQueue(job);
                    }
                });
                return;
            }
        }
    }

    /* JADX INFO: renamed from: lambda$pump$1$nihil-sunshine-parts-SoundAliveQueue, reason: not valid java name */
    /* synthetic */ void m111lambda$pump$1$nihilsunshinepartsSoundAliveQueue(final Job job) {
        final V v;
        try {
            v = this.backend.read(job.key);
        } catch (Exception e) {
            v = null;
        }
        this.dispatch.main(new Runnable() { // from class: nihil.sunshine.parts.SoundAliveQueue$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m110lambda$pump$0$nihilsunshinepartsSoundAliveQueue(job, v);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: complete, reason: merged with bridge method [inline-methods] */
    public void m110lambda$pump$0$nihilsunshinepartsSoundAliveQueue(Job<K, V> job, V v) {
        if (this.running != job) {
            return;
        }
        this.running = null;
        Entry<K, V> entry = job.entry;
        if (entry.active && entry.generation == job.generation && Objects.equals(entry.key, job.key) && this.backend.current(entry, job.key)) {
            if (v != null) {
                entry.snapshot = v;
                entry.dirty = false;
                entry.retryAfter = 0L;
                this.backend.publish(entry);
            } else {
                entry.dirty = true;
                entry.retryAfter = this.dispatch.now() + 1000;
            }
        }
        pump();
    }
}
