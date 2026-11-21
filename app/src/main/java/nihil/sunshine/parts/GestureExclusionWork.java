package nihil.sunshine.parts;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class GestureExclusionWork {
    static long reads;

    static XC_MethodHook hook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.GestureExclusionWork.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (VisualPolicy.leanQs()) {
                    GestureExclusionWork.reads++;
                    methodHookParam.setResult(Boolean.TRUE);
                }
            }
        };
    }

    static void install(ClassLoader classLoader) {
        try {
            if (!FrameworkCode.inspect("/system/framework/framework.jar").gesture) {
                throw new IllegalStateException("Gesture method code changed");
            }
            Method declaredMethod = Class.forName("com.android.internal.hidden_from_bootclasspath.com.android.window.flags.Flags", false, classLoader).getDeclaredMethod("reduceChangedExclusionRectsMsgs", new Class[0]);
            if (!Modifier.isStatic(declaredMethod.getModifiers()) || declaredMethod.getReturnType() != Boolean.TYPE) {
                throw new NoSuchMethodException("Exclusion guard ABI");
            }
            if (Boolean.TRUE.equals(declaredMethod.invoke(null, new Object[0]))) {
                ControlBus.feature("gesture_exclusion_work", "Native unchanged-rectangle guard already enabled");
                return;
            }
            QsHotPaths.deoptimize(View.class.getDeclaredMethod("setSystemGestureExclusionRects", List.class));
            if (RuntimeHooks.hookMethod(declaredMethod, hook()) != null) {
                ControlBus.feature("gesture_exclusion_work", "Native unchanged-rectangle guard enabled");
                return;
            }
            throw new IllegalStateException("Hook rejected");
        } catch (Throwable th) {
            ControlBus.feature("gesture_exclusion_work", "native fallback");
        }
    }

    private GestureExclusionWork() {
    }
}
