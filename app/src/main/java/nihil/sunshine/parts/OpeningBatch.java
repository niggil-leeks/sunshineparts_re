package nihil.sunshine.parts;

import android.os.Looper;
import android.view.MotionEvent;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class OpeningBatch {
    private static final String ENTRY = "sunshine.opening.window.batch";
    static long batches;
    static long deferredCalls;
    static long flushCalls;
    static long multiRequestBatches;
    private static final Object[] NO_ARGS = new Object[0];
    private static final ThreadLocal<Entry> active = new ThreadLocal<>();
    static String error = "";

    static final class Access {
        final Method apply;
        final Field barState;
        final Field defer;
        final Field fraction;
        final Field owner;
        final Field window;
        final Class<?> windowClass;

        private static Field field(Class<?> cls, String str) throws Exception {
            Field declaredField = cls.getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        }

        Access(Class<?> cls, Class<?> cls2, Class<?> cls3) throws Exception {
            this.windowClass = cls3;
            this.owner = field(cls, "this$0");
            this.window = field(cls2, "mNotificationShadeWindowController");
            this.fraction = field(cls2, "mExpandedFraction");
            this.barState = field(cls2, "mBarState");
            this.defer = field(cls3, "mDeferWindowLayoutParams");
            this.apply = cls3.getDeclaredMethod("applyWindowLayoutParams", new Class[0]);
            this.apply.setAccessible(true);
            if (this.owner.getType() != cls2 || this.fraction.getType() != Float.TYPE || this.barState.getType() != Integer.TYPE || this.defer.getType() != Integer.TYPE || this.apply.getReturnType() != Void.TYPE) {
                throw new NoSuchFieldException("Opening batch ABI");
            }
        }
    }

    private static final class Entry {
        final Entry parent;
        int requests;
        final Object window;

        Entry(Object obj, Entry entry) {
            this.window = obj;
            this.parent = entry;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Throwable cause(Throwable th) {
        return (!(th instanceof InvocationTargetException) || th.getCause() == null) ? th : th.getCause();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void fallback(Throwable th) {
        error = cause(th).getClass().getSimpleName();
        ControlBus.feature("opening_window_batch", "native fallback");
    }

    static XC_MethodHook touchHook(final Access access) {
        return new XC_MethodHook(Integer.MIN_VALUE) { // from class: nihil.sunshine.parts.OpeningBatch.1
            private boolean disabled;

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Entry entry;
                Object obj;
                if (this.disabled || !Config.leanQs || Looper.myLooper() != Looper.getMainLooper() || methodHookParam.args == null || methodHookParam.args.length != 1 || !(methodHookParam.args[0] instanceof MotionEvent)) {
                    return;
                }
                int actionMasked = ((MotionEvent) methodHookParam.args[0]).getActionMasked();
                if (actionMasked != 0 && actionMasked != 2) {
                    return;
                }
                try {
                    Object obj2 = access.owner.get(methodHookParam.thisObject);
                    if (obj2 != null && access.barState.getInt(obj2) == 0 && access.fraction.getFloat(obj2) == 0.0f && (obj = access.window.get(obj2)) != null && obj.getClass() == access.windowClass && access.defer.getInt(obj) == 0) {
                        entry = new Entry(obj, (Entry) OpeningBatch.active.get());
                        try {
                            methodHookParam.setObjectExtra(OpeningBatch.ENTRY, entry);
                            access.defer.setInt(obj, 1);
                            OpeningBatch.active.set(entry);
                            OpeningBatch.batches++;
                        } catch (Throwable th) {
                            th = th;
                            if (entry != null) {
                                try {
                                    if (access.defer.getInt(entry.window) == 1) {
                                        access.defer.setInt(entry.window, 0);
                                    }
                                } catch (Throwable th2) {
                                }
                                methodHookParam.setObjectExtra(OpeningBatch.ENTRY, (Object) null);
                                OpeningBatch.active.set(entry.parent);
                            }
                            this.disabled = true;
                            OpeningBatch.fallback(th);
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    entry = null;
                }
            }

            /* JADX WARN: Code duplicated, block: B:21:0x0074 A[DONT_GENERATE] */
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Entry entry = (Entry) methodHookParam.getObjectExtra(OpeningBatch.ENTRY);
                if (entry == null) {
                    return;
                }
                methodHookParam.setObjectExtra(OpeningBatch.ENTRY, (Object) null);
                try {
                    int i = access.defer.getInt(entry.window);
                    if (i >= 1) {
                        access.defer.setInt(entry.window, i - 1);
                    } else {
                        this.disabled = true;
                        OpeningBatch.fallback(new IllegalStateException("Native batch depth changed"));
                    }
                    OpeningBatch.active.set(entry.parent);
                    OpeningBatch.flushCalls++;
                    access.apply.invoke(entry.window, OpeningBatch.NO_ARGS);
                    if (entry.requests > 1) {
                    }
                } catch (Throwable th) {
                    try {
                        this.disabled = true;
                        OpeningBatch.fallback(th);
                        Throwable thCause = OpeningBatch.cause(th);
                        if (methodHookParam.hasThrowable()) {
                            methodHookParam.getThrowable().addSuppressed(thCause);
                        } else {
                            methodHookParam.setThrowable(thCause);
                        }
                    } finally {
                        if (entry.requests > 1) {
                            OpeningBatch.multiRequestBatches++;
                        }
                        OpeningBatch.active.set(entry.parent);
                    }
                }
            }
        };
    }

    static XC_MethodHook requestHook() {
        return new XC_MethodHook(Integer.MIN_VALUE) { // from class: nihil.sunshine.parts.OpeningBatch.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Entry entry = (Entry) OpeningBatch.active.get();
                if (entry == null || entry.window != methodHookParam.thisObject) {
                    return;
                }
                entry.requests++;
                OpeningBatch.deferredCalls++;
            }
        };
    }

    static void install(ClassLoader classLoader, String str) {
        ArrayList<XC_MethodHook.Unhook> arrayList = new ArrayList();
        try {
            if (!IconDimensions.pinned(str)) {
                throw new IllegalStateException("SystemUI DEX differs from inspected target");
            }
            Class<?> cls = Class.forName("com.android.systemui.shade.NotificationPanelViewController", false, classLoader);
            Class<?> cls2 = Class.forName("com.android.systemui.shade.NotificationPanelViewController$TouchHandler", false, classLoader);
            Class<?> cls3 = Class.forName("com.android.systemui.shade.NotificationShadeWindowControllerImpl", false, classLoader);
            Access access = new Access(cls2, cls, cls3);
            Method declaredMethod = cls2.getDeclaredMethod("handleTouch$1", MotionEvent.class);
            Method declaredMethod2 = cls3.getDeclaredMethod("batchApplyWindowLayoutParams", Runnable.class);
            if (declaredMethod.getReturnType() != Boolean.TYPE || declaredMethod2.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("Native input/batch contract");
            }
            declaredMethod.setAccessible(true);
            arrayList.add(RuntimeHooks.hookMethod(access.apply, requestHook()));
            arrayList.add(RuntimeHooks.hookMethod(declaredMethod, touchHook(access)));
            if (arrayList.contains(null)) {
                throw new IllegalStateException("Opening hook rejected");
            }
            ControlBus.feature("opening_window_batch", "collapsed input batch");
        } catch (Throwable th) {
            for (XC_MethodHook.Unhook unhook : arrayList) {
                if (unhook != null) {
                    RuntimeHooks.unhook(unhook);
                }
            }
            fallback(th);
        }
    }

    private OpeningBatch() {
    }
}
