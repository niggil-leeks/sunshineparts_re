package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes.dex */
final class SurfaceDiagnosticWork {
    static String error = "";
    private static boolean installed;
    static long skipped;

    static XC_MethodHook hook(final Field field, final Field field2, final Field field3) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.SurfaceDiagnosticWork.1
            boolean failed;

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (this.failed || !VisualPolicy.leanQs() || methodHookParam.args.length != 5 || !"apply".equals(methodHookParam.args[0]) || methodHookParam.args[1] == null) {
                    return;
                }
                try {
                    if (field.getBoolean(null)) {
                        return;
                    }
                    Object obj = methodHookParam.args[1];
                    field2.set(obj, null);
                    field3.set(obj, null);
                    methodHookParam.setResult((Object) null);
                    SurfaceDiagnosticWork.skipped++;
                } catch (Throwable th) {
                    this.failed = true;
                    SurfaceDiagnosticWork.error = th.getClass().getSimpleName();
                    ControlBus.feature("surface_diagnostic_work", "Native fallback");
                }
            }
        };
    }

    static void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        try {
            if (!FrameworkCode.inspect("/system/framework/framework.jar").surface) {
                ControlBus.feature("surface_diagnostic_work", "Native fallback: transaction method code changed");
                return;
            }
            Class<?> cls = Class.forName("android.view.SurfaceControlRegistry", false, classLoader);
            Class<?> cls2 = Class.forName("android.view.SurfaceControl$Transaction", false, classLoader);
            Class<?> cls3 = Class.forName("android.view.SurfaceControl", false, classLoader);
            Field field = FrameVoteWork.field(cls, "sCallStackDebuggingEnabled");
            Field field2 = FrameVoteWork.field(cls2, "mDebugName");
            Field field3 = FrameVoteWork.field(cls2, "mLowDebugName");
            if (field.getType() != Boolean.TYPE || !Modifier.isStatic(field.getModifiers()) || field2.getType() != String.class || field3.getType() != String.class || Modifier.isStatic(field2.getModifiers()) || Modifier.isStatic(field3.getModifiers())) {
                throw new NoSuchFieldException("Surface diagnostic ABI");
            }
            Method declaredMethod = cls.getDeclaredMethod("checkCallStackDebugging", String.class, cls2, cls3, String.class, Boolean.TYPE);
            Method declaredMethod2 = cls2.getDeclaredMethod("apply", Boolean.TYPE, Boolean.TYPE);
            if (declaredMethod.getReturnType() != Void.TYPE || declaredMethod2.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("Surface apply ABI");
            }
            QsHotPaths.deoptimize(declaredMethod2);
            if (RuntimeHooks.hookMethod(declaredMethod, hook(field, field2, field3)) == null) {
                throw new IllegalStateException("Surface diagnostic hook rejected");
            }
            installed = true;
            ControlBus.feature("surface_diagnostic_work", "Native apply preserved; named-transaction diagnostic work removed");
        } catch (Throwable th) {
            error = th.getClass().getSimpleName();
            ControlBus.feature("surface_diagnostic_work", "Native fallback");
        }
    }

    private SurfaceDiagnosticWork() {
    }
}
