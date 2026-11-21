package nihil.lain.sunshine;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
final class FlexGuard {
    static final String CLASS = "com.android.wm.shell.flexpanel.FlexPanelStartController";
    private final AtomicLong avoided = new AtomicLong();
    private final Method changed;
    private final Method clear;
    private final Field context;
    private final Method executable;
    private volatile boolean failed;
    private final Field manager;
    private final Method subDisplay;

    FlexGuard(ClassLoader classLoader, boolean z) throws Exception {
        if (!z) {
            throw new IllegalArgumentException("Unverified Flex DEX");
        }
        Class<?> cls = Class.forName(CLASS, false, classLoader);
        Field field = SunshineModule.field(cls, "mSemStatusBarManager");
        this.manager = field;
        Field field2 = SunshineModule.field(cls, "mContext");
        this.context = field2;
        if (field.getType().getName().equals("android.app.SemStatusBarManager") && field2.getType() == Context.class) {
            Method method = SunshineModule.method(classLoader, CLASS, "onFlexModeChanged", new Class[0]);
            this.changed = method;
            Method method2 = SunshineModule.method(classLoader, CLASS, "clearMessages", new Class[0]);
            this.clear = method2;
            Method method3 = SunshineModule.method(classLoader, CLASS, "isExecutableState", new Class[0]);
            this.executable = method3;
            Method method4 = SunshineModule.method(classLoader, "com.samsung.android.multiwindow.MultiWindowUtils", "isInSubDisplay", Context.class);
            this.subDisplay = method4;
            if (method.getReturnType() != Void.TYPE || method2.getReturnType() != Void.TYPE || method3.getReturnType() != Boolean.TYPE || method4.getReturnType() != Boolean.TYPE) {
                throw new NoSuchMethodException("Flex return type");
            }
            return;
        }
        throw new NoSuchFieldException("Flex field types");
    }

    void install() {
        RuntimeHooks.hookMethod(this.changed, new XC_MethodHook() { // from class: nihil.lain.sunshine.FlexGuard.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (FlexGuard.this.failed) {
                    return;
                }
                try {
                    if (((Boolean) FlexGuard.this.subDisplay.invoke(null, FlexGuard.this.context.get(methodHookParam.thisObject))).booleanValue()) {
                        return;
                    }
                    FlexGuard.this.clear.invoke(methodHookParam.thisObject, null);
                    if (FlexGuard.this.manager.get(methodHookParam.thisObject) != null) {
                        FlexGuard.this.executable.invoke(methodHookParam.thisObject, null);
                    }
                    FlexGuard.this.avoided.incrementAndGet();
                    methodHookParam.setResult((Object) null);
                } catch (Throwable th) {
                    FlexGuard.this.failed = true;
                    SunshineModule.log("flex-guard desativado: " + th.getClass().getSimpleName());
                }
            }
        });
    }

    String report() {
        return "flexStubCorrigido=" + this.avoided.get() + " flexFalha=" + this.failed;
    }
}
