package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class DensityQueries {
    private static final String MISS = "sunshine.density.query.frame";
    private static final int NESTING = 4;
    private static final int OWNERS = 4;
    static long frames;
    static long hits;
    static long nativeLoads;
    private static final ThreadLocal<Passes> local = new ThreadLocal<>();
    static String error = "";

    private static final class Frame {
        int count;
        final Object[] owners;
        final Integer[][] values;

        private Frame() {
            this.owners = new Object[4];
            this.values = (Integer[][]) Array.newInstance((Class<?>) Integer.class, 4, 2);
        }

        int find(Object obj) {
            for (int i = 0; i < this.count; i++) {
                if (this.owners[i] == obj) {
                    return i;
                }
            }
            return -1;
        }

        void clear() {
            for (int i = 0; i < this.count; i++) {
                this.owners[i] = null;
                this.values[i][0] = null;
                this.values[i][1] = null;
            }
            this.count = 0;
        }
    }

    private static final class Passes {
        int depth;
        final Frame[] frames;

        private Passes() {
            this.frames = new Frame[4];
        }

        Frame current() {
            if (this.depth <= 0 || this.depth > 4) {
                return null;
            }
            return this.frames[this.depth - 1];
        }

        void begin() {
            int i = this.depth + 1;
            this.depth = i;
            if (i <= 4) {
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
        Passes passes = local.get();
        if (passes == null) {
            return null;
        }
        return passes.current();
    }

    static XC_MethodHook frameHook() {
        return new XC_MethodHook(Integer.MIN_VALUE) { // from class: nihil.sunshine.parts.DensityQueries.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Passes passes = (Passes) DensityQueries.local.get();
                if (passes == null) {
                    passes = new Passes();
                    DensityQueries.local.set(passes);
                }
                passes.begin();
                DensityQueries.frames++;
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Passes passes = (Passes) DensityQueries.local.get();
                if (passes != null) {
                    passes.end();
                }
            }
        };
    }

    static XC_MethodHook queryHook(final int i) {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Density query slot");
        }
        return new XC_MethodHook(Integer.MIN_VALUE) { // from class: nihil.sunshine.parts.DensityQueries.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Frame frameCurrent;
                if (Config.leanQs && methodHookParam.thisObject != null && methodHookParam.args != null && methodHookParam.args.length == 1 && (methodHookParam.args[0] instanceof Integer) && ((Integer) methodHookParam.args[0]).intValue() == 0 && (frameCurrent = DensityQueries.current()) != null) {
                    int iFind = frameCurrent.find(methodHookParam.thisObject);
                    if (iFind >= 0 && frameCurrent.values[iFind][i] != null) {
                        DensityQueries.hits++;
                        methodHookParam.setResult(frameCurrent.values[iFind][i]);
                    } else if (iFind >= 0 || frameCurrent.count < 4) {
                        methodHookParam.setObjectExtra(DensityQueries.MISS, frameCurrent);
                    }
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Frame frame = (Frame) methodHookParam.getObjectExtra(DensityQueries.MISS);
                if (frame == null) {
                    return;
                }
                methodHookParam.setObjectExtra(DensityQueries.MISS, (Object) null);
                if (!Config.leanQs || DensityQueries.current() != frame || methodHookParam.hasThrowable() || !(methodHookParam.getResult() instanceof Integer)) {
                    return;
                }
                Integer num = (Integer) methodHookParam.getResult();
                if (num.intValue() <= 0) {
                    return;
                }
                int iFind = frame.find(methodHookParam.thisObject);
                if (iFind < 0) {
                    if (frame.count == 4) {
                        return;
                    }
                    iFind = frame.count;
                    frame.count = iFind + 1;
                    frame.owners[iFind] = methodHookParam.thisObject;
                }
                if (frame.values[iFind][i] == null) {
                    frame.values[iFind][i] = num;
                    DensityQueries.nativeLoads++;
                }
            }
        };
    }

    static boolean pinned(String str) {
        return NativeTargets.systemUi(str);
    }

    static void install(ClassLoader classLoader, String str) {
        ArrayList<XC_MethodHook.Unhook> arrayList = new ArrayList();
        try {
            if (!pinned(str)) {
                throw new IllegalStateException("SystemUI DEX differs from inspected target");
            }
            Class<?> cls = Class.forName("androidx.compose.ui.platform.AndroidUiDispatcher$dispatchCallback$1", false, classLoader);
            Class<?> cls2 = Class.forName("android.view.IWindowManager$Stub$Proxy", false, classLoader);
            Method declaredMethod = cls.getDeclaredMethod("doFrame", Long.TYPE);
            Method declaredMethod2 = cls2.getDeclaredMethod("getInitialDisplayDensity", Integer.TYPE);
            Method declaredMethod3 = cls2.getDeclaredMethod("getBaseDisplayDensity", Integer.TYPE);
            if (declaredMethod.getReturnType() != Void.TYPE || declaredMethod2.getReturnType() != Integer.TYPE || declaredMethod3.getReturnType() != Integer.TYPE || Modifier.isStatic(declaredMethod2.getModifiers()) || Modifier.isStatic(declaredMethod3.getModifiers())) {
                throw new NoSuchMethodException("Density query ABI");
            }
            Method[] methodArr = {declaredMethod, declaredMethod2, declaredMethod3};
            for (int i = 0; i < 3; i++) {
                methodArr[i].setAccessible(true);
            }
            arrayList.add(RuntimeHooks.hookMethod(declaredMethod2, queryHook(0)));
            arrayList.add(RuntimeHooks.hookMethod(declaredMethod3, queryHook(1)));
            arrayList.add(RuntimeHooks.hookMethod(declaredMethod, frameHook()));
            if (!arrayList.contains(null)) {
                ControlBus.feature("density_queries", "Compose frame cache");
                return;
            }
            throw new IllegalStateException("Density query hook rejected");
        } catch (Throwable th) {
            for (XC_MethodHook.Unhook unhook : arrayList) {
                if (unhook != null) {
                    RuntimeHooks.unhook(unhook);
                }
            }
            error = th.getClass().getSimpleName();
            ControlBus.feature("density_queries", "native fallback");
        }
    }

    private DensityQueries() {
    }
}
