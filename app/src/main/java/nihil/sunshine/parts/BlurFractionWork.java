package nihil.sunshine.parts;

import android.view.View;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class BlurFractionWork {
    static String error = "";
    static long updates;

    static final class Access {
        final Field bouncer;
        final Field interactor;
        final Field mirror;
        final MethodHandle value;
        final Field view;
        final Field viewModel;

        Access(Class<?> cls) throws Exception {
            this.view = BlurFractionWork.field(cls, "view");
            this.bouncer = BlurFractionWork.field(cls, "bouncerShowing");
            this.viewModel = BlurFractionWork.field(cls, "viewModel");
            this.interactor = BlurFractionWork.field(this.viewModel.getType(), "secCapturedBlurInteractor");
            this.mirror = BlurFractionWork.field(this.interactor.getType(), "mirrorShowing");
            Method declaredMethod = this.mirror.getType().getDeclaredMethod("getValue", new Class[0]);
            declaredMethod.setAccessible(true);
            if (!View.class.isAssignableFrom(this.view.getType()) || this.bouncer.getType() != Boolean.TYPE || declaredMethod.getReturnType() != Object.class) {
                throw new NoSuchFieldException("Blur fraction ABI");
            }
            this.value = MethodHandles.lookup().unreflect(declaredMethod).asType(MethodType.methodType((Class<?>) Object.class, (Class<?>) Object.class));
        }

        boolean apply(Object obj, float f) throws Throwable {
            boolean zBooleanValue = false;
            if (!Float.isFinite(f) || f < 0.0f || f > 1.0f) {
                return false;
            }
            if (this.bouncer.getBoolean(obj)) {
                return true;
            }
            View view = (View) this.view.get(obj);
            if (f == 0.0f) {
                zBooleanValue = ((Boolean) (Object) this.value.invokeExact(this.mirror.get(this.interactor.get(this.viewModel.get(obj))))).booleanValue();
            }
            if (f == 0.0f && !zBooleanValue) {
                view.setAlpha(0.0f);
                view.setBackgroundDrawable(null);
            }
            view.setAlpha(f);
            BlurFractionWork.updates++;
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Field field(Class<?> cls, String str) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private BlurFractionWork() {
    }
}
