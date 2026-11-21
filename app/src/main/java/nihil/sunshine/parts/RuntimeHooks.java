package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class RuntimeHooks {
    private static final List<Entry> entries = new ArrayList();
    private static final IdentityHashMap<XC_MethodHook.Unhook, Entry> handles = new IdentityHashMap<>();
    private static boolean suspended;

    private static final class Entry {
        XC_MethodHook.Unhook attached;
        final XC_MethodHook callback;
        final Set<XC_MethodHook.Unhook> handles = Collections.newSetFromMap(new IdentityHashMap());
        final Member member;
        boolean removed;

        Entry(Member member, XC_MethodHook xC_MethodHook) {
            this.member = member;
            this.callback = xC_MethodHook;
        }
    }

    public static synchronized XC_MethodHook.Unhook hookMethod(Member member, XC_MethodHook xC_MethodHook) {
        for (Entry entry : entries) {
            if (!entry.removed && entry.member.equals(member) && entry.callback == xC_MethodHook) {
                return entry.handles.iterator().next();
            }
        }
        XC_MethodHook.Unhook unhookHookMethod = XposedBridge.hookMethod(member, xC_MethodHook);
        if (unhookHookMethod == null) {
            return null;
        }
        Entry entry2 = new Entry(member, xC_MethodHook);
        entries.add(entry2);
        entry2.handles.add(unhookHookMethod);
        handles.put(unhookHookMethod, entry2);
        if (suspended) {
            unhookHookMethod.unhook();
        } else {
            entry2.attached = unhookHookMethod;
        }
        return unhookHookMethod;
    }

    public static synchronized void unhook(XC_MethodHook.Unhook unhook) {
        if (unhook == null) {
            return;
        }
        Entry entry = handles.get(unhook);
        if (entry == null) {
            unhook.unhook();
            return;
        }
        entry.removed = true;
        entries.remove(entry);
        if (entry.attached != null) {
            entry.attached.unhook();
        }
        Iterator<XC_MethodHook.Unhook> it = entry.handles.iterator();
        while (it.hasNext()) {
            handles.remove(it.next());
        }
        entry.handles.clear();
        entry.attached = null;
    }

    public static synchronized void suspend() {
        suspended = true;
        for (Entry entry : entries) {
            if (entry.attached != null) {
                try {
                    entry.attached.unhook();
                    entry.attached = null;
                } catch (Throwable th) {
                    XposedBridge.log("SunshineParts: own hook detachment unavailable");
                }
            }
        }
    }

    public static synchronized void resume() {
        if (suspended) {
            try {
                for (Entry entry : new ArrayList(entries)) {
                    if (!entry.removed) {
                        XC_MethodHook.Unhook unhookHookMethod = XposedBridge.hookMethod(entry.member, entry.callback);
                        if (unhookHookMethod == null) {
                            throw new IllegalStateException("Hook registration refused");
                        }
                        entry.attached = unhookHookMethod;
                    }
                }
                suspended = false;
            } catch (Throwable th) {
                for (Entry entry2 : entries) {
                    if (entry2.attached != null) {
                        entry2.attached.unhook();
                        entry2.attached = null;
                    }
                }
                if (!(th instanceof Error)) {
                    throw new IllegalStateException(th);
                }
                throw th;
            }
        }
    }

    static synchronized int count() {
        return entries.size();
    }

    static synchronized boolean suspended() {
        return suspended;
    }

    private RuntimeHooks() {
    }
}
