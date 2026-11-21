package nihil.sunshine.parts;

import android.animation.ValueAnimator;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes.dex */
final class BlurAnimatorState {

    static final class Access {
        final Field animator;
        final Constructor<?> listener;

        Access(Class<?> cls, Class<?> cls2) throws Exception {
            this.animator = cls.getDeclaredField("blurAnimator");
            this.animator.setAccessible(true);
            this.listener = cls2.getDeclaredConstructor(cls);
            this.listener.setAccessible(true);
            if (this.animator.getType() != ValueAnimator.class || Modifier.isFinal(this.animator.getModifiers()) || !ValueAnimator.AnimatorUpdateListener.class.isAssignableFrom(cls2)) {
                throw new NoSuchFieldException("Blur animator ABI");
            }
        }

        void run(Object obj, float f, float f2, int i) throws Throwable {
            ValueAnimator valueAnimator = (ValueAnimator) this.animator.get(obj);
            if (valueAnimator != null && valueAnimator.isRunning()) {
                valueAnimator.cancel();
            }
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(f, f2);
            this.animator.set(obj, valueAnimatorOfFloat);
            valueAnimatorOfFloat.setDuration(i);
            try {
                valueAnimatorOfFloat.addUpdateListener((ValueAnimator.AnimatorUpdateListener) this.listener.newInstance(obj));
                valueAnimatorOfFloat.start();
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    static XC_MethodHook hook(final Access access) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.BlurAnimatorState.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                try {
                    access.run(methodHookParam.thisObject, ((Float) methodHookParam.args[0]).floatValue(), ((Float) methodHookParam.args[1]).floatValue(), ((Integer) methodHookParam.args[2]).intValue());
                    methodHookParam.setResult((Object) null);
                } catch (Throwable th) {
                    methodHookParam.setThrowable(th);
                }
            }
        };
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
                throw new IllegalStateException("Unknown System UI code");
            }
            Class<?> cls = Class.forName("com.android.systemui.blur.SecQpBlurController", false, classLoader);
            Class<?> cls2 = Class.forName(cls.getName() + "$makeAnimationAndRun$1", false, classLoader);
            Method declaredMethod = cls.getDeclaredMethod("makeAnimationAndRun", Float.TYPE, Float.TYPE, Integer.TYPE);
            if (declaredMethod.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("Blur animator result");
            }
            if (RuntimeHooks.hookMethod(declaredMethod, hook(new Access(cls, cls2))) == null) {
                throw new IllegalStateException("Blur animator hook rejected");
            }
        } catch (Throwable th) {
        }
    }

    private BlurAnimatorState() {
    }
}
