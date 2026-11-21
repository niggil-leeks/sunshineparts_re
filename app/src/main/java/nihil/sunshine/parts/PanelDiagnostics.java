package nihil.sunshine.parts;

import android.os.Debug;
import android.view.MotionEvent;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class PanelDiagnostics {
    private static final Object TOKEN = new Object();
    private static final HotScope panelState = new HotScope();
    static long stateStacksSkipped;
    static long touchLogsSkipped;

    static XC_MethodHook stateScopeHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.PanelDiagnostics.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                PanelDiagnostics.panelState.enter(PanelDiagnostics.TOKEN);
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                PanelDiagnostics.panelState.leave();
            }
        };
    }

    static XC_MethodHook stackHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.PanelDiagnostics.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (VisualPolicy.leanQs() && PanelDiagnostics.panelState.active()) {
                    PanelDiagnostics.stateStacksSkipped++;
                    methodHookParam.setResult("");
                }
            }
        };
    }

    static XC_MethodHook touchHook(final Field field, final boolean z) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.PanelDiagnostics.3
            private boolean failed;

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                if (!VisualPolicy.leanQs() || this.failed) {
                    return;
                }
                if (!(methodHookParam.args[0] instanceof MotionEvent)) {
                    return;
                }
                if (z) {
                    try {
                        field.setBoolean(methodHookParam.thisObject, ((MotionEvent) methodHookParam.args[0]).getAction() != 2);
                    } catch (Throwable th) {
                        this.failed = true;
                        ControlBus.feature("panel_touch_diagnostics", "native fallback");
                        return;
                    }
                }
                PanelDiagnostics.touchLogsSkipped++;
                methodHookParam.setResult((Object) null);
            }
        };
    }

    static void install(ClassLoader classLoader) {
        install(classLoader, null);
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            installState(classLoader, DensityQueries.pinned(str) && IconDimensions.pinned(str));
        } catch (Throwable th) {
            ControlBus.feature("panel_state_diagnostics", "native fallback");
        }
        try {
            installTouch(classLoader);
        } catch (Throwable th2) {
            ControlBus.feature("panel_touch_diagnostics", "native fallback");
        }
    }

    private static void installState(ClassLoader classLoader, boolean z) throws Exception {
        Class<?> cls = Class.forName("com.android.systemui.log.QuickPanelLogger", false, classLoader);
        Method declaredMethod = cls.getDeclaredMethod("logPanelState", String.class);
        if (z && installLeanState(cls, declaredMethod)) {
            return;
        }
        Method declaredMethod2 = Debug.class.getDeclaredMethod("getCallers", Integer.TYPE, String.class);
        if (declaredMethod.getReturnType() != Void.TYPE || declaredMethod2.getReturnType() != String.class) {
            throw new NoSuchMethodException("Panel diagnostic stack contract");
        }
        XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(declaredMethod2, stackHook());
        if (unhookHookMethod == null) {
            throw new IllegalStateException("Diagnostic hook rejected");
        }
        try {
            if (RuntimeHooks.hookMethod(declaredMethod, stateScopeHook()) == null) {
                throw new IllegalStateException("Panel diagnostic scope rejected");
            }
            ControlBus.feature("panel_state_diagnostics", "short native history");
        } catch (Throwable th) {
            RuntimeHooks.unhook(unhookHookMethod);
            throw new IllegalStateException(th);
        }
    }

    static XC_MethodHook leanStateHook(final Field field, final Field field2, final Field field3) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.PanelDiagnostics.4
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (!VisualPolicy.leanQs() || methodHookParam.args[0] == null) {
                    return;
                }
                try {
                    Object obj = field2.get(field.get(methodHookParam.thisObject));
                    if (!methodHookParam.args[0].equals(field3.get(obj))) {
                        field3.set(obj, methodHookParam.args[0]);
                        PanelDiagnostics.stateStacksSkipped++;
                    }
                    methodHookParam.setResult((Object) null);
                } catch (Throwable th) {
                }
            }
        };
    }

    private static boolean installLeanState(Class<?> cls, Method method) {
        try {
            Field declaredField = cls.getDeclaredField("quickPanelLoggerHelper");
            declaredField.setAccessible(true);
            Field declaredField2 = declaredField.getType().getDeclaredField("panelStateLogger");
            declaredField2.setAccessible(true);
            Field declaredField3 = declaredField2.getType().getDeclaredField("lastPanelState");
            declaredField3.setAccessible(true);
            if (declaredField3.getType() != String.class) {
                return false;
            }
            Method declaredMethod = cls.getDeclaredMethod("gridPanelLog", Object.class, String.class);
            if (method.getReturnType() == Void.TYPE && declaredMethod.getReturnType() == Void.TYPE) {
                RuntimeHooks.hookMethod(method, leanStateHook(declaredField, declaredField2, declaredField3));
                RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.PanelDiagnostics.5
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (VisualPolicy.leanQs()) {
                            methodHookParam.setResult((Object) null);
                        }
                    }
                });
                ControlBus.feature("panel_state_diagnostics", "Latest diagnostic state retained without stack/string/buffer work");
                return true;
            }
            return false;
        } catch (Throwable th) {
            return false;
        }
    }

    private static void installTouch(ClassLoader classLoader) throws Exception {
        Class<?> cls = Class.forName("com.android.systemui.log.QuickPanelLoggerHelper$TouchLogger", false, classLoader);
        Method declaredMethod = cls.getDeclaredMethod("log", MotionEvent.class, String.class, String.class);
        Method declaredMethod2 = cls.getDeclaredMethod("log", MotionEvent.class, String.class, String.class, Boolean.TYPE);
        Field declaredField = cls.getDeclaredField("moveEventAllowed");
        declaredField.setAccessible(true);
        if (declaredMethod.getReturnType() != Void.TYPE || declaredMethod2.getReturnType() != Void.TYPE || declaredField.getType() != Boolean.TYPE) {
            throw new NoSuchMethodException("Panel touch diagnostic contract");
        }
        XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(declaredMethod, touchHook(declaredField, true));
        if (unhookHookMethod == null) {
            throw new IllegalStateException("Touch diagnostic hook rejected");
        }
        try {
            if (RuntimeHooks.hookMethod(declaredMethod2, touchHook(declaredField, false)) == null) {
                throw new IllegalStateException("Touch diagnostic hook rejected");
            }
            ControlBus.feature("panel_touch_diagnostics", "formatting bypass");
        } catch (Throwable th) {
            RuntimeHooks.unhook(unhookHookMethod);
            throw new IllegalStateException(th);
        }
    }

    private PanelDiagnostics() {
    }
}
