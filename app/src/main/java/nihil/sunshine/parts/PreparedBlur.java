package nihil.sunshine.parts;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
final class PreparedBlur {
    private static final String BASE = "com.android.systemui.blur.";
    private static Context application;
    static long applied;
    private static Field binderGenerator;
    private static Field binderView;
    private static Field bouncer;
    private static Method currentUser;
    static long discarded;
    private static volatile long epoch;
    static long errors;
    private static CloneFactory factory;
    private static boolean failed;
    static long fractionFastPaths;
    private static BlurFractionWork.Access fractionWork;
    private static Method generate;
    private static KeyguardManager keyguard;
    private static boolean listeners;
    private static Handler main;
    static long nativeCapturesReduced;
    static long nativeMainCalls;
    static long nativeUnscoped;
    static long openingRequests;
    static volatile boolean panelVisible;
    private static boolean policyReload;
    private static PowerManager power;
    private static Object quick;
    private static BitmapDrawable readyImage;
    private static Request readyRequest;
    static long replaced;
    static long requested;
    static long softwareCaptures;
    static long sourceCapturesReduced;
    private static Request submittedRequest;
    static long warmups;
    private static Handler worker;
    static long workerMaxMs;
    private static final LatestOnly<Request> queue = new LatestOnly<>();
    private static final ThreadLocal<Object> caller = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> cloning = new ThreadLocal<>();
    private static final ThreadLocal<Capture> capture = new ThreadLocal<>();
    private static final WeakHashMap<Object, Boolean> open = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> nativeQuickCapture = new ThreadLocal<>();
    private static WeakReference<Object> lastBinder = new WeakReference<>(null);
    private static WeakReference<Object> lastGenerator = new WeakReference<>(null);
    private static final Runnable expireReady = new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            PreparedBlur.lambda$static$0();
        }
    };
    private static WeakReference<View> paintedView = new WeakReference<>(null);
    private static long paintedEpoch = -1;

    /* JADX INFO: Access modifiers changed from: private */
    static final class Capture {
        final LatestOnly.Work<Request> work;

        Capture(LatestOnly.Work<Request> work) {
            this.work = work;
        }
    }

    static void policyChanged() {
        if (panelVisible) {
            policyReload = true;
        } else {
            reload();
        }
    }

    static void trimIdle() {
        if (panelVisible || main == null) {
            return;
        }
        reset();
    }

    static /* synthetic */ void lambda$static$0() {
        readyRequest = null;
        readyImage = null;
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
    static final class CloneFactory {
        final Field blurFilter;
        final Field displayLifecycle;
        final Constructor<?> generator;
        Object prepared;
        final Constructor<?> screenshot;
        final Field screenshotField;
        final Field settings;
        final Constructor<?> wallpaper;
        final Method wallpaperBitmap;
        final Field wallpaperContext;
        final Field wallpaperField;
        final Constructor<?> window;
        final Field windowContext;
        final Field windowField;
        WeakReference<Object> source = new WeakReference<>(null);
        final Field[] dependencies = new Field[6];

        CloneFactory(ClassLoader classLoader, Class<?> cls) throws Exception {
            String[] strArr = {"context", "secBlurCustomColorInteractor", "notificationShadeWindowStateInteractor", "keyguardFoldController", "screenShotBitmapFactory", "keyguardUpdateMonitor"};
            this.blurFilter = PreparedBlur.field(cls, "blurFilter");
            Class<?>[] clsArr = new Class[6];
            for (int i = 0; i < 6; i++) {
                this.dependencies[i] = PreparedBlur.field(cls, strArr[i]);
                clsArr[i] = this.dependencies[i].getType();
            }
            this.generator = cls.getDeclaredConstructor(clsArr);
            this.generator.setAccessible(true);
            this.screenshotField = this.dependencies[4];
            Class<?> type = this.screenshotField.getType();
            this.windowField = PreparedBlur.field(type, "windowManagerScreenShotProvider");
            this.wallpaperField = PreparedBlur.field(type, "wallpaperScreenShotProvider");
            Class<?> type2 = this.windowField.getType();
            Class<?> type3 = this.wallpaperField.getType();
            this.windowContext = PreparedBlur.field(type2, "context");
            this.displayLifecycle = PreparedBlur.field(type2, "displayLifecycle");
            this.wallpaperContext = PreparedBlur.field(type3, "context");
            this.settings = PreparedBlur.field(type3, "settingsInteractor");
            this.window = type2.getDeclaredConstructor(Context.class, this.displayLifecycle.getType());
            this.wallpaper = type3.getDeclaredConstructor(Context.class, this.settings.getType());
            this.screenshot = type.getDeclaredConstructor(type2, type3);
            this.wallpaperBitmap = type3.getDeclaredMethod("getScreenShot", new Class[0]);
            this.wallpaperBitmap.setAccessible(true);
            this.window.setAccessible(true);
            this.wallpaper.setAccessible(true);
            this.screenshot.setAccessible(true);
        }

        Object get(Object obj) throws Exception {
            if (this.source.get() == obj && this.prepared != null) {
                return this.prepared;
            }
            int length = this.dependencies.length;
            Object[] objArr = new Object[length];
            for (int i = 0; i < length; i++) {
                objArr[i] = this.dependencies[i].get(obj);
            }
            Object obj2 = this.screenshotField.get(obj);
            Object obj3 = this.windowField.get(obj2);
            Object obj4 = this.wallpaperField.get(obj2);
            objArr[4] = this.screenshot.newInstance(this.window.newInstance(this.windowContext.get(obj3), this.displayLifecycle.get(obj3)), this.wallpaper.newInstance(this.wallpaperContext.get(obj4), this.settings.get(obj4)));
            PreparedBlur.cloning.set(Boolean.TRUE);
            try {
                this.prepared = this.generator.newInstance(objArr);
                PreparedBlur.cloning.remove();
                this.source = new WeakReference<>(obj);
                return this.prepared;
            } catch (Throwable th) {
                PreparedBlur.cloning.remove();
                throw th;
            }
        }

        Bitmap wallpaperPreview(Object obj) throws Exception {
            Object obj2 = this.wallpaperField.get(this.screenshotField.get(obj));
            return (Bitmap) this.wallpaperBitmap.invoke(this.wallpaper.newInstance(this.wallpaperContext.get(obj2), this.settings.get(obj2)), new Object[0]);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void clear() {
            this.prepared = null;
            this.source.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Request {
        final WeakReference<Object> binder;
        final Configuration configuration;
        final int display;
        final boolean hardware;
        final Object original;
        final Resources resources;
        final int rotation;
        final WeakReference<View> view;
        final Object windowToken;
        final long epoch = PreparedBlur.epoch;
        final int user = ((Number) PreparedBlur.currentUser.invoke(null, new Object[0])).intValue();
        final boolean locked = PreparedBlur.keyguard.isKeyguardLocked();

        Request(Object obj, View view, Object obj2) throws Exception {
            this.binder = new WeakReference<>(obj);
            this.view = new WeakReference<>(view);
            this.original = obj2;
            this.windowToken = view.getWindowToken();
            this.resources = view.getResources();
            this.configuration = new Configuration(this.resources.getConfiguration());
            this.display = view.getDisplay().getDisplayId();
            this.rotation = view.getDisplay().getRotation();
            this.hardware = view.isHardwareAccelerated();
        }

        boolean local() {
            try {
                View view = this.view.get();
                Object obj = this.binder.get();
                return !PreparedBlur.failed && VisualPolicy.nativeBlur() && VisualPolicy.workerBlur() && this.epoch == PreparedBlur.epoch && obj != null && !PreparedBlur.bouncer.getBoolean(obj) && view != null && view.isAttachedToWindow() && view.getWindowToken() == this.windowToken && view.getDisplay() != null && view.getDisplay().getDisplayId() == this.display && view.getDisplay().getRotation() == this.rotation && this.configuration.equals(view.getResources().getConfiguration());
            } catch (Throwable th) {
                return false;
            }
        }

        boolean valid() {
            try {
                return local() && PreparedBlur.power.isInteractive() && PreparedBlur.keyguard.isKeyguardLocked() == this.locked && this.user == ((Number) PreparedBlur.currentUser.invoke(null, new Object[0])).intValue();
            } catch (Throwable th) {
                return false;
            }
        }
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            Class<?> clsType = type(classLoader, "com.android.systemui.blur.domain.interactor.SecCapturedBlurBitmapGenerator");
            Class<?> clsType2 = type(classLoader, "com.android.systemui.blur.di.SecPanelBlurBinding$BlurType");
            quick = field(clsType2, "QUICK_PANEL").get(null);
            generate = clsType.getDeclaredMethod("getBlurredBitmapWithEffect", clsType2);
            generate.setAccessible(true);
            if (generate.getReturnType() != BitmapDrawable.class) {
                throw new NoSuchMethodException("Blur result");
            }
            factory = new CloneFactory(classLoader, clsType);
            currentUser = ActivityManager.class.getDeclaredMethod("semGetCurrentUser", new Class[0]);
            currentUser.setAccessible(true);
            Class<?> clsType3 = type(classLoader, "com.android.systemui.blur.ui.viewbinder.SecCapturedBlurContainerBinder");
            binderView = field(clsType3, "view");
            bouncer = field(clsType3, "bouncerShowing");
            binderGenerator = field(clsType3, "secCapturedBlurBitmapGenerator");
            if (binderGenerator.getType() != clsType) {
                throw new NoSuchFieldException("Binder generator");
            }
            Method declaredMethod = clsType3.getDeclaredMethod("setFraction", Float.TYPE, clsType2);
            try {
                if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
                    throw new IllegalStateException("Unrecognized SystemUI DEX");
                }
                fractionWork = new BlurFractionWork.Access(clsType3);
                ControlBus.feature("blur_fraction_work", "Native alpha and mirror state without diagnostic strings");
                if (DensityQueries.pinned(str) && IconDimensions.pinned(str)) {
                    installCaptureSize(classLoader);
                }
                Class<?> clsType4 = type(classLoader, "com.android.systemui.blur.ui.viewbinder.SecCapturedBlurContainerBinder$1$1$3");
                Class<?> clsType5 = type(classLoader, "com.android.systemui.blur.ui.viewbinder.SecCapturedBlurContainerBinder$2$1");
                Field field = field(clsType4, "this$0");
                Field field2 = field(clsType5, "this$0");
                Method declaredMethod2 = clsType4.getDeclaredMethod("invokeSuspend", Object.class);
                Method declaredMethod3 = clsType5.getDeclaredMethod("run", new Class[0]);
                main = new Handler(Looper.getMainLooper());
                scope(declaredMethod2, field);
                scope(declaredMethod3, field2);
                RuntimeHooks.hookMethod(declaredMethod, new AnonymousClass1());
                RuntimeHooks.hookMethod(factory.generator, new AnonymousClass2());
                RuntimeHooks.hookMethod(generate, new XC_MethodHook() { // from class: nihil.sunshine.parts.PreparedBlur.3
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (Looper.myLooper() != Looper.getMainLooper()) {
                            return;
                        }
                        if (methodHookParam.args[0] != PreparedBlur.quick) {
                            PreparedBlur.invalidate();
                            return;
                        }
                        PreparedBlur.nativeMainCalls++;
                        if (AdaptiveWorkload.level() >= 2) {
                            PreparedBlur.nativeQuickCapture.set(Boolean.TRUE);
                        }
                        if (!PreparedBlur.failed && VisualPolicy.workerBlur() && VisualPolicy.nativeBlur()) {
                            Object obj = PreparedBlur.caller.get();
                            if (obj == null) {
                                PreparedBlur.nativeUnscoped++;
                                return;
                            }
                            try {
                                Request requestBegin = PreparedBlur.begin(obj, methodHookParam.thisObject);
                                if (requestBegin == null) {
                                    return;
                                }
                                methodHookParam.setResult(PreparedBlur.opening(requestBegin.view.get(), requestBegin.user));
                            } catch (Throwable th) {
                                PreparedBlur.disable(th);
                            }
                        }
                    }

                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            PreparedBlur.nativeQuickCapture.remove();
                        }
                    }
                });
                ControlBus.feature("prepared_blur", "ready");
            } catch (Throwable th) {
                fractionWork = null;
                ControlBus.feature("blur_fraction_work", "Native fallback");
            }
        } catch (Throwable th2) {
            failed = true;
            ControlBus.feature("prepared_blur", "native fallback");
            Log.w("SunshinePartsUI", "Prepared blur unavailable; native capture retained", th2);
        }
    }

    /* JADX INFO: renamed from: nihil.sunshine.parts.PreparedBlur$1, reason: invalid class name */
    class AnonymousClass1 extends XC_MethodHook {
        AnonymousClass1() {
        }

        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            if (methodHookParam.args[1] == PreparedBlur.quick) {
                PreparedBlur.panelVisible = ((Float) methodHookParam.args[0]).floatValue() > 0.0f;
                if (!PreparedBlur.panelVisible && PreparedBlur.policyReload) {
                    boolean unused = PreparedBlur.policyReload = false;
                    PreparedBlur.main.post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$1$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            PreparedBlur.reload();
                        }
                    });
                }
            }
            if (PreparedBlur.failed || !VisualPolicy.nativeBlur()) {
                return;
            }
            if (!VisualPolicy.workerBlur()) {
                if (methodHookParam.args[1] == PreparedBlur.quick && PreparedBlur.fractionWork != null && VisualPolicy.leanQs()) {
                    try {
                        if (PreparedBlur.fractionWork.apply(methodHookParam.thisObject, ((Float) methodHookParam.args[0]).floatValue())) {
                            methodHookParam.setResult((Object) null);
                            return;
                        }
                        return;
                    } catch (Throwable th) {
                        BlurFractionWork.Access unused2 = PreparedBlur.fractionWork = null;
                        BlurFractionWork.error = th.getClass().getSimpleName();
                        return;
                    }
                }
                return;
            }
            if (methodHookParam.args[1] != PreparedBlur.quick) {
                if (((Float) methodHookParam.args[0]).floatValue() > 0.0f) {
                    PreparedBlur.invalidate();
                    return;
                }
                return;
            }
            if (PreparedBlur.fractionWork != null && VisualPolicy.leanQs() && AdaptiveWorkload.reduced()) {
                try {
                    if (PreparedBlur.fractionWork.apply(methodHookParam.thisObject, ((Float) methodHookParam.args[0]).floatValue())) {
                        methodHookParam.setResult((Object) null);
                    }
                } catch (Throwable th2) {
                    BlurFractionWork.Access unused3 = PreparedBlur.fractionWork = null;
                    BlurFractionWork.error = th2.getClass().getSimpleName();
                    ControlBus.feature("blur_fraction_work", "Native fallback");
                }
            }
            boolean z = ((Float) methodHookParam.args[0]).floatValue() > 0.0f;
            Boolean bool = (Boolean) PreparedBlur.open.get(methodHookParam.thisObject);
            if (bool == null || bool.booleanValue() != z) {
                PreparedBlur.open.put(methodHookParam.thisObject, Boolean.valueOf(z));
            }
            if (!z) {
                if (Boolean.TRUE.equals(bool) || PreparedBlur.readyImage != null) {
                    PreparedBlur.invalidate();
                }
            }
        }

        protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            if (!PreparedBlur.failed && VisualPolicy.workerBlur() && VisualPolicy.nativeBlur() && !methodHookParam.hasThrowable() && methodHookParam.args[1] == PreparedBlur.quick && ((Float) methodHookParam.args[0]).floatValue() > 0.0f) {
                Request request = PreparedBlur.submittedRequest;
                View view = (View) PreparedBlur.paintedView.get();
                if (PreparedBlur.readyRequest != null || !PreparedBlur.sameOpening(request, methodHookParam.thisObject, PreparedBlur.epoch) || PreparedBlur.paintedEpoch != PreparedBlur.epoch || view == null || request.view.get() != view || view.getBackground() == null) {
                    if (PreparedBlur.readyRequest != null && PreparedBlur.readyRequest.binder.get() == methodHookParam.thisObject) {
                        if (PreparedBlur.readyRequest.valid()) {
                            PreparedBlur.apply(PreparedBlur.readyRequest, PreparedBlur.readyImage);
                        } else {
                            PreparedBlur.discarded++;
                        }
                        PreparedBlur.clearReady();
                    }
                    try {
                        View view2 = (View) PreparedBlur.binderView.get(methodHookParam.thisObject);
                        if (view2 != null && !PreparedBlur.bouncer.getBoolean(methodHookParam.thisObject)) {
                            Request requestBegin = PreparedBlur.submittedRequest;
                            if (!PreparedBlur.sameOpening(requestBegin, methodHookParam.thisObject, PreparedBlur.epoch) && (requestBegin = PreparedBlur.begin(methodHookParam.thisObject, PreparedBlur.binderGenerator.get(methodHookParam.thisObject))) != null) {
                                PreparedBlur.openingRequests++;
                            }
                            if (requestBegin != null) {
                                if (view2.getBackground() == null || PreparedBlur.paintedView.get() != view2 || PreparedBlur.paintedEpoch != PreparedBlur.epoch) {
                                    view2.setBackgroundDrawable(PreparedBlur.opening(view2, requestBegin.user));
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        return;
                    } catch (Throwable th) {
                        PreparedBlur.disable(th);
                        return;
                    }
                }
                PreparedBlur.fractionFastPaths++;
            }
        }
    }

    /* JADX INFO: renamed from: nihil.sunshine.parts.PreparedBlur$2, reason: invalid class name */
    class AnonymousClass2 extends XC_MethodHook {
        AnonymousClass2() {
        }

        protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            if (methodHookParam.hasThrowable() || Boolean.TRUE.equals(PreparedBlur.cloning.get())) {
                return;
            }
            final WeakReference weakReference = new WeakReference(methodHookParam.thisObject);
            PreparedBlur.main.post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PreparedBlur.AnonymousClass2.lambda$afterHookedMethod$0(weakReference);
                }
            });
        }

        static /* synthetic */ void lambda$afterHookedMethod$0(WeakReference weakReference) {
            Object obj = weakReference.get();
            if (obj != null) {
                PreparedBlur.remember(obj);
            }
        }
    }

    private static void installCaptureSize(ClassLoader classLoader) {
        try {
            Method declaredMethod = type(classLoader, "com.samsung.android.view.SemWindowManager").getDeclaredMethod("screenshot", Integer.TYPE, Integer.TYPE, Boolean.TYPE, Rect.class, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE, Boolean.TYPE);
            if (declaredMethod.getReturnType() != Bitmap.class) {
                throw new NoSuchMethodException("Screenshot result");
            }
            if (RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.PreparedBlur.4
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Capture capture2 = (Capture) PreparedBlur.capture.get();
                    boolean zEquals = Boolean.TRUE.equals(PreparedBlur.nativeQuickCapture.get());
                    boolean z = capture2 != null && AdaptiveWorkload.reduced() && PreparedBlur.queue.current(capture2.work) && capture2.work.value.epoch == PreparedBlur.epoch;
                    if ((zEquals || z) && CaptureSize.reduce(methodHookParam.args, AdaptiveWorkload.previewWidth(), AdaptiveWorkload.previewHeight())) {
                        if (zEquals) {
                            PreparedBlur.nativeCapturesReduced++;
                        } else {
                            PreparedBlur.sourceCapturesReduced++;
                        }
                    }
                }
            }) != null) {
                ControlBus.feature("blur_source_size", "Worker QUICK_PANEL pixels reduced before screenshot IPC");
                return;
            }
            throw new IllegalStateException("Screenshot hook rejected");
        } catch (Throwable th) {
            ControlBus.feature("blur_source_size", "Native capture size; reduced filter retained");
        }
    }

    private static void scope(Method method, final Field field) {
        RuntimeHooks.hookMethod(method, new XC_MethodHook() { // from class: nihil.sunshine.parts.PreparedBlur.5
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    return;
                }
                try {
                    PreparedBlur.caller.set(field.get(methodHookParam.thisObject));
                } catch (Throwable th) {
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                PreparedBlur.caller.remove();
            }
        });
    }

    private static Handler worker() {
        if (worker == null) {
            HandlerThread handlerThread = new HandlerThread("SunshinePanelBlur", 0);
            handlerThread.start();
            worker = new Handler(handlerThread.getLooper());
        }
        return worker;
    }

    private static void attach(Context context) {
        if (listeners) {
            return;
        }
        application = context.getApplicationContext();
        if (application == null) {
            application = context;
        }
        power = (PowerManager) application.getSystemService(PowerManager.class);
        keyguard = (KeyguardManager) application.getSystemService(KeyguardManager.class);
        IntentFilter intentFilter = new IntentFilter();
        String[] strArr = {"android.intent.action.SCREEN_OFF", "android.intent.action.SCREEN_ON", "android.intent.action.USER_PRESENT", "android.intent.action.USER_SWITCHED", "android.intent.action.WALLPAPER_CHANGED"};
        for (int i = 0; i < 5; i++) {
            intentFilter.addAction(strArr[i]);
        }
        application.registerReceiver(new BroadcastReceiver() { // from class: nihil.sunshine.parts.PreparedBlur.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                PreparedBlur.reset();
                if (PreparedBlur.power.isInteractive()) {
                    PreparedBlur.prepareLastPreview();
                }
            }
        }, intentFilter, 2);
        application.registerComponentCallbacks(new ComponentCallbacks2() { // from class: nihil.sunshine.parts.PreparedBlur.7
            @Override // android.content.ComponentCallbacks
            public void onConfigurationChanged(Configuration configuration) {
                PreparedBlur.reset();
                PreparedBlur.prepareLastPreview();
            }

            @Override // android.content.ComponentCallbacks
            public void onLowMemory() {
                PreparedBlur.reset();
            }

            @Override // android.content.ComponentCallbacks2
            public void onTrimMemory(int i2) {
                if (i2 == 10 || i2 == 15 || i2 >= 40) {
                    PreparedBlur.reset();
                }
            }
        });
        listeners = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void remember(Object obj) {
        remember(obj, -1);
    }

    private static void remember(final Object obj, int i) {
        if (failed || obj == null) {
            return;
        }
        try {
            attach((Context) factory.dependencies[0].get(obj));
            boolean z = lastGenerator.get() != obj;
            if (z) {
                lastGenerator = new WeakReference<>(obj);
            }
            if (VisualPolicy.workerBlur() && VisualPolicy.nativeBlur()) {
                if (z || !PanelBackdrop.isReady()) {
                    preparePreview(obj, i);
                }
                if (!z || !VisualPolicy.capturePanel()) {
                    return;
                }
                worker().post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        PreparedBlur.lambda$remember$2(obj);
                    }
                });
            }
        } catch (Throwable th) {
            PanelBackdrop.error = th.getClass().getSimpleName();
        }
    }

    static /* synthetic */ void lambda$remember$1() {
        warmups++;
    }

    static /* synthetic */ void lambda$remember$2(Object obj) {
        try {
            factory.get(obj);
            main.post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    PreparedBlur.lambda$remember$1();
                }
            });
        } catch (Throwable th) {
            disable(th);
        }
    }

    private static void preparePreview(Object obj) {
        preparePreview(obj, -1);
    }

    private static void preparePreview(final Object obj, int i) {
        try {
            if (!failed && VisualPolicy.preparedBlur() && VisualPolicy.nativeBlur() && power.isInteractive()) {
                final Context context = (Context) factory.dependencies[0].get(obj);
                if (i < 0) {
                    i = ((Number) currentUser.invoke(null, new Object[0])).intValue();
                }
                PanelBackdrop.prepare(context, i, new PanelBackdrop.WallpaperSource() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda3
                    @Override // nihil.sunshine.parts.PanelBackdrop.WallpaperSource
                    public final Bitmap load() {
                        return PreparedBlur.lambda$preparePreview$3(obj, context);
                    }
                }, new PanelBackdrop.UserCheck() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda4
                    @Override // nihil.sunshine.parts.PanelBackdrop.UserCheck
                    public final int current() {
                        return ((Number) PreparedBlur.currentUser.invoke(null, new Object[0])).intValue();
                    }
                });
            }
        } catch (Throwable th) {
        }
    }

    static /* synthetic */ Bitmap lambda$preparePreview$3(Object obj, Context context) throws Exception {
        Bitmap bitmapWallpaperPreview;
        try {
            bitmapWallpaperPreview = factory.wallpaperPreview(obj);
        } catch (Throwable th) {
            bitmapWallpaperPreview = null;
        }
        if (bitmapWallpaperPreview != null) {
            return bitmapWallpaperPreview;
        }
        Drawable drawable = WallpaperManager.getInstance(context).getDrawable();
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void prepareLastPreview() {
        Object obj = lastGenerator.get();
        if (obj != null) {
            preparePreview(obj);
        }
    }

    static void previewReady() {
        Request request;
        View view;
        if (VisualPolicy.capturePanel() || (request = submittedRequest) == null) {
            return;
        }
        try {
            if (request.valid() && (view = request.view.get()) != null && view.getAlpha() > 0.0f) {
                view.setBackgroundDrawable(PanelBackdrop.opening(view.getResources(), request.user));
                view.postInvalidateOnAnimation();
                paintedView = new WeakReference<>(view);
                paintedEpoch = epoch;
            }
        } catch (Throwable th) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void clearReady() {
        readyRequest = null;
        readyImage = null;
        if (main != null) {
            main.removeCallbacks(expireReady);
        }
    }

    static void invalidate() {
        epoch++;
        queue.invalidate();
        submittedRequest = null;
        clearReady();
    }

    static void reset() {
        invalidate();
        PanelBackdrop.reset();
        if (worker != null) {
            worker.post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    PreparedBlur.factory.clear();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void reload() {
        reset();
        prepareLastPreview();
        try {
            Object obj = lastBinder.get();
            Object obj2 = lastGenerator.get();
            if (!failed && VisualPolicy.nativeBlur() && obj != null && obj2 != null) {
                View view = (View) binderView.get(obj);
                if (view.isAttachedToWindow() && view.getAlpha() > 0.0f && !bouncer.getBoolean(obj)) {
                    if (VisualPolicy.workerBlur()) {
                        Request request = new Request(obj, view, obj2);
                        if (VisualPolicy.capturePanel()) {
                            submit(request);
                        } else {
                            submittedRequest = request;
                            clearReady();
                        }
                        view.setBackgroundDrawable(opening(view, request.user));
                        return;
                    }
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) generate.invoke(obj2, quick);
                    if (bitmapDrawable != null) {
                        view.setBackgroundDrawable(bitmapDrawable);
                        view.postInvalidateOnAnimation();
                    }
                }
            }
        } catch (Throwable th) {
            disable(th);
        }
    }

    private static void submit(Request request) {
        submittedRequest = request;
        clearReady();
        LatestOnly.Work<Request> workOffer = queue.offer(request);
        requested++;
        if (workOffer == null) {
            replaced++;
        } else {
            dispatch(workOffer);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean sameOpening(Request request, Object obj, long j) {
        return request != null && request.epoch == j && request.binder.get() == obj;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Request begin(Object obj, Object obj2) throws Exception {
        if (sameOpening(submittedRequest, obj, epoch)) {
            return submittedRequest;
        }
        attach((Context) factory.dependencies[0].get(obj2));
        View view = (View) binderView.get(obj);
        if (view == null || !view.isAttachedToWindow() || view.getDisplay() == null || bouncer.getBoolean(obj) || !power.isInteractive()) {
            return null;
        }
        lastBinder = new WeakReference<>(obj);
        Request request = new Request(obj, view, obj2);
        remember(obj2, request.user);
        if (VisualPolicy.capturePanel()) {
            submit(request);
        } else {
            submittedRequest = request;
            clearReady();
        }
        return request;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void disable(final Throwable th) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    PreparedBlur.disable(th);
                }
            });
            return;
        }
        if (!failed) {
            Log.w("SunshinePartsUI", "Prepared blur disabled; native capture retained", th);
        }
        try {
            Request request = submittedRequest;
            View view = paintedView.get();
            if (request != null && view != null && request.view.get() == view && paintedEpoch == epoch && request.valid()) {
                view.setBackgroundDrawable(null);
            }
        } catch (Throwable th2) {
        }
        failed = true;
        errors++;
        reset();
        ControlBus.feature("prepared_blur", "native fallback");
    }

    private static void dispatch(final LatestOnly.Work<Request> work) {
        if (!work.value.local()) {
            finish(work, null, 0L);
        } else if (!worker().post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                PreparedBlur.lambda$dispatch$8(work);
            }
        })) {
            disable(new IllegalStateException("Blur worker unavailable"));
            finish(work, null, 0L);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v0 */
    /* JADX WARN: Type inference failed for: r2v10 */
    /* JADX WARN: Type inference failed for: r2v14 */
    /* JADX WARN: Type inference failed for: r2v15 */
    /* JADX WARN: Type inference failed for: r2v2, types: [android.graphics.drawable.BitmapDrawable] */
    /* JADX WARN: Type inference failed for: r2v4 */
    /* JADX WARN: Type inference failed for: r3v1 */
    /* JADX WARN: Type inference failed for: r3v11, types: [boolean] */
    /* JADX WARN: Type inference failed for: r3v4 */
    static /* synthetic */ void lambda$dispatch$8(final LatestOnly.Work work) {
        ?? CapturePanel;
        Throwable th;
        Throwable th2;
        long jUptimeMillis = SystemClock.uptimeMillis();
        final ?? r2 = 0;
        r2 = 0;
        r2 = 0;
        try {
            if (queue.current(work) && ((Request) work.value).epoch == epoch && (CapturePanel = VisualPolicy.capturePanel()) != 0) {
                try {
                    Trace.beginSection("Sunshine:NativePanelBlur");
                    try {
                        Object obj = factory.get(((Request) work.value).original);
                        capture.set(new Capture(work));
                        try {
                            BitmapDrawable bitmapDrawable = (BitmapDrawable) generate.invoke(obj, quick);
                            try {
                                capture.remove();
                                if (bitmapDrawable != null && !bitmapDrawable.getBitmap().isRecycled()) {
                                    bitmapDrawable.getBitmap().prepareToDraw();
                                }
                                Trace.endSection();
                                r2 = bitmapDrawable;
                            } catch (Throwable th3) {
                                th2 = th3;
                                Trace.endSection();
                                throw th2;
                            }
                        } catch (Throwable th4) {
                            capture.remove();
                            throw th4;
                        }
                    } catch (Throwable th5) {
                        th2 = th5;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    if (queue.current(work) && ((Request) work.value).epoch == epoch) {
                        disable(th);
                    }
                    r2 = CapturePanel;
                }
            }
        } catch (Throwable th7) {
            CapturePanel = 0;
            th = th7;
        }
        final long jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
        main.post(new Runnable() { // from class: nihil.sunshine.parts.PreparedBlur$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                PreparedBlur.finish(work, r2, jUptimeMillis2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void finish(LatestOnly.Work<Request> work, BitmapDrawable bitmapDrawable, long j) {
        workerMaxMs = Math.max(workerMaxMs, j);
        LatestOnly.Completion<Request> completionComplete = queue.complete(work);
        work.value.view.get();
        try {
            if (completionComplete.current && VisualPolicy.capturePanel() && bitmapDrawable != null && work.value.valid()) {
                present(work.value, bitmapDrawable);
            } else {
                discarded++;
            }
        } catch (Throwable th) {
            discarded++;
            disable(th);
        }
        if (completionComplete.next != null) {
            dispatch(completionComplete.next);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static BitmapDrawable opening(View view, int i) {
        if (paintedView.get() == view && paintedEpoch == epoch && (view.getBackground() instanceof BitmapDrawable)) {
            return (BitmapDrawable) view.getBackground();
        }
        paintedView = new WeakReference<>(view);
        paintedEpoch = epoch;
        return PanelBackdrop.opening(view.getResources(), i);
    }

    private static void present(Request request, BitmapDrawable bitmapDrawable) {
        View view = request.view.get();
        if (view == null) {
            return;
        }
        clearReady();
        if (view.getAlpha() <= 0.0f) {
            readyRequest = request;
            readyImage = bitmapDrawable;
            main.postDelayed(expireReady, 1000L);
            return;
        }
        apply(request, bitmapDrawable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void apply(Request request, BitmapDrawable bitmapDrawable) {
        View view = request.view.get();
        if (view == null || bitmapDrawable == null) {
            return;
        }
        Trace.beginSection("Sunshine:ApplyPanelBlur");
        try {
            view.setBackgroundDrawable(bitmapDrawable);
            view.postInvalidateOnAnimation();
            applied++;
            paintedView = new WeakReference<>(view);
            paintedEpoch = epoch;
        } finally {
            Trace.endSection();
        }
    }

    private PreparedBlur() {
    }
}
