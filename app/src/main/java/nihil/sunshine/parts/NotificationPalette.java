package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class NotificationPalette {
    private static final int LIMIT = 32;
    private static final int NESTING = 4;
    static long hits;
    private static final ThreadLocal<Passes> local = new ThreadLocal<>();
    static long misses;
    static long passes;

    private static final class Frame {
        final int[] colors;
        int count;
        final int[] flags;
        final int[] kinds;
        final Object[] owners;

        private Frame() {
            this.owners = new Object[NotificationPalette.LIMIT];
            this.kinds = new int[NotificationPalette.LIMIT];
            this.flags = new int[NotificationPalette.LIMIT];
            this.colors = new int[NotificationPalette.LIMIT];
        }

        int find(Object obj, int i, int i2) {
            for (int i3 = 0; i3 < this.count; i3++) {
                if (this.owners[i3] == obj && this.kinds[i3] == i && this.flags[i3] == i2) {
                    return i3;
                }
            }
            return -1;
        }

        void clear() {
            for (int i = 0; i < this.count; i++) {
                this.owners[i] = null;
            }
            this.count = 0;
        }
    }

    private static final class Passes {
        int depth;
        final Frame[] frames;

        private Passes() {
            this.frames = new Frame[NotificationPalette.NESTING];
        }

        Frame current() {
            if (this.depth <= 0 || this.depth > NotificationPalette.NESTING) {
                return null;
            }
            return this.frames[this.depth - 1];
        }

        void begin() {
            int i = this.depth + 1;
            this.depth = i;
            if (i <= NotificationPalette.NESTING) {
                if (this.frames[this.depth - 1] == null) {
                    this.frames[this.depth - 1] = new Frame();
                }
                this.frames[this.depth - 1].clear();
            }
        }

        void end() {
            Frame frameCurrent = current();
            if (frameCurrent != null) {
                frameCurrent.clear();
            }
            if (this.depth > 0) {
                this.depth--;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Frame current() {
        Passes passes2 = local.get();
        if (passes2 == null) {
            return null;
        }
        return passes2.current();
    }

    static XC_MethodHook passHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.NotificationPalette.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Passes passes2 = (Passes) NotificationPalette.local.get();
                if (passes2 == null) {
                    passes2 = new Passes();
                    NotificationPalette.local.set(passes2);
                }
                passes2.begin();
                NotificationPalette.passes++;
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                ((Passes) NotificationPalette.local.get()).end();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int flags(Object[] objArr) {
        return (((Boolean) objArr[1]).booleanValue() ? 1 : 0) | (((Boolean) objArr[2]).booleanValue() ? 2 : 0);
    }

    static XC_MethodHook colorHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.NotificationPalette.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Frame frameCurrent;
                if (VisualPolicy.leanQs() && (frameCurrent = NotificationPalette.current()) != null) {
                    int iFind = frameCurrent.find(methodHookParam.thisObject, ((Integer) methodHookParam.args[0]).intValue(), NotificationPalette.flags(methodHookParam.args));
                    if (iFind >= 0) {
                        NotificationPalette.hits++;
                        methodHookParam.setResult(Integer.valueOf(frameCurrent.colors[iFind]));
                    } else {
                        NotificationPalette.misses++;
                    }
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Frame frameCurrent;
                if (!VisualPolicy.leanQs() || methodHookParam.hasThrowable() || !(methodHookParam.getResult() instanceof Integer) || (frameCurrent = NotificationPalette.current()) == null) {
                    return;
                }
                int iIntValue = ((Integer) methodHookParam.args[0]).intValue();
                int iFlags = NotificationPalette.flags(methodHookParam.args);
                int iFind = frameCurrent.find(methodHookParam.thisObject, iIntValue, iFlags);
                if (iFind < 0) {
                    if (frameCurrent.count == NotificationPalette.LIMIT) {
                        return;
                    }
                    iFind = frameCurrent.count;
                    frameCurrent.count = iFind + 1;
                    frameCurrent.owners[iFind] = methodHookParam.thisObject;
                    frameCurrent.kinds[iFind] = iIntValue;
                    frameCurrent.flags[iFind] = iFlags;
                }
                frameCurrent.colors[iFind] = ((Integer) methodHookParam.getResult()).intValue();
            }
        };
    }

    static void install(ClassLoader classLoader) {
        try {
            Class<?> cls = Class.forName("noticolorpicker.NotificationColorPicker", false, classLoader);
            Class<?> cls2 = Class.forName("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout$1", false, classLoader);
            Method declaredMethod = cls.getDeclaredMethod("getTextColor", Integer.TYPE, Boolean.TYPE, Boolean.TYPE);
            Method declaredMethod2 = cls2.getDeclaredMethod("onPreDraw", new Class[0]);
            if (declaredMethod.getReturnType() != Integer.TYPE || declaredMethod2.getReturnType() != Boolean.TYPE) {
                throw new NoSuchMethodException("Notification palette contract");
            }
            if (RuntimeHooks.hookMethod(declaredMethod, colorHook()) != null && RuntimeHooks.hookMethod(declaredMethod2, passHook()) != null) {
                ControlBus.feature("notification_palette", "per-pass cache");
                return;
            }
            throw new IllegalStateException("Palette hook rejected");
        } catch (Throwable th) {
            if (0 != 0) {
                RuntimeHooks.unhook(null);
            }
            ControlBus.feature("notification_palette", "native fallback");
        }
    }

    private NotificationPalette() {
    }
}
