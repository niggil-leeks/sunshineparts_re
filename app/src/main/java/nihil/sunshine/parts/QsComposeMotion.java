package nihil.sunshine.parts;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
final class QsComposeMotion {
    static long changed;
    private static Field compositionField;
    private static Context context;
    static long created;
    private static XC_MethodHook.Unhook creationHook;
    private static Field delegate;
    static long fastPaths;
    private static ClassLoader loader;
    private static boolean observing;
    private static Method parent;
    private static Method setFloat;
    private static final WeakHashMap<View, WeakReference<Object>> hosts = new WeakHashMap<>();
    private static final WeakHashMap<Object, WeakReference<View>> scales = new WeakHashMap<>();

    static void configure(ClassLoader classLoader) {
        if (VisualPolicy.motionScale() == 1.0f) {
            if (creationHook != null) {
                RuntimeHooks.unhook(creationHook);
                creationHook = null;
            }
            if (context != null) {
                apply();
            }
            ControlBus.feature("qs_compose_motion", "Native clock; inactive composition hook detached");
            return;
        }
        if (creationHook == null) {
            install(classLoader);
        }
        if (context != null) {
            apply();
        }
    }

    private static Class<?> type(String str) throws Exception {
        return Class.forName(str, false, loader);
    }

    private static Object field(Object obj, String str) throws Exception {
        Field declaredField = obj.getClass().getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField.get(obj);
    }

    static boolean qs(View view) {
        boolean z = false;
        for (int i = 0; view != null && i < 32; i++) {
            String name = view.getClass().getName();
            if (MotionPolicy.protectedClass(name) || name.toLowerCase(Locale.ROOT).contains("media")) {
                return false;
            }
            if (name.startsWith("com.android.systemui.qs.")) {
                z = true;
            }
            Object parent2 = view.getParent();
            view = parent2 instanceof View ? (View) parent2 : null;
        }
        return z;
    }

    static void apply() {
        float f = context != null ? Settings.Global.getFloat(context.getContentResolver(), "animator_duration_scale", 1.0f) : 1.0f;
        for (Map.Entry entry : new ArrayList(scales.entrySet())) {
            try {
                setFloat.invoke(delegate.get(entry.getKey()), Float.valueOf(VisualPolicy.motionScale() * f));
                View view = (View) ((WeakReference) entry.getValue()).get();
                if (view != null) {
                    view.postInvalidateOnAnimation();
                }
                changed++;
            } catch (Throwable th) {
                ControlBus.feature("qs_compose_motion", "native fallback");
            }
        }
    }

    static void install(ClassLoader classLoader) {
        loader = classLoader;
        try {
            Class<?> clsType = type("androidx.compose.ui.platform.AbstractComposeView");
            Class<?> clsType2 = type("androidx.compose.runtime.CompositionContext");
            compositionField = clsType.getDeclaredField("composition");
            compositionField.setAccessible(true);
            parent = clsType.getDeclaredMethod("setParentCompositionContext", clsType2);
            parent.setAccessible(true);
            delegate = type("androidx.compose.ui.platform.MotionDurationScaleImpl").getDeclaredField("scaleFactor$delegate");
            delegate.setAccessible(true);
            setFloat = type("androidx.compose.runtime.SnapshotMutableFloatStateImpl").getDeclaredMethod("setFloatValue", Float.TYPE);
            setFloat.setAccessible(true);
            type("androidx.compose.ui.platform.WindowRecomposer_androidKt$createLifecycleAwareWindowRecomposer$2").getDeclaredConstructor(type("kotlinx.coroutines.CoroutineScope"), type("androidx.compose.runtime.PausableMonotonicFrameClock"), type("androidx.compose.runtime.Recomposer"), type("kotlin.jvm.internal.Ref$ObjectRef"), View.class);
            creationHook = RuntimeHooks.hookMethod(clsType.getDeclaredMethod("ensureCompositionCreated", new Class[0]), new XC_MethodHook(9000) { // from class: nihil.sunshine.parts.QsComposeMotion.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    View view = (View) methodHookParam.thisObject;
                    try {
                        if (QsComposeMotion.compositionField.get(view) != null) {
                            QsComposeMotion.fastPaths++;
                            return;
                        }
                        if (view.isAttachedToWindow() && QsComposeMotion.qs(view)) {
                            WeakReference weakReference = (WeakReference) QsComposeMotion.hosts.get(view);
                            if (weakReference == null || weakReference.get() == null) {
                                QsComposeMotion.hosts.put(view, new WeakReference(view));
                                try {
                                    QsComposeMotion.create(view);
                                } catch (Throwable th) {
                                    ControlBus.feature("qs_compose_motion", "native fallback");
                                    QsComposeMotion.hosts.put(view, new WeakReference(view));
                                }
                            }
                        }
                    } catch (Throwable th2) {
                    }
                }
            });
            ControlBus.feature("qs_compose_motion", "ready");
        } catch (Throwable th) {
            ControlBus.feature("qs_compose_motion", "unavailable");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void create(View view) throws Exception {
        Class<?> clsType = type("kotlin.coroutines.CoroutineContext");
        Class<?> clsType2 = type("kotlin.coroutines.CoroutineContext$Key");
        Object objInvoke = type("kotlin.Lazy").getMethod("getValue", new Class[0]).invoke(type("androidx.compose.ui.platform.AndroidUiDispatcher").getDeclaredField("Main$delegate").get(null), new Object[0]);
        Object objNewInstance = type("androidx.compose.runtime.PausableMonotonicFrameClock").getDeclaredConstructor(type("androidx.compose.runtime.MonotonicFrameClock")).newInstance(clsType.getMethod("get", clsType2).invoke(objInvoke, type("androidx.compose.runtime.MonotonicFrameClock").getDeclaredField("Key").get(null)));
        Object objField = field(objNewInstance, "latch");
        Object objField2 = field(objField, "lock");
        Field declaredField = objField.getClass().getDeclaredField("_isOpen");
        declaredField.setAccessible(true);
        synchronized (objField2) {
            declaredField.setBoolean(objField, false);
        }
        Object objNewInstance2 = type("androidx.compose.ui.platform.MotionDurationScaleImpl").getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        context = view.getContext().getApplicationContext();
        setFloat.invoke(delegate.get(objNewInstance2), Float.valueOf(Settings.Global.getFloat(context.getContentResolver(), "animator_duration_scale", 1.0f) * VisualPolicy.motionScale()));
        Method method = clsType.getMethod("plus", clsType);
        Object objInvoke2 = method.invoke(method.invoke(objInvoke, objNewInstance), objNewInstance2);
        final Object objNewInstance3 = type("androidx.compose.runtime.Recomposer").getDeclaredConstructor(clsType).newInstance(objInvoke2);
        Object objInvoke3 = type("kotlinx.coroutines.CoroutineScopeKt").getDeclaredMethod("CoroutineScope", clsType).invoke(null, objInvoke2);
        Object objNewInstance4 = type("kotlin.jvm.internal.Ref$ObjectRef").getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        Object objInvoke4 = type("androidx.lifecycle.ViewTreeLifecycleOwner").getDeclaredMethod("get", View.class).invoke(null, view);
        if (objInvoke4 == null) {
            objNewInstance3.getClass().getMethod("cancel", new Class[0]).invoke(objNewInstance3, new Object[0]);
            throw new IllegalStateException("Missing QS lifecycle owner");
        }
        final Object objInvoke5 = type("androidx.lifecycle.LifecycleOwner").getMethod("getLifecycle", new Class[0]).invoke(objInvoke4, new Object[0]);
        final Class<?> clsType3 = type("androidx.lifecycle.LifecycleObserver");
        final Class<?> clsType4 = type("androidx.lifecycle.Lifecycle");
        final Object objNewInstance5 = type("androidx.compose.ui.platform.WindowRecomposer_androidKt$createLifecycleAwareWindowRecomposer$2").getDeclaredConstructor(type("kotlinx.coroutines.CoroutineScope"), objNewInstance.getClass(), objNewInstance3.getClass(), objNewInstance4.getClass(), View.class).newInstance(objInvoke3, objNewInstance, objNewInstance3, objNewInstance4, view);
        try {
            parent.invoke(view, objNewInstance3);
            clsType4.getMethod("addObserver", clsType3).invoke(objInvoke5, objNewInstance5);
            hosts.put(view, new WeakReference<>(objNewInstance3));
            scales.put(objNewInstance2, new WeakReference<>(view));
            created++;
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() { // from class: nihil.sunshine.parts.QsComposeMotion.2
                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewAttachedToWindow(View view2) {
                }

                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewDetachedFromWindow(View view2) {
                    try {
                        clsType4.getMethod("removeObserver", clsType3).invoke(objInvoke5, objNewInstance5);
                        objNewInstance3.getClass().getMethod("cancel", new Class[0]).invoke(objNewInstance3, new Object[0]);
                        QsComposeMotion.parent.invoke(view2, null);
                    } catch (Throwable th) {
                    }
                    QsComposeMotion.hosts.remove(view2);
                    view2.removeOnAttachStateChangeListener(this);
                }
            });
            if (!observing) {
                observing = true;
                context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("animator_duration_scale"), false, new ContentObserver(new Handler(Looper.getMainLooper())) { // from class: nihil.sunshine.parts.QsComposeMotion.3
                    @Override // android.database.ContentObserver
                    public void onChange(boolean z) {
                        QsComposeMotion.apply();
                    }
                });
            }
        } catch (Throwable th) {
            parent.invoke(view, null);
            objNewInstance3.getClass().getMethod("cancel", new Class[0]).invoke(objNewInstance3, new Object[0]);
            throw new IllegalStateException(th);
        }
    }

    private QsComposeMotion() {
    }
}
