package nihil.lain.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.view.Display;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
final class AsyncBlur {
    static final String BINDER = "com.android.systemui.blur.ui.viewbinder.SecCapturedBlurContainerBinder";
    static final String COLLECTOR = "com.android.systemui.blur.ui.viewbinder.SecCapturedBlurContainerBinder$1$1$3";
    static final String GENERATOR = "com.android.systemui.blur.domain.interactor.SecCapturedBlurBitmapGenerator";
    static final String WM = "com.android.systemui.blur.domain.interactor.WindowManagerScreenShotProvider";
    static final String WP = "com.android.systemui.blur.domain.interactor.WallpaperScreenShotProvider";
    long applied;
    final Method applyFilter;
    final Field binderGenerator;
    final Field binderView;
    final Field bouncer;
    long coalesced;
    final Method collect;
    final Field collectorOwner;
    final Field collectorType;
    final Field colorInteractor;
    final Constructor<?> curveConstructor;
    final Field[] curveFields;
    final Method curveFraction;
    final Field custom;
    long discarded;
    volatile boolean enabled;
    long errors;
    final Field factoryField;
    final Field factoryWm;
    final Field factoryWp;
    volatile boolean failed;
    final Constructor<?> filterConstructor;
    final Method[] filterSetters;
    final Method fraction;
    final Field generatorContext;
    Object isolatedFilter;
    final Field label;
    long lastLog;
    double maxLaunchMs;
    final Field minimal;
    final Field mode;
    final Object quick;
    boolean receiverAdded;
    long scheduled;
    final Field settings;
    final Field stateInteractor;
    final Field status;
    final Method tablet;
    final Object unit;
    final Method user;
    final Method value;
    final Field wallpaperType;
    final Field wallpaperWhich;
    final Constructor<?> wmConstructor;
    final Field wmContext;
    final Field wmLifecycle;
    final Method wmShot;
    final Constructor<?> wpConstructor;
    final Field wpContext;
    final Field wpPrev;
    final Field wpSettings;
    final Method wpShot;
    final Handler main = new Handler(Looper.getMainLooper());
    final WeakHashMap<Object, Session> sessions = new WeakHashMap<>();
    final AtomicLong environment = new AtomicLong();
    final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue(1), new ThreadFactory() { // from class: nihil.lain.sunshine.AsyncBlur$$ExternalSyntheticLambda0
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return AsyncBlur.lambda$new$1(runnable);
        }
    });

    static /* synthetic */ Thread lambda$new$1(final Runnable runnable) {
        Thread thread = new Thread(new Runnable() { // from class: nihil.lain.sunshine.AsyncBlur$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AsyncBlur.lambda$new$0(runnable);
            }
        }, "SunshineUI-blur");
        thread.setDaemon(true);
        return thread;
    }

    static /* synthetic */ void lambda$new$0(Runnable runnable) {
        Process.setThreadPriority(10);
        runnable.run();
    }

    static final class Session {
        Job job;
        volatile long epoch = 1;
        boolean active = true;
        long emptyEpoch = -1;

        Session() {
        }
    }

    AsyncBlur(ClassLoader classLoader, boolean z) throws Exception {
        if (!z) {
            throw new IllegalArgumentException("Unverified blur DEX");
        }
        Class<?> cls = Class.forName(BINDER, false, classLoader);
        Class<?> cls2 = Class.forName(GENERATOR, false, classLoader);
        Class<?> cls3 = Class.forName("com.android.systemui.blur.di.SecPanelBlurBinding$BlurType", false, classLoader);
        Object obj = null;
        for (Object obj2 : cls3.getEnumConstants()) {
            if (((Enum) obj2).name().equals("QUICK_PANEL")) {
                obj = obj2;
            }
        }
        if (obj != null) {
            this.quick = obj;
            this.unit = Class.forName("kotlin.Unit", false, classLoader).getField("INSTANCE").get(null);
            Method method = SunshineModule.method(classLoader, COLLECTOR, "invokeSuspend", Object.class);
            this.collect = method;
            Field field = SunshineModule.field(method.getDeclaringClass(), "this$0");
            this.collectorOwner = field;
            this.collectorType = SunshineModule.field(method.getDeclaringClass(), "L$0");
            Field field2 = SunshineModule.field(method.getDeclaringClass(), "label");
            this.label = field2;
            Method method2 = SunshineModule.method(classLoader, BINDER, "setFraction", Float.TYPE, cls3);
            this.fraction = method2;
            Field field3 = SunshineModule.field(cls, "view");
            this.binderView = field3;
            Field field4 = SunshineModule.field(cls, "secCapturedBlurBitmapGenerator");
            this.binderGenerator = field4;
            Field field5 = SunshineModule.field(cls, "bouncerShowing");
            this.bouncer = field5;
            Field field6 = SunshineModule.field(cls, "secQsUiDisplayModeInteractor");
            this.mode = field6;
            this.settings = SunshineModule.field(cls, "settingsInteractor");
            Method declaredMethod = field6.getType().getDeclaredMethod("isTablet", null);
            this.tablet = declaredMethod;
            declaredMethod.setAccessible(true);
            Field field7 = SunshineModule.field(cls2, "screenShotBitmapFactory");
            this.factoryField = field7;
            Field field8 = SunshineModule.field(field7.getType(), "wallpaperScreenShotProvider");
            this.factoryWp = field8;
            Field field9 = SunshineModule.field(field7.getType(), "windowManagerScreenShotProvider");
            this.factoryWm = field9;
            Class<?> cls4 = Class.forName(WP, false, classLoader);
            Class<?> cls5 = Class.forName(WM, false, classLoader);
            if (field8.getType() != cls4 || field9.getType() != cls5 || field4.getType() != cls2 || field.getType() != cls || field2.getType() != Integer.TYPE || !View.class.isAssignableFrom(field3.getType()) || field5.getType() != Boolean.TYPE || method.getReturnType() != Object.class || method2.getReturnType() != Void.TYPE) {
                throw new NoSuchFieldException("binder contract");
            }
            this.wpPrev = SunshineModule.field(cls4, "prevWallPaper");
            Field field10 = SunshineModule.field(cls4, "settingsInteractor");
            this.wpSettings = field10;
            this.wpContext = SunshineModule.field(cls4, "context");
            this.wmContext = SunshineModule.field(cls5, "context");
            Field field11 = SunshineModule.field(cls5, "displayLifecycle");
            this.wmLifecycle = field11;
            Constructor<?> declaredConstructor = cls4.getDeclaredConstructor(Context.class, field10.getType());
            this.wpConstructor = declaredConstructor;
            declaredConstructor.setAccessible(true);
            Constructor<?> declaredConstructor2 = cls5.getDeclaredConstructor(Context.class, field11.getType());
            this.wmConstructor = declaredConstructor2;
            declaredConstructor2.setAccessible(true);
            this.generatorContext = SunshineModule.field(cls2, "context");
            Field field12 = SunshineModule.field(cls2, "notificationShadeWindowStateInteractor");
            this.stateInteractor = field12;
            this.status = SunshineModule.field(field12.getType(), "statusBarState");
            Field field13 = SunshineModule.field(cls2, "secBlurCustomColorInteractor");
            this.colorInteractor = field13;
            this.custom = SunshineModule.field(field13.getType(), "hasCustomColorApplied");
            Class<?> cls6 = Class.forName("com.android.systemui.blur.QSColorCurve", false, classLoader);
            Constructor<?> declaredConstructor3 = cls6.getDeclaredConstructor(Context.class);
            this.curveConstructor = declaredConstructor3;
            declaredConstructor3.setAccessible(true);
            Method declaredMethod2 = cls6.getDeclaredMethod("setFraction", Float.TYPE);
            this.curveFraction = declaredMethod2;
            declaredMethod2.setAccessible(true);
            String[] strArr = {"radius", "saturation", "curve", "minX", "maxX", "minY", "maxY"};
            String[] strArr2 = {"setBlurRadius", "setProportionalSaturation", "setCurveLevel", "setCurveMinX", "setCurveMaxX", "setCurveMinY", "setCurveMaxY"};
            Class<?> type = SunshineModule.field(cls2, "blurFilter").getType();
            if (!type.getName().equals("com.samsung.android.graphics.SemGfxImageFilter")) {
                throw new NoSuchFieldException("filter type");
            }
            Constructor<?> declaredConstructor4 = type.getDeclaredConstructor(null);
            this.filterConstructor = declaredConstructor4;
            declaredConstructor4.setAccessible(true);
            this.curveFields = new Field[7];
            this.filterSetters = new Method[7];
            for (int i = 0; i < 7; i++) {
                this.curveFields[i] = SunshineModule.field(cls6, strArr[i]);
                this.filterSetters[i] = type.getMethod(strArr2[i], Float.TYPE);
                this.filterSetters[i].setAccessible(true);
                if (this.curveFields[i].getType() != Float.TYPE || this.filterSetters[i].getReturnType() != Void.TYPE) {
                    throw new NoSuchFieldException("curve/filter parameters");
                }
            }
            Method method3 = type.getMethod("applyToBitmap", Bitmap.class);
            this.applyFilter = method3;
            method3.setAccessible(true);
            this.minimal = SunshineModule.field(this.settings.getType(), "minimalBatteryUse");
            this.value = Class.forName("kotlinx.coroutines.flow.StateFlow", false, classLoader).getMethod("getValue", null);
            Method declaredMethod3 = Class.forName("android.app.ActivityManager", false, classLoader).getDeclaredMethod("semGetCurrentUser", null);
            this.user = declaredMethod3;
            declaredMethod3.setAccessible(true);
            Class<?> cls7 = Class.forName("com.android.systemui.wallpaper.WallpaperUtils", false, classLoader);
            this.wallpaperWhich = SunshineModule.field(cls7, "sCurrentWhich");
            this.wallpaperType = SunshineModule.field(cls7, "sWallpaperType");
            Method method4 = SunshineModule.method(classLoader, WP, "getScreenShot", new Class[0]);
            this.wpShot = method4;
            Method method5 = SunshineModule.method(classLoader, WM, "getScreenShot", new Class[0]);
            this.wmShot = method5;
            if (method4.getReturnType() != Bitmap.class || method5.getReturnType() != Bitmap.class || this.wpPrev.getType() != Bitmap.class || this.tablet.getReturnType() != Boolean.TYPE || method3.getReturnType() != Bitmap.class || this.curveFraction.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("capture contract");
            }
            return;
        }
        throw new NoSuchFieldException("QUICK_PANEL");
    }

    void install() {
        RuntimeHooks.hookMethod(this.fraction, new XC_MethodHook() { // from class: nihil.lain.sunshine.AsyncBlur.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Session session;
                if (!AsyncBlur.this.enabled || methodHookParam.hasThrowable() || Looper.myLooper() != Looper.getMainLooper() || (session = AsyncBlur.this.sessions.get(methodHookParam.thisObject)) == null) {
                    return;
                }
                if (methodHookParam.args[1] != AsyncBlur.this.quick || ((Float) methodHookParam.args[0]).floatValue() <= 0.0f) {
                    session.epoch++;
                    session.active = false;
                    session.job = null;
                } else {
                    if (session.active) {
                        return;
                    }
                    session.epoch++;
                    session.active = true;
                    session.emptyEpoch = -1L;
                }
            }
        });
        RuntimeHooks.hookMethod(this.collect, new XC_MethodHook() { // from class: nihil.lain.sunshine.AsyncBlur.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (AsyncBlur.this.enabled && !AsyncBlur.this.failed && Looper.myLooper() == Looper.getMainLooper() && RuntimeOptions.enabled(0)) {
                    try {
                        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
                        if (AsyncBlur.this.label.getInt(methodHookParam.thisObject) == 0 && AsyncBlur.this.collectorType.get(methodHookParam.thisObject) == AsyncBlur.this.quick) {
                            if (methodHookParam.args[0] == null || !methodHookParam.args[0].getClass().getName().equals("kotlin.Result$Failure")) {
                                Object obj = AsyncBlur.this.collectorOwner.get(methodHookParam.thisObject);
                                View view = (View) AsyncBlur.this.binderView.get(obj);
                                if (view != null && !AsyncBlur.this.bouncer.getBoolean(obj)) {
                                    if (view.getAlpha() > 0.0f && view.getBackground() == null && !((Boolean) AsyncBlur.this.tablet.invoke(AsyncBlur.this.mode.get(obj), null)).booleanValue()) {
                                        if (view.isAttachedToWindow() && view.getDisplay() != null && view.getDisplay().getState() == 2) {
                                            Session session = AsyncBlur.this.sessions.get(obj);
                                            if (session == null) {
                                                session = new Session();
                                                AsyncBlur.this.sessions.put(obj, session);
                                            }
                                            if (session.active && session.emptyEpoch != session.epoch) {
                                                if (session.job != null && session.job.epoch == session.epoch) {
                                                    AsyncBlur.this.coalesced++;
                                                    methodHookParam.setResult(AsyncBlur.this.unit);
                                                    return;
                                                }
                                                if (Boolean.TRUE.equals(AsyncBlur.this.value.invoke(AsyncBlur.this.minimal.get(AsyncBlur.this.settings.get(obj)), null))) {
                                                    return;
                                                }
                                                AsyncBlur.this.ensureReceiver(view.getContext());
                                                Job job = AsyncBlur.this.new Job(obj, view, session);
                                                session.job = job;
                                                Runnable runnablePoll = AsyncBlur.this.worker.getQueue().poll();
                                                if (runnablePoll instanceof Job) {
                                                    Job job2 = (Job) runnablePoll;
                                                    job2.cancelled = true;
                                                    if (job2.session.job == job2) {
                                                        job2.session.job = null;
                                                    }
                                                    AsyncBlur.this.discarded++;
                                                }
                                                try {
                                                    AsyncBlur.this.worker.execute(job);
                                                    AsyncBlur.this.scheduled++;
                                                    methodHookParam.setResult(AsyncBlur.this.unit);
                                                    job.launchMs = (SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos) / 1000000.0d;
                                                    AsyncBlur asyncBlur = AsyncBlur.this;
                                                    asyncBlur.maxLaunchMs = Math.max(asyncBlur.maxLaunchMs, job.launchMs);
                                                    return;
                                                } catch (Throwable th) {
                                                    if (session.job == job) {
                                                        session.job = null;
                                                    }
                                                    throw th;
                                                }
                                            }
                                            methodHookParam.setResult(AsyncBlur.this.unit);
                                            return;
                                        }
                                        return;
                                    }
                                    methodHookParam.setResult(AsyncBlur.this.unit);
                                }
                            }
                        }
                    } catch (Throwable th2) {
                        AsyncBlur.this.disable(th2);
                    }
                }
            }
        });
        this.enabled = true;
    }

    void ensureReceiver(Context context) {
        RuntimeOptions.register(context);
        if (this.receiverAdded) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.WALLPAPER_CHANGED");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        context.registerReceiver(new BroadcastReceiver() { // from class: nihil.lain.sunshine.AsyncBlur.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                AsyncBlur.this.environment.incrementAndGet();
                for (Session session : AsyncBlur.this.sessions.values()) {
                    session.epoch++;
                    session.job = null;
                    session.emptyEpoch = -1L;
                }
            }
        }, intentFilter, 2);
        this.receiverAdded = true;
    }

    final class Job implements Runnable {
        volatile boolean cancelled;
        final float[] coefficients;
        final Configuration config;
        final boolean customColor;
        final int displayId;
        final long env;
        final long epoch;
        Throwable error;
        final Object factory;
        final Object generator;
        final int height;
        double launchMs;
        final Object originalWm;
        final Object originalWp;
        final WeakReference<Object> owner;
        final Resources resources;
        BitmapDrawable result;
        final int rotation;
        final Session session;
        final Object source;
        double sourceMs;
        final int state;
        final Object stockPrev;
        double totalMs;
        final int type;
        final int userId;
        final WeakReference<View> view;
        final boolean wallpaper;
        final int which;
        final int width;

        Job(Object obj, View view, Session session) throws Exception {
            float[] fArr;
            this.owner = new WeakReference<>(obj);
            this.view = new WeakReference<>(view);
            this.session = session;
            this.epoch = session.epoch;
            this.env = AsyncBlur.this.environment.get();
            Object obj2 = AsyncBlur.this.binderGenerator.get(obj);
            this.generator = obj2;
            Object obj3 = AsyncBlur.this.factoryField.get(obj2);
            this.factory = obj3;
            Object obj4 = AsyncBlur.this.factoryWp.get(obj3);
            this.originalWp = obj4;
            Object obj5 = AsyncBlur.this.factoryWm.get(obj3);
            this.originalWm = obj5;
            int iIntValue = ((Number) AsyncBlur.this.value.invoke(AsyncBlur.this.status.get(AsyncBlur.this.stateInteractor.get(obj2)), null)).intValue();
            this.state = iIntValue;
            this.which = AsyncBlur.this.wallpaperWhich.getInt(null);
            int i = AsyncBlur.this.wallpaperType.getInt(null);
            this.type = i;
            this.userId = ((Integer) AsyncBlur.this.user.invoke(null, null)).intValue();
            int i2 = 0;
            boolean z = (iIntValue == 0 || i == 7) ? false : true;
            this.wallpaper = z;
            this.customColor = ((Boolean) AsyncBlur.this.value.invoke(AsyncBlur.this.custom.get(AsyncBlur.this.colorInteractor.get(obj2)), null)).booleanValue();
            this.config = new Configuration(view.getResources().getConfiguration());
            this.width = view.getWidth();
            this.height = view.getHeight();
            this.rotation = view.getDisplay().getRotation();
            this.displayId = view.getDisplay().getDisplayId();
            Object obj6 = AsyncBlur.this.wpPrev.get(obj4);
            this.stockPrev = obj6;
            if (z) {
                Object objNewInstance = AsyncBlur.this.wpConstructor.newInstance(AsyncBlur.this.wpContext.get(obj4), AsyncBlur.this.wpSettings.get(obj4));
                this.source = objNewInstance;
                AsyncBlur.this.wpPrev.set(objNewInstance, obj6);
            } else {
                this.source = AsyncBlur.this.wmConstructor.newInstance(AsyncBlur.this.wmContext.get(obj5), AsyncBlur.this.wmLifecycle.get(obj5));
            }
            Context context = (Context) AsyncBlur.this.generatorContext.get(obj2);
            this.resources = context.getResources();
            Object objNewInstance2 = AsyncBlur.this.curveConstructor.newInstance(context);
            AsyncBlur.this.curveFraction.invoke(objNewInstance2, Float.valueOf(1.0f));
            this.coefficients = new float[AsyncBlur.this.curveFields.length];
            while (true) {
                fArr = this.coefficients;
                if (i2 >= fArr.length) {
                    break;
                }
                fArr[i2] = AsyncBlur.this.curveFields[i2].getFloat(objNewInstance2);
                i2++;
            }
            if (!this.customColor) {
                fArr[1] = 0.0f;
                return;
            }
            fArr[5] = 0.0f;
            fArr[3] = 0.0f;
            fArr[2] = 0.0f;
            fArr[6] = 255.0f;
            fArr[4] = 255.0f;
        }

        boolean stale() {
            return this.cancelled || this.session.epoch != this.epoch || AsyncBlur.this.environment.get() != this.env || AsyncBlur.this.failed;
        }

        @Override // java.lang.Runnable
        public void run() {
            Bitmap bitmap;
            Bitmap bitmap2;
            Handler handler;
            Runnable runnable;
            Handler handler2;
            Runnable runnable2;
            long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
            try {
                if (stale()) {
                    BitmapDrawable bitmapDrawable = this.result;
                    Bitmap bitmap3 = bitmapDrawable == null ? null : bitmapDrawable.getBitmap();
                    if (this.wallpaper) {
                        try {
                            Object obj = AsyncBlur.this.wpPrev.get(this.source);
                            AsyncBlur.this.wpPrev.set(this.source, null);
                            if ((obj instanceof Bitmap) && obj != this.stockPrev && obj != null && obj != bitmap3) {
                                AsyncBlur.recycle((Bitmap) obj);
                            }
                        } catch (Throwable unused) {
                        }
                    }
                    if (bitmap3 != null && this.stockPrev != null) {
                        AsyncBlur.recycle(null);
                    }
                    this.totalMs = (SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos) / 1000000.0d;
                    handler2 = AsyncBlur.this.main;
                    runnable2 = new Runnable() { // from class: nihil.lain.sunshine.AsyncBlur$Job$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            this.f$0.m0lambda$run$0$nihillainsunshineAsyncBlur$Job();
                        }
                    };
                } else {
                    Bitmap bitmap4 = (Bitmap) XposedBridge.invokeOriginalMethod(this.wallpaper ? AsyncBlur.this.wpShot : AsyncBlur.this.wmShot, this.source, new Object[0]);
                    try {
                        this.sourceMs = (SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos) / 1000000.0d;
                        if (bitmap4 != null && !bitmap4.isRecycled() && !stale()) {
                            if (AsyncBlur.this.isolatedFilter == null) {
                                AsyncBlur asyncBlur = AsyncBlur.this;
                                asyncBlur.isolatedFilter = asyncBlur.filterConstructor.newInstance(null);
                            }
                            for (int i = 0; i < this.coefficients.length; i++) {
                                AsyncBlur.this.filterSetters[i].invoke(AsyncBlur.this.isolatedFilter, Float.valueOf(this.coefficients[i]));
                            }
                            Bitmap bitmap5 = (Bitmap) AsyncBlur.this.applyFilter.invoke(AsyncBlur.this.isolatedFilter, bitmap4);
                            if (bitmap5 != null) {
                                try {
                                    this.result = new BitmapDrawable(this.resources, bitmap5);
                                } catch (Throwable th) {
                                    bitmap = bitmap4;
                                    th = th;
                                    bitmap2 = bitmap5;
                                    try {
                                        this.error = th;
                                        BitmapDrawable bitmapDrawable2 = this.result;
                                        Bitmap bitmap6 = bitmapDrawable2 == null ? null : bitmapDrawable2.getBitmap();
                                        if (bitmap2 != null && bitmap2 != bitmap && bitmap2 != bitmap6) {
                                            AsyncBlur.recycle(bitmap2);
                                        }
                                        if (this.wallpaper) {
                                            try {
                                                Object obj2 = AsyncBlur.this.wpPrev.get(this.source);
                                                AsyncBlur.this.wpPrev.set(this.source, null);
                                                if ((obj2 instanceof Bitmap) && obj2 != this.stockPrev && obj2 != bitmap && obj2 != bitmap6) {
                                                    AsyncBlur.recycle((Bitmap) obj2);
                                                }
                                            } catch (Throwable unused2) {
                                            }
                                        }
                                        if (bitmap != bitmap6 && bitmap != this.stockPrev) {
                                            AsyncBlur.recycle(bitmap);
                                        }
                                        this.totalMs = (SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos) / 1000000.0d;
                                        handler = AsyncBlur.this.main;
                                        runnable = new Runnable() { // from class: nihil.lain.sunshine.AsyncBlur$Job$$ExternalSyntheticLambda0
                                            @Override // java.lang.Runnable
                                            public final void run() {
                                                this.f$0.m0lambda$run$0$nihillainsunshineAsyncBlur$Job();
                                            }
                                        };
                                    } catch (Throwable th2) {
                                        Bitmap bitmap7 = this.result == null ? null : this.result.getBitmap();
                                        if (bitmap2 != null && bitmap2 != bitmap && bitmap2 != bitmap7) {
                                            AsyncBlur.recycle(bitmap2);
                                        }
                                        if (this.wallpaper) {
                                            try {
                                                Object obj3 = AsyncBlur.this.wpPrev.get(this.source);
                                                AsyncBlur.this.wpPrev.set(this.source, null);
                                                if ((obj3 instanceof Bitmap) && obj3 != this.stockPrev && obj3 != bitmap && obj3 != bitmap7) {
                                                    AsyncBlur.recycle((Bitmap) obj3);
                                                }
                                            } catch (Throwable unused3) {
                                            }
                                        }
                                        if (bitmap != bitmap7 && bitmap != this.stockPrev) {
                                            AsyncBlur.recycle(bitmap);
                                        }
                                        this.totalMs = (SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos) / 1000000.0d;
                                        AsyncBlur.this.main.post(new Runnable() { // from class: nihil.lain.sunshine.AsyncBlur$Job$$ExternalSyntheticLambda0
                                            @Override // java.lang.Runnable
                                            public final void run() {
                                                this.f$0.m0lambda$run$0$nihillainsunshineAsyncBlur$Job();
                                            }
                                        });
                                        throw th2;
                                    }
                                }
                            }
                            BitmapDrawable bitmapDrawable3 = this.result;
                            Bitmap bitmap8 = bitmapDrawable3 == null ? null : bitmapDrawable3.getBitmap();
                            if (bitmap5 != null && bitmap5 != bitmap4 && bitmap5 != bitmap8) {
                                AsyncBlur.recycle(bitmap5);
                            }
                            if (this.wallpaper) {
                                try {
                                    Object obj4 = AsyncBlur.this.wpPrev.get(this.source);
                                    AsyncBlur.this.wpPrev.set(this.source, null);
                                    if ((obj4 instanceof Bitmap) && obj4 != this.stockPrev && obj4 != bitmap4 && obj4 != bitmap8) {
                                        AsyncBlur.recycle((Bitmap) obj4);
                                    }
                                } catch (Throwable unused4) {
                                }
                            }
                            if (bitmap4 != bitmap8 && bitmap4 != this.stockPrev) {
                                AsyncBlur.recycle(bitmap4);
                            }
                            this.totalMs = (SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos) / 1000000.0d;
                            handler = AsyncBlur.this.main;
                            runnable = new Runnable() { // from class: nihil.lain.sunshine.AsyncBlur$Job$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    this.f$0.m0lambda$run$0$nihillainsunshineAsyncBlur$Job();
                                }
                            };
                            handler.post(runnable);
                            return;
                        }
                        BitmapDrawable bitmapDrawable4 = this.result;
                        Bitmap bitmap9 = bitmapDrawable4 == null ? null : bitmapDrawable4.getBitmap();
                        if (this.wallpaper) {
                            try {
                                Object obj5 = AsyncBlur.this.wpPrev.get(this.source);
                                AsyncBlur.this.wpPrev.set(this.source, null);
                                if ((obj5 instanceof Bitmap) && obj5 != this.stockPrev && obj5 != bitmap4 && obj5 != bitmap9) {
                                    AsyncBlur.recycle((Bitmap) obj5);
                                }
                            } catch (Throwable unused5) {
                            }
                        }
                        if (bitmap4 != bitmap9 && bitmap4 != this.stockPrev) {
                            AsyncBlur.recycle(bitmap4);
                        }
                        this.totalMs = (SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos) / 1000000.0d;
                        handler2 = AsyncBlur.this.main;
                        runnable2 = new Runnable() { // from class: nihil.lain.sunshine.AsyncBlur$Job$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                this.f$0.m0lambda$run$0$nihillainsunshineAsyncBlur$Job();
                            }
                        };
                    } catch (Throwable th3) {
                        bitmap = bitmap4;
                        th = th3;
                        bitmap2 = null;
                    }
                }
                handler2.post(runnable2);
            } catch (Throwable th4) {
                th = th4;
                bitmap = null;
                bitmap2 = null;
            }
        }

        /* JADX INFO: renamed from: lambda$run$0$nihil-lain-sunshine-AsyncBlur$Job, reason: not valid java name */
        /* synthetic */ void m0lambda$run$0$nihillainsunshineAsyncBlur$Job() {
            AsyncBlur.this.finish(this);
        }

        boolean valid(Object obj, View view) throws Exception {
            Display display = view.getDisplay();
            return !stale() && this.session.active && RuntimeOptions.enabled(0) && view.isAttachedToWindow() && view.getAlpha() > 0.0f && view.getVisibility() == 0 && view.getBackground() == null && !AsyncBlur.this.bouncer.getBoolean(obj) && display != null && display.getState() == 2 && display.getDisplayId() == this.displayId && display.getRotation() == this.rotation && view.getWidth() == this.width && view.getHeight() == this.height && this.config.equals(view.getResources().getConfiguration()) && this.userId == ((Integer) AsyncBlur.this.user.invoke(null, null)).intValue() && this.which == AsyncBlur.this.wallpaperWhich.getInt(null) && this.type == AsyncBlur.this.wallpaperType.getInt(null) && this.state == ((Number) AsyncBlur.this.value.invoke(AsyncBlur.this.status.get(AsyncBlur.this.stateInteractor.get(this.generator)), null)).intValue() && this.customColor == ((Boolean) AsyncBlur.this.value.invoke(AsyncBlur.this.custom.get(AsyncBlur.this.colorInteractor.get(this.generator)), null)).booleanValue() && !Boolean.TRUE.equals(AsyncBlur.this.value.invoke(AsyncBlur.this.minimal.get(AsyncBlur.this.settings.get(obj)), null));
        }
    }

    /* JADX WARN: Code duplicated, block: B:37:0x00b6  */
    /* JADX WARN: Code duplicated, block: B:38:0x00b8  */
    void finish(Job job) {
        StringBuilder sb;
        boolean z;
        long jUptimeMillis;
        String str;
        if (job.session.job == job) {
            job.session.job = null;
        }
        Object obj = job.owner.get();
        View view = job.view.get();
        try {
            if (job.error == null) {
                if (job.result != null && obj != null && view != null && job.valid(obj, view)) {
                    view.setBackgroundDrawable(job.result);
                    this.applied++;
                    z = true;
                } else {
                    this.discarded++;
                    if (!job.stale() && job.result == null) {
                        job.session.emptyEpoch = job.epoch;
                    }
                }
                if (!z && job.result != null && (view == null || view.getBackground() != job.result)) {
                    recycle(job.result.getBitmap());
                }
                jUptimeMillis = SystemClock.uptimeMillis();
                if (this.applied > 3 || jUptimeMillis - this.lastLog >= 2000 || job.error != null) {
                    this.lastLog = jUptimeMillis;
                    StringBuilder sb2 = new StringBuilder("qs-async source=");
                    if (job.wallpaper) {
                        str = "wallpaper";
                    } else {
                        str = "window";
                    }
                    sb2.append(str);
                    sb2.append(" launchMainMs=");
                    sb2.append(job.launchMs);
                    sb2.append(" sourceMs=");
                    sb2.append(job.sourceMs);
                    sb2.append(" totalMs=");
                    sb2.append(job.totalMs);
                    sb2.append(" worker=true aplicado=");
                    sb2.append(z);
                    sb2.append(" agendados=");
                    sb2.append(this.scheduled);
                    sb2.append(" agrupados=");
                    sb2.append(this.coalesced);
                    sb2.append(" aplicados=");
                    sb2.append(this.applied);
                    sb2.append(" descartados=");
                    sb2.append(this.discarded);
                    SunshineModule.diagnostic(sb2.toString());
                }
                return;
            }
            this.errors++;
            disable(job.error);
            z = false;
            if (!z) {
                recycle(job.result.getBitmap());
            }
            jUptimeMillis = SystemClock.uptimeMillis();
            if (this.applied > 3) {
            }
            this.lastLog = jUptimeMillis;
            StringBuilder sb3 = new StringBuilder("qs-async source=");
            if (job.wallpaper) {
                str = "wallpaper";
            } else {
                str = "window";
            }
            sb3.append(str);
            sb3.append(" launchMainMs=");
            sb3.append(job.launchMs);
            sb3.append(" sourceMs=");
            sb3.append(job.sourceMs);
            sb3.append(" totalMs=");
            sb3.append(job.totalMs);
            sb3.append(" worker=true aplicado=");
            sb3.append(z);
            sb3.append(" agendados=");
            sb3.append(this.scheduled);
            sb3.append(" agrupados=");
            sb3.append(this.coalesced);
            sb3.append(" aplicados=");
            sb3.append(this.applied);
            sb3.append(" descartados=");
            sb3.append(this.discarded);
            SunshineModule.diagnostic(sb3.toString());
        } catch (Throwable th) {
            String str2 = "wallpaper";
            try {
                this.errors++;
                disable(th);
                if (job.result != null && (view == null || view.getBackground() != job.result)) {
                    recycle(job.result.getBitmap());
                }
                long jUptimeMillis2 = SystemClock.uptimeMillis();
                if (this.applied <= 3 || jUptimeMillis2 - this.lastLog >= 2000 || job.error != null) {
                    this.lastLog = jUptimeMillis2;
                    sb = new StringBuilder("qs-async source=");
                    str2 = job.wallpaper ? str2 : "window";
                }
            } finally {
                if (job.result != null && (view == null || view.getBackground() != job.result)) {
                    recycle(job.result.getBitmap());
                }
                long jUptimeMillis3 = SystemClock.uptimeMillis();
                if (this.applied <= 3 || jUptimeMillis3 - this.lastLog >= 2000 || job.error != null) {
                    this.lastLog = jUptimeMillis3;
                    sb = new StringBuilder("qs-async source=");
                    sb.append(job.wallpaper ? "wallpaper" : "window");
                    sb.append(" launchMainMs=");
                    sb.append(job.launchMs);
                    sb.append(" sourceMs=");
                    sb.append(job.sourceMs);
                    sb.append(" totalMs=");
                    sb.append(job.totalMs);
                    sb.append(" worker=true aplicado=false agendados=");
                    sb.append(this.scheduled);
                    sb.append(" agrupados=");
                    sb.append(this.coalesced);
                    sb.append(" aplicados=");
                    sb.append(this.applied);
                    sb.append(" descartados=");
                    sb.append(this.discarded);
                    SunshineModule.diagnostic(sb.toString());
                }
            }
        }
    }

    static void recycle(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        bitmap.recycle();
    }

    void disable(Throwable th) {
        if (this.failed) {
            return;
        }
        this.failed = true;
        for (Session session : this.sessions.values()) {
            session.epoch++;
            session.job = null;
        }
        while ((th instanceof InvocationTargetException) && th.getCause() != null) {
            th = th.getCause();
        }
        SunshineModule.diagnostic("qs-async desativado; captura original retomada: " + th.getClass().getSimpleName());
    }

    String report() {
        return "asyncAgendados=" + this.scheduled + " asyncAgrupados=" + this.coalesced + " asyncAplicados=" + this.applied + " asyncDescartados=" + this.discarded + " asyncErros=" + this.errors + " asyncMainMaxMs=" + this.maxLaunchMs + " asyncFalha=" + this.failed;
    }
}
