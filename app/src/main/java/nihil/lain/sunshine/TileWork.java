package nihil.lain.sunshine;

import android.animation.ValueAnimator;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
final class TileWork {
    static final String BAR = "com.android.systemui.qs.bar.TileChunkLayoutBar";
    static final String HELPER = "com.android.systemui.qs.TileChunkLayoutBarExpandHelper";
    final Method animate;
    final Field animator;
    long cancelledDrags;
    long cancelledRetargets;
    long duplicateIndicators;
    boolean enabled;
    boolean failed;
    final Method height;
    final Method indicator;
    final Field indicatorView;
    final Field isTracking;
    final ThreadLocal<Scope> scopes = new ThreadLocal<>();
    final Method tracking;

    static final class Scope {
        int fraction;
        int height;
        ViewGroup.LayoutParams lp;
        final Object owner;
        final Scope previous;
        boolean recorded;
        View view;

        Scope(Object obj, Scope scope) {
            this.owner = obj;
            this.previous = scope;
        }
    }

    TileWork(ClassLoader classLoader, boolean z) throws Exception {
        if (!z) {
            throw new IllegalArgumentException("Unverified tiles DEX");
        }
        Method method = SunshineModule.method(classLoader, BAR, "setContainerHeight", Integer.TYPE);
        this.height = method;
        Method method2 = SunshineModule.method(classLoader, BAR, "setIndicatorTouchHeight", Float.TYPE);
        this.indicator = method2;
        Method method3 = SunshineModule.method(classLoader, HELPER, "setHeight", Integer.TYPE);
        this.animate = method3;
        Method method4 = SunshineModule.method(classLoader, HELPER, "setTracking", Float.TYPE, Boolean.TYPE);
        this.tracking = method4;
        Field field = SunshineModule.field(method.getDeclaringClass(), "mScrollIndicatorClickContainer");
        this.indicatorView = field;
        Field field2 = SunshineModule.field(method3.getDeclaringClass(), "expandAnimator");
        this.animator = field2;
        Field field3 = SunshineModule.field(method3.getDeclaringClass(), "tracking");
        this.isTracking = field3;
        if (!View.class.isAssignableFrom(field.getType()) || field2.getType() != ValueAnimator.class || field3.getType() != Boolean.TYPE || method.getReturnType() != Void.TYPE || method2.getReturnType() != Void.TYPE || method3.getReturnType() != Void.TYPE || method4.getReturnType() != Boolean.TYPE) {
            throw new NoSuchFieldException("tile contract");
        }
    }

    void install() {
        RuntimeHooks.hookMethod(this.height, new XC_MethodHook() { // from class: nihil.lain.sunshine.TileWork.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (TileWork.this.enabled && !TileWork.this.failed && RuntimeOptions.enabled(1) && Looper.myLooper() == Looper.getMainLooper()) {
                    Scope scope = new Scope(methodHookParam.thisObject, TileWork.this.scopes.get());
                    TileWork.this.scopes.set(scope);
                    methodHookParam.setObjectExtra("sunshine.tile.scope", scope);
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra("sunshine.tile.scope");
                if (objectExtra instanceof Scope) {
                    Scope scope = ((Scope) objectExtra).previous;
                    ThreadLocal<Scope> threadLocal = TileWork.this.scopes;
                    if (scope == null) {
                        threadLocal.remove();
                    } else {
                        threadLocal.set(scope);
                    }
                }
            }
        });
        RuntimeHooks.hookMethod(this.indicator, new XC_MethodHook() { // from class: nihil.lain.sunshine.TileWork.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Scope scope;
                if (!TileWork.this.enabled || TileWork.this.failed || (scope = TileWork.this.scopes.get()) == null || scope.owner != methodHookParam.thisObject) {
                    return;
                }
                try {
                    float fFloatValue = ((Float) methodHookParam.args[0]).floatValue();
                    if (!Float.isNaN(fFloatValue) && !Float.isInfinite(fFloatValue)) {
                        View view = (View) TileWork.this.indicatorView.get(methodHookParam.thisObject);
                        ViewGroup.LayoutParams layoutParams = view == null ? null : view.getLayoutParams();
                        if (scope.recorded && scope.fraction == Float.floatToIntBits(fFloatValue) && scope.view == view && layoutParams != null && scope.lp == layoutParams && layoutParams.height == scope.height) {
                            TileWork.this.duplicateIndicators++;
                            methodHookParam.setResult((Object) null);
                            return;
                        }
                        methodHookParam.setObjectExtra("sunshine.tile.record", scope);
                    }
                } catch (Throwable th) {
                    TileWork.this.fail(th);
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra("sunshine.tile.record");
                if (!(objectExtra instanceof Scope) || methodHookParam.hasThrowable()) {
                    return;
                }
                try {
                    Scope scope = (Scope) objectExtra;
                    scope.view = (View) TileWork.this.indicatorView.get(methodHookParam.thisObject);
                    scope.lp = scope.view == null ? null : scope.view.getLayoutParams();
                    scope.height = scope.lp == null ? 0 : scope.lp.height;
                    scope.fraction = Float.floatToIntBits(((Float) methodHookParam.args[0]).floatValue());
                    scope.recorded = scope.lp != null;
                } catch (Throwable th) {
                    TileWork.this.fail(th);
                }
            }
        });
        RuntimeHooks.hookMethod(this.animate, new XC_MethodHook() { // from class: nihil.lain.sunshine.TileWork.3
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (TileWork.this.canAnimate()) {
                    try {
                        if (TileWork.this.isTracking.getBoolean(methodHookParam.thisObject) && TileWork.this.cancel(methodHookParam.thisObject)) {
                            TileWork.this.cancelledRetargets++;
                        }
                    } catch (Throwable th) {
                        TileWork.this.fail(th);
                    }
                }
            }
        });
        RuntimeHooks.hookMethod(this.tracking, new XC_MethodHook() { // from class: nihil.lain.sunshine.TileWork.4
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (TileWork.this.canAnimate() && Boolean.TRUE.equals(methodHookParam.args[1])) {
                    try {
                        if (TileWork.this.isTracking.getBoolean(methodHookParam.thisObject) || !TileWork.this.cancel(methodHookParam.thisObject)) {
                            return;
                        }
                        TileWork.this.cancelledDrags++;
                    } catch (Throwable th) {
                        TileWork.this.fail(th);
                    }
                }
            }
        });
        this.enabled = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean canAnimate() {
        return this.enabled && !this.failed && Looper.myLooper() == Looper.getMainLooper() && RuntimeOptions.enabled(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean cancel(Object obj) throws Exception {
        ValueAnimator valueAnimator = (ValueAnimator) this.animator.get(obj);
        if (valueAnimator == null || !valueAnimator.isRunning()) {
            return false;
        }
        valueAnimator.cancel();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fail(Throwable th) {
        if (this.failed) {
            return;
        }
        this.failed = true;
        SunshineModule.diagnostic("tile-work desativado: " + th.getClass().getSimpleName());
    }

    String report() {
        return "tileIndicadoresDuplicados=" + this.duplicateIndicators + " tileRetargets=" + this.cancelledRetargets + " tileGestos=" + this.cancelledDrags + " tileFalha=" + this.failed;
    }
}
