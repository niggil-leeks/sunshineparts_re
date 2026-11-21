package nihil.sunshine.parts;

import android.os.Looper;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class BlurDelivery {
    static long avoided;
    static String error = "";

    static final class Access {
        final Field kind;
        final Field label;
        final Field owner;
        final Object quick;
        final Object unit;
        final Field view;

        Access(Class<?> cls, Class<?> cls2, Object obj, Object obj2) throws Exception {
            this.owner = BlurDelivery.field(cls, "this$0");
            this.label = BlurDelivery.field(cls, "label");
            this.kind = BlurDelivery.field(cls, "L$0");
            this.view = BlurDelivery.field(cls2, "view");
            if (this.owner.getType() != cls2 || this.label.getType() != Integer.TYPE || !View.class.isAssignableFrom(this.view.getType())) {
                throw new NoSuchFieldException("Blur delivery ABI");
            }
            this.quick = obj;
            this.unit = obj2;
        }

        boolean redundant(Object obj, Object obj2) throws Exception {
            Object obj3;
            View view;
            return obj2 == this.unit && this.label.getInt(obj) == 0 && this.kind.get(obj) == this.quick && (obj3 = this.owner.get(obj)) != null && (view = (View) this.view.get(obj3)) != null && view.isAttachedToWindow() && view.getAlpha() > 0.0f && view.getBackground() != null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Field field(Class<?> cls, String str) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField;
    }

    static XC_MethodHook hook(final Access access) {
        return new XC_MethodHook(10000) { // from class: nihil.sunshine.parts.BlurDelivery.1
            private boolean failed;

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (this.failed || !VisualPolicy.leanQs() || !VisualPolicy.nativeBlur() || Looper.myLooper() != Looper.getMainLooper()) {
                    return;
                }
                try {
                    if (access.redundant(methodHookParam.thisObject, methodHookParam.args[0])) {
                        methodHookParam.setResult(access.unit);
                        BlurDelivery.avoided++;
                    }
                } catch (Throwable th) {
                    this.failed = true;
                    BlurDelivery.error = th.getClass().getSimpleName();
                }
            }
        };
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
                throw new IllegalStateException("Unrecognized SystemUI DEX");
            }
            Class<?> cls = Class.forName("com.android.systemui.blur.ui.viewbinder.SecCapturedBlurContainerBinder", false, classLoader);
            Class<?> cls2 = Class.forName(cls.getName() + "$1$1$3", false, classLoader);
            Access access = new Access(cls2, cls, field(Class.forName("com.android.systemui.blur.di.SecPanelBlurBinding$BlurType", false, classLoader), "QUICK_PANEL").get(null), field(Class.forName("kotlin.Unit", false, classLoader), "INSTANCE").get(null));
            Method declaredMethod = cls2.getDeclaredMethod("invokeSuspend", Object.class);
            if (declaredMethod.getReturnType() == Object.class && RuntimeHooks.hookMethod(declaredMethod, hook(access)) != null) {
                ControlBus.feature("blur_delivery", "Unchanged background deliveries bypassed");
                return;
            }
            throw new IllegalStateException("Blur receiver hook");
        } catch (Throwable th) {
            error = th.getClass().getSimpleName();
            ControlBus.feature("blur_delivery", "Native fallback");
        }
    }

    private BlurDelivery() {
    }
}
