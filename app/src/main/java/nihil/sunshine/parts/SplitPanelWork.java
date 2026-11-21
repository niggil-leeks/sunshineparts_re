package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes.dex */
final class SplitPanelWork {
    static String error = "";
    static long expansionEvents;
    static long updates;

    static final class Access {
        final Field adapter;
        final Field[] animators = new Field[3];
        final Field compose;
        final Field flow;
        final MethodHandle flowCall;
        final Class<?> owner;
        final Field position;
        final MethodHandle positionCall;
        final Method setPosition;
        final Method update;
        final Field userChanged;

        Access(Class<?> cls, Class<?> cls2) throws Exception {
            this.owner = cls;
            this.compose = SplitPanelWork.field(cls2, "QUICK_PANEL_COMPOSE");
            this.userChanged = SplitPanelWork.field(cls, "mUserChanged");
            this.position = SplitPanelWork.field(cls, "openPosition");
            this.adapter = SplitPanelWork.field(cls, "composeAdapter");
            String[] strArr = {"headerAnimator", "blurViewAnimator", "clockDateAnimator"};
            for (int i = 0; i < 3; i++) {
                this.animators[i] = SplitPanelWork.field(cls, strArr[i]);
            }
            Class<?> type = this.animators[0].getType();
            if (this.animators[1].getType() != type || this.animators[2].getType() != type || this.compose.getType() != Boolean.TYPE || !Modifier.isStatic(this.compose.getModifiers()) || this.userChanged.getType() != Boolean.TYPE || this.position.getType() != Float.TYPE) {
                throw new NoSuchFieldException("Split animator ABI");
            }
            this.setPosition = type.getDeclaredMethod("setPosition", Float.TYPE);
            this.setPosition.setAccessible(true);
            this.flow = SplitPanelWork.field(this.adapter.getType(), "_expansionFraction");
            this.update = this.flow.getType().getDeclaredMethod("updateState", Object.class, Object.class);
            this.update.setAccessible(true);
            if (this.setPosition.getReturnType() != Void.TYPE || this.update.getReturnType() != Boolean.TYPE) {
                throw new NoSuchMethodException("Compose branch ABI");
            }
            this.positionCall = MethodHandles.lookup().unreflect(this.setPosition).asType(MethodType.methodType(Void.TYPE, Object.class, Float.TYPE));
            this.flowCall = MethodHandles.lookup().unreflect(this.update).asType(MethodType.methodType(Boolean.TYPE, Object.class, Object.class, Object.class));
        }

        boolean enabled(Object obj) throws Exception {
            return obj != null && obj.getClass() == this.owner && this.compose.getBoolean(null);
        }

        void run(Object obj, float f) throws Throwable {
            runValue(obj, f, Float.valueOf(f));
        }

        void run(Object obj, Object[] objArr) throws Throwable {
            runValue(obj, ((Float) objArr[0]).floatValue(), objArr[0]);
        }

        private void runValue(Object obj, float f, Object obj2) throws Throwable {
            this.userChanged.setBoolean(obj, false);
            this.position.setFloat(obj, f);
            for (Field field : this.animators) {
                Object obj3 = field.get(obj);
                if (obj3 != null) {
                    (void) this.positionCall.invokeExact(obj3, f);
                }
            }
            (boolean) this.flowCall.invokeExact(this.flow.get(this.adapter.get(obj)), null, obj2);
            SplitPanelWork.updates++;
        }
    }

    static Field field(Class<?> cls, String str) throws Exception {
        while (cls != null) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(str);
    }

    static Object invoke(Method method, Object obj, Object... objArr) throws Throwable {
        try {
            return method.invoke(obj, objArr);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static XC_MethodHook hook(final Access access) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.SplitPanelWork.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                if (!VisualPolicy.leanQs() || !(methodHookParam.args[0] instanceof Float) || !Float.isFinite(((Float) methodHookParam.args[0]).floatValue()) || !access.enabled(methodHookParam.thisObject)) {
                    return;
                }
                try {
                    access.run(methodHookParam.thisObject, methodHookParam.args);
                    methodHookParam.setResult((Object) null);
                } catch (Throwable th) {
                    SplitPanelWork.error = th.getClass().getSimpleName();
                    methodHookParam.setThrowable(th);
                }
            }
        };
    }

    static XC_MethodHook expansionHook(final Access access, final Field field, final Field field2, Method method) throws Exception {
        final MethodHandle methodHandleAsType = MethodHandles.lookup().unreflect(method).asType(MethodType.methodType((Class<?>) Boolean.TYPE, (Class<?>) Object.class));
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.SplitPanelWork.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                if (!VisualPolicy.leanQs() || !access.enabled(methodHookParam.thisObject) || methodHookParam.args[0] == null) {
                    return;
                }
                float f = field.getFloat(methodHookParam.args[0]);
                if (Float.isFinite(f)) {
                    try {
                        if (!(boolean) methodHandleAsType.invokeExact(methodHookParam.thisObject) && field2.getInt(null) != 1) {
                            access.run(methodHookParam.thisObject, f);
                        }
                        SplitPanelWork.expansionEvents++;
                        methodHookParam.setResult((Object) null);
                    } catch (Throwable th) {
                        SplitPanelWork.error = th.getClass().getSimpleName();
                        methodHookParam.setThrowable(th);
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
            Class<?> cls = Class.forName("com.android.systemui.qs.animator.PanelSplitOpenAnimator", false, classLoader);
            Access access = new Access(cls, Class.forName("com.android.systemui.QpRune", false, classLoader));
            Method declaredMethod = cls.getDeclaredMethod("setPosition$1", Float.TYPE);
            if (declaredMethod.getReturnType() != Void.TYPE || Modifier.isStatic(declaredMethod.getModifiers())) {
                throw new NoSuchMethodException("Split position ABI");
            }
            Method declaredMethod2 = cls.getDeclaredMethod("onPanelExpansionChanged", Class.forName("com.android.systemui.shade.ShadeExpansionChangeEvent", false, classLoader));
            QsHotPaths.deoptimize(declaredMethod2);
            if (RuntimeHooks.hookMethod(declaredMethod, hook(access)) == null) {
                throw new IllegalStateException("Hook rejected");
            }
            Field field = field(declaredMethod2.getParameterTypes()[0], "fraction");
            Field field2 = field(Class.forName("com.android.systemui.qs.animator.QsAnimatorState", false, classLoader), "state");
            Method declaredMethod3 = cls.getDeclaredMethod("isThereNoView", new Class[0]);
            declaredMethod3.setAccessible(true);
            if (field.getType() == Float.TYPE && field2.getType() == Integer.TYPE && Modifier.isStatic(field2.getModifiers()) && declaredMethod3.getReturnType() == Boolean.TYPE && RuntimeHooks.hookMethod(declaredMethod2, expansionHook(access, field, field2, declaredMethod3)) == null) {
                throw new IllegalStateException("Expansion hook rejected");
            }
            ControlBus.feature("split_panel_work", "Native motion with lighter bookkeeping");
        } catch (Throwable th) {
            error = th.getClass().getSimpleName();
            ControlBus.feature("split_panel_work", "Native fallback");
        }
    }

    private SplitPanelWork() {
    }
}
