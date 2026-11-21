package nihil.sunshine.parts;

import java.util.ArrayDeque;

/* JADX INFO: loaded from: classes.dex */
final class HotScope {
    private final ThreadLocal<ArrayDeque<Object>> owners = new ThreadLocal<>();

    HotScope() {
    }

    void enter(Object obj) {
        ArrayDeque<Object> arrayDeque = this.owners.get();
        if (arrayDeque == null) {
            arrayDeque = new ArrayDeque<>();
            this.owners.set(arrayDeque);
        }
        arrayDeque.push(obj);
    }

    void leave() {
        ArrayDeque<Object> arrayDeque = this.owners.get();
        if (arrayDeque == null || arrayDeque.isEmpty()) {
            return;
        }
        arrayDeque.pop();
    }

    boolean active() {
        ArrayDeque<Object> arrayDeque = this.owners.get();
        return (arrayDeque == null || arrayDeque.isEmpty()) ? false : true;
    }

    boolean is(Object obj) {
        ArrayDeque<Object> arrayDeque = this.owners.get();
        return (arrayDeque == null || arrayDeque.isEmpty() || arrayDeque.peek() != obj) ? false : true;
    }
}
