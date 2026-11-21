package nihil.sunshine.parts;

import android.graphics.Canvas;
import android.os.Debug;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.PathInterpolator;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.BiFunction;

/* JADX INFO: loaded from: classes.dex */
final class QsHotPaths {
    static long alphaWrites;
    static long curveHits;
    static boolean curveProbeVerified;
    static long directTargetReads;
    static long duplicateTargets;
    static long frameStacksSkipped;
    static long ownedTargetInstances;
    private static boolean probing;
    static long stateBindingEvictions;
    static long stateBindingsBuilt;
    static long stateBindingsReused;
    static long targetCreationScopes;
    private static final Object TOKEN = new Object();
    private static final Object[] NO_ARGS = new Object[0];
    private static final HotScope frameScope = new HotScope();
    private static final HotScope mediaScope = new HotScope();
    private static final HotScope gridScope = new HotScope();
    private static final HotScope creationScope = new HotScope();
    private static final WeakIdentityCache<OwnedTarget> ownedTargets = new WeakIdentityCache<>(4096, false);
    static String curveError = "";
    static String targetMode = "";
    static String targetError = "";

    private static final class OwnedTarget {
        TargetReader.Binding binding;

        private OwnedTarget() {
        }
    }

    private static Class<?> type(ClassLoader classLoader, String str) throws Exception {
        return Class.forName(str, false, classLoader);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Field field(Class<?> cls, String str) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Method method(Class<?> cls, String str, Class<?>... clsArr) throws Exception {
        Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
        declaredMethod.setAccessible(true);
        return declaredMethod;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void unavailable(String str, Throwable th) {
        ControlBus.feature(str, "native fallback");
        if ("media_curve_cache".equals(str)) {
            curveError = reason(th);
        }
        if ("qs_target_coalescing".equals(str)) {
            targetError = reason(th);
        }
        Log.w("SunshinePartsUI", str + " unavailable; native behavior retained", th);
    }

    private static String reason(Throwable th) {
        while ((th instanceof InvocationTargetException) && th.getCause() != null) {
            th = th.getCause();
        }
        return th.getClass().getSimpleName() + ": " + String.valueOf(th.getMessage());
    }

    static void deoptimize(Member member) throws Exception {
        method(XposedBridge.class, "deoptimizeMethod", Member.class).invoke(null, member);
    }

    static void install(ClassLoader classLoader) {
        install(classLoader, null);
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            visibility(classLoader, str);
        } catch (Throwable th) {
        }
        try {
            alpha(classLoader);
        } catch (Throwable th2) {
            unavailable("qs_alpha_path", th2);
        }
        ControlBus.feature("compose_frame_diagnostics", "Native frame votes; unverified framework replacements retired");
        try {
            mediaCurves(classLoader);
        } catch (Throwable th3) {
            unavailable("media_curve_cache", th3);
        }
        try {
            gridTargets(classLoader, str);
        } catch (Throwable th4) {
            unavailable("qs_target_coalescing", th4);
        }
        NotificationPalette.install(classLoader);
    }

    private static void visibility(ClassLoader classLoader, String str) throws Exception {
        if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
            return;
        }
        final Class<?> clsType = type(classLoader, "com.android.systemui.qs.animator.QSAnimView");
        final Field field = field(clsType, "view");
        Method method = method(clsType, "setVisibility", Integer.TYPE);
        if (field.getType() != View.class || method.getReturnType() != Void.TYPE) {
            throw new NoSuchMethodException("QS visibility ABI");
        }
        RuntimeHooks.hookMethod(method, new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                if (AdaptiveWorkload.level() != 3 || methodHookParam.thisObject.getClass() != clsType) {
                    return;
                }
                try {
                    View view = (View) field.get(methodHookParam.thisObject);
                    if (view != null) {
                        view.setVisibility(((Integer) methodHookParam.args[0]).intValue());
                    }
                    methodHookParam.setResult((Object) null);
                } catch (IllegalAccessException e) {
                }
            }
        });
    }

    private static void alpha(ClassLoader classLoader) throws Exception {
        Class<?> clsType = type(classLoader, "com.android.systemui.qs.animator.QSAnimView");
        Field field = field(clsType, "view");
        Method method = method(clsType, "setAlpha", Float.TYPE);
        if (field.getType() != View.class || method.getReturnType() != Void.TYPE) {
            throw new NoSuchMethodException("QS alpha contract");
        }
        RuntimeHooks.hookMethod(method, alphaHook(field));
        ControlBus.feature("qs_alpha_path", "ready");
    }

    static XC_MethodHook alphaHook(final Field field) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                if (VisualPolicy.leanQs()) {
                    try {
                        View view = (View) field.get(methodHookParam.thisObject);
                        if (view != null) {
                            view.setAlpha(((Float) methodHookParam.args[0]).floatValue());
                        }
                        QsHotPaths.alphaWrites++;
                        methodHookParam.setResult((Object) null);
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        };
    }

    private static XC_MethodHook scoped(final HotScope hotScope) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.3
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                hotScope.enter(QsHotPaths.TOKEN);
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                hotScope.leave();
            }
        };
    }

    private static void frameDiagnostics(ClassLoader classLoader) throws Exception {
        Method method = method(type(classLoader, "androidx.compose.ui.platform.Api35Impl"), "setRequestedFrameRate", View.class, Float.TYPE);
        Method method2 = method(Debug.class, "getCallers", Integer.TYPE);
        if (method.getReturnType() != Void.TYPE || method2.getReturnType() != String.class) {
            throw new NoSuchMethodException("Compose frame diagnostics contract");
        }
        RuntimeHooks.hookMethod(method2, new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.4
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (VisualPolicy.leanQs() && QsHotPaths.frameScope.active()) {
                    QsHotPaths.frameStacksSkipped++;
                    methodHookParam.setResult("");
                }
            }
        });
        RuntimeHooks.hookMethod(method, scoped(frameScope));
        ControlBus.feature("compose_frame_diagnostics", "ready");
    }

    static final class CubicMemo {
        private final float[][] x = new float[2][];
        private final float[][] y = new float[2][];
        private final PathInterpolator[] reference = new PathInterpolator[2];
        private final Field xs = QsHotPaths.field(PathInterpolator.class, "mX");
        private final Field ys = QsHotPaths.field(PathInterpolator.class, "mY");

        CubicMemo() throws Exception {
            if (this.xs.getType() != float[].class || this.ys.getType() != float[].class) {
                throw new NoSuchFieldException("Path tables");
            }
            int i = 0;
            while (i < 2) {
                this.reference[i] = new PathInterpolator(i == 0 ? 0.22f : 0.25f, 0.25f, 0.63f, 1.0f);
                this.x[i] = (float[]) ((float[]) this.xs.get(this.reference[i])).clone();
                this.y[i] = (float[]) ((float[]) this.ys.get(this.reference[i])).clone();
                if (this.x[i].length >= 2 && this.x[i].length == this.y[i].length && this.x[i].length <= 16384) {
                    i++;
                } else {
                    throw new IllegalStateException("Unexpected native path table");
                }
            }
        }

        static int index(Object[] objArr) {
            if (Float.floatToRawIntBits(((Float) objArr[1]).floatValue()) != 1048576000 || Float.floatToRawIntBits(((Float) objArr[2]).floatValue()) != 1059145646 || Float.floatToRawIntBits(((Float) objArr[3]).floatValue()) != 1065353216) {
                return -1;
            }
            int iFloatToRawIntBits = Float.floatToRawIntBits(((Float) objArr[0]).floatValue());
            if (iFloatToRawIntBits == 1046562734) {
                return 0;
            }
            return iFloatToRawIntBits == 1048576000 ? 1 : -1;
        }

        boolean reuse(XC_MethodHook.MethodHookParam methodHookParam) throws IllegalAccessException {
            int iIndex = index(methodHookParam.args);
            if (iIndex < 0) {
                return false;
            }
            this.xs.set(methodHookParam.thisObject, this.x[iIndex]);
            this.ys.set(methodHookParam.thisObject, this.y[iIndex]);
            methodHookParam.setResult((Object) null);
            QsHotPaths.curveHits++;
            return true;
        }

        void verify() throws Exception {
            long j = QsHotPaths.curveHits;
            boolean unused = QsHotPaths.probing = true;
            QsHotPaths.mediaScope.enter(QsHotPaths.TOKEN);
            int i = 0;
            while (i < 2) {
                try {
                    PathInterpolator pathInterpolator = new PathInterpolator(i == 0 ? 0.22f : 0.25f, 0.25f, 0.63f, 1.0f);
                    for (int i2 = 0; i2 <= 1024; i2++) {
                        float f = i2 / 1024.0f;
                        if (Float.floatToIntBits(pathInterpolator.getInterpolation(f)) != Float.floatToIntBits(this.reference[i].getInterpolation(f))) {
                            throw new IllegalStateException("Native interpolation changed");
                        }
                    }
                    i++;
                } catch (Throwable th) {
                    QsHotPaths.mediaScope.leave();
                    boolean unused2 = QsHotPaths.probing = false;
                    QsHotPaths.curveHits = j;
                    throw th;
                }
            }
            if (QsHotPaths.curveHits - j == 2) {
                QsHotPaths.mediaScope.leave();
                boolean unused3 = QsHotPaths.probing = false;
                QsHotPaths.curveHits = j;
                return;
            }
            throw new IllegalStateException("Cubic hook not reached");
        }
    }

    private static void mediaCurves(ClassLoader classLoader) throws Exception {
        Method method = method(type(classLoader, "com.android.systemui.media.audiovisseekbar.AudioVisSeekBarProgressDrawable"), "draw", Canvas.class);
        Method method2 = method(PathInterpolator.class, "initCubic", Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
        final CubicMemo cubicMemo = new CubicMemo();
        XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(method2, new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.5
            private boolean failed;

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (!this.failed) {
                    if ((VisualPolicy.leanQs() || QsHotPaths.probing) && QsHotPaths.mediaScope.active()) {
                        try {
                            cubicMemo.reuse(methodHookParam);
                        } catch (Throwable th) {
                            this.failed = true;
                            QsHotPaths.curveProbeVerified = false;
                            QsHotPaths.unavailable("media_curve_cache", th);
                        }
                    }
                }
            }
        });
        try {
            try {
                cubicMemo.verify();
            } catch (IllegalStateException e) {
                if (!"Cubic hook not reached".equals(e.getMessage())) {
                    throw e;
                }
                deoptimize(PathInterpolator.class.getDeclaredConstructor(Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE));
                cubicMemo.verify();
            }
            deoptimize(method);
            RuntimeHooks.hookMethod(method, scoped(mediaScope));
            curveProbeVerified = true;
            ControlBus.feature("media_curve_cache", "ready");
        } catch (Throwable th) {
            RuntimeHooks.unhook(unhookHookMethod);
            throw th;
        }
    }

    static final class TargetReader {
        private final Field animatable;
        private final Method animationValue;
        private final WeakIdentityCache<Binding> bindings = new WeakIdentityCache<>(128, false);
        private final Field internal;
        private final Field pending;
        private final boolean stableContainers;
        private final Method stateValue;
        private final Field target;

        private static final class Binding {
            final WeakReference<Object> anim;
            final WeakReference<Object> internal;
            final WeakReference<Object> pending;
            final WeakReference<Object> target;

            Binding(Object obj, Object obj2, Object obj3, Object obj4) {
                this.anim = new WeakReference<>(obj);
                this.pending = new WeakReference<>(obj2);
                this.target = new WeakReference<>(obj3);
                this.internal = new WeakReference<>(obj4);
            }
        }

        TargetReader(Class<?> cls, Class<?> cls2, Class<?> cls3, Class<?> cls4) throws Exception {
            boolean z = false;
            this.animatable = QsHotPaths.field(cls, "animatable");
            this.pending = QsHotPaths.field(cls, "_pendingTarget$delegate");
            this.target = QsHotPaths.field(cls2, "targetValue$delegate");
            this.internal = QsHotPaths.field(cls2, "internalState");
            this.stateValue = QsHotPaths.method(cls4, "getValue", new Class[0]);
            this.animationValue = QsHotPaths.method(cls3, "getValue", new Class[0]);
            if (Modifier.isFinal(this.pending.getModifiers()) && Modifier.isFinal(this.target.getModifiers()) && Modifier.isFinal(this.internal.getModifiers())) {
                z = true;
            }
            this.stableContainers = z;
        }

        boolean reuse(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
            return reuse(methodHookParam, null);
        }

        boolean reuse(XC_MethodHook.MethodHookParam methodHookParam, OwnedTarget ownedTarget) throws Throwable {
            Object obj;
            Object obj2;
            Object obj3;
            Object obj4 = this.animatable.get(methodHookParam.thisObject);
            if (obj4 == null) {
                return false;
            }
            if (this.stableContainers && Looper.myLooper() == Looper.getMainLooper()) {
                Binding binding = ownedTarget == null ? this.bindings.get(methodHookParam.thisObject) : ownedTarget.binding;
                if (binding == null || binding.anim.get() != obj4) {
                    obj = null;
                    obj3 = null;
                    obj2 = null;
                } else {
                    obj = binding.pending.get();
                    obj2 = binding.target.get();
                    obj3 = binding.internal.get();
                }
                if (obj == null || obj2 == null || obj3 == null) {
                    obj = this.pending.get(methodHookParam.thisObject);
                    obj2 = this.target.get(obj4);
                    obj3 = this.internal.get(obj4);
                    Binding binding2 = new Binding(obj4, obj, obj2, obj3);
                    if (ownedTarget != null) {
                        ownedTarget.binding = binding2;
                    } else {
                        long jEvictions = this.bindings.evictions();
                        this.bindings.put(methodHookParam.thisObject, binding2);
                        QsHotPaths.stateBindingEvictions += this.bindings.evictions() - jEvictions;
                    }
                    QsHotPaths.stateBindingsBuilt++;
                } else {
                    QsHotPaths.stateBindingsReused++;
                }
            } else {
                obj = this.pending.get(methodHookParam.thisObject);
                obj2 = this.target.get(obj4);
                obj3 = this.internal.get(obj4);
            }
            Object obj5 = methodHookParam.args[0];
            if (!Objects.equals(this.stateValue.invoke(obj, QsHotPaths.NO_ARGS), obj5) || !Objects.equals(this.stateValue.invoke(obj2, QsHotPaths.NO_ARGS), obj5)) {
                return false;
            }
            methodHookParam.setResult(this.animationValue.invoke(obj3, QsHotPaths.NO_ARGS));
            QsHotPaths.duplicateTargets++;
            return true;
        }
    }

    private static void gridTargets(ClassLoader classLoader, String str) throws Exception {
        final boolean z = false;
        if (NativeTargets.systemUi(str)) {
            targetMode = "native path";
            String[] strArr = {"qs_target_coalescing", "qs_direct_snapshot", "qs_target_ownership"};
            for (int i = 0; i < 3; i++) {
                ControlBus.feature(strArr[i], "Native path");
            }
            return;
        }
        Class<?> clsType = type(classLoader, "androidx.compose.animation.core.DeferredTargetAnimation");
        Method method = method(clsType, "updateTarget", Object.class, type(classLoader, "kotlinx.coroutines.CoroutineScope"), type(classLoader, "androidx.compose.animation.core.FiniteAnimationSpec"));
        final TargetReader targetReader = new TargetReader(clsType, type(classLoader, "androidx.compose.animation.core.Animatable"), type(classLoader, "androidx.compose.animation.core.AnimationState"), type(classLoader, "androidx.compose.runtime.SnapshotMutableStateImpl"));
        try {
            creationOwnership(classLoader, clsType);
            z = true;
            z = true;
            targetMode = "creation";
        } catch (Throwable th) {
            targetError = reason(th);
            placementOwnership(classLoader, clsType);
            targetMode = "placement fallback";
        }
        final BiFunction<Object, Object, Object> biFunctionLoad = (DensityQueries.pinned(str) && IconDimensions.pinned(str)) ? FastTargetAccess.load(classLoader, TOKEN) : null;
        ControlBus.feature("qs_direct_snapshot", biFunctionLoad == null ? "Reflection fallback" : "Native Snapshot getters; no reflective state bindings");
        RuntimeHooks.hookMethod(method, new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.6
            private BiFunction<Object, Object, Object> access;
            private boolean failed;

            {
                this.access = biFunctionLoad;
            }

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (this.failed || !VisualPolicy.leanQs()) {
                    return;
                }
                OwnedTarget ownedTarget = z ? (OwnedTarget) QsHotPaths.ownedTargets.get(methodHookParam.thisObject) : null;
                if (z) {
                    if (ownedTarget == null) {
                        return;
                    }
                } else if (!QsHotPaths.gridScope.is(methodHookParam.thisObject)) {
                    return;
                }
                try {
                    if (this.access != null) {
                        try {
                            Object objApply = this.access.apply(methodHookParam.thisObject, methodHookParam.args[0]);
                            QsHotPaths.directTargetReads++;
                            if (objApply != QsHotPaths.TOKEN) {
                                methodHookParam.setResult(objApply);
                                QsHotPaths.duplicateTargets++;
                                return;
                            }
                            return;
                        } catch (ClassCastException | LinkageError e) {
                            this.access = null;
                            ControlBus.feature("qs_direct_snapshot", "Reflection fallback");
                        }
                    }
                    targetReader.reuse(methodHookParam, ownedTarget);
                } catch (Throwable th2) {
                    this.failed = true;
                    QsHotPaths.unavailable("qs_target_coalescing", th2);
                }
            }
        });
        ControlBus.feature("qs_target_coalescing", "ready");
        ControlBus.feature("qs_target_ownership", targetMode);
    }

    static XC_MethodHook creationHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.7
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                QsHotPaths.targetCreationScopes++;
                QsHotPaths.creationScope.enter(QsHotPaths.TOKEN);
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                QsHotPaths.creationScope.leave();
            }
        };
    }

    static XC_MethodHook targetConstructorHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.8
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (!methodHookParam.hasThrowable() && QsHotPaths.creationScope.active() && QsHotPaths.ownedTargets.get(methodHookParam.thisObject) == null) {
                    QsHotPaths.ownedTargets.put(methodHookParam.thisObject, new OwnedTarget());
                    QsHotPaths.ownedTargetInstances++;
                }
            }
        };
    }

    private static void creationOwnership(ClassLoader classLoader, Class<?> cls) throws Exception {
        Method method = method(type(classLoader, "com.android.systemui.samsung.quicksetting.ui.base.spannablegrid.SpannableGridScopeImpl$$ExternalSyntheticLambda0"), "invoke", Object.class, Object.class, Object.class);
        Constructor<?> declaredConstructor = cls.getDeclaredConstructor(type(classLoader, "androidx.compose.animation.core.TwoWayConverter"));
        deoptimize(method);
        XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(declaredConstructor, targetConstructorHook());
        if (unhookHookMethod == null) {
            throw new IllegalStateException("Target constructor hook rejected");
        }
        try {
            if (RuntimeHooks.hookMethod(method, creationHook()) == null) {
                throw new IllegalStateException("QS creation scope hook rejected");
            }
        } catch (Throwable th) {
            RuntimeHooks.unhook(unhookHookMethod);
            throw new IllegalStateException("QS creation ownership unavailable", th);
        }
    }

    private static void placementOwnership(ClassLoader classLoader, Class<?> cls) throws Exception {
        Class<?> clsType = type(classLoader, "com.android.systemui.samsung.quicksetting.ui.base.spannablegrid.SpannableGridScopeImpl$$ExternalSyntheticLambda9");
        final Field field = field(clsType, "f$2");
        if (field.getType() != cls) {
            throw new NoSuchFieldException("QS grid animation owner");
        }
        RuntimeHooks.hookMethod(method(clsType, "invoke", Object.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.QsHotPaths.9
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                Object obj;
                try {
                    obj = field.get(methodHookParam.thisObject);
                } catch (IllegalAccessException e) {
                    obj = QsHotPaths.TOKEN;
                }
                HotScope hotScope = QsHotPaths.gridScope;
                if (obj == null) {
                    obj = QsHotPaths.TOKEN;
                }
                hotScope.enter(obj);
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                QsHotPaths.gridScope.leave();
            }
        });
    }

    private QsHotPaths() {
    }
}
