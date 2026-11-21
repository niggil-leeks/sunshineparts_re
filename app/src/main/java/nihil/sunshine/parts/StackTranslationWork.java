package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class StackTranslationWork {
    static long avoided;
    static String error = "";

    static XC_MethodHook hook(final Field field) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.StackTranslationWork.1
            private boolean failed;

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (this.failed || !VisualPolicy.leanQs() || !(methodHookParam.args[0] instanceof Float)) {
                    return;
                }
                float fFloatValue = ((Float) methodHookParam.args[0]).floatValue();
                if (Float.isFinite(fFloatValue)) {
                    try {
                        if (field.getFloat(methodHookParam.thisObject) != fFloatValue) {
                            field.setFloat(methodHookParam.thisObject, fFloatValue);
                            StackTranslationWork.avoided++;
                        }
                    } catch (Throwable th) {
                        this.failed = true;
                        StackTranslationWork.error = th.getClass().getSimpleName();
                        ControlBus.feature("notification_translation_work", "native fallback");
                    }
                }
            }
        };
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
                throw new IllegalStateException("Unrecognized SystemUI DEX");
            }
            Class<?> cls = Class.forName("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", false, classLoader);
            Method declaredMethod = cls.getDeclaredMethod("setTranslationX", Float.TYPE);
            Field declaredField = cls.getDeclaredField("mPreviousTranslationX");
            declaredField.setAccessible(true);
            if (declaredField.getType() == Float.TYPE && declaredMethod.getReturnType() == Void.TYPE) {
                QsHotPaths.deoptimize(Class.forName("com.android.systemui.qs.animator.QSAnimView", false, classLoader).getDeclaredMethod("setTranslationX", Float.TYPE));
                if (RuntimeHooks.hookMethod(declaredMethod, hook(declaredField)) == null) {
                    throw new IllegalStateException("Hook rejected");
                }
                ControlBus.feature("notification_translation_work", "Native translation without diagnostic stacks");
                return;
            }
            throw new NoSuchFieldException("Translation ABI");
        } catch (Throwable th) {
            error = th.getClass().getSimpleName();
            ControlBus.feature("notification_translation_work", "native fallback");
        }
    }

    private StackTranslationWork() {
    }
}
