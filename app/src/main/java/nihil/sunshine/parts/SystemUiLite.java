package nihil.sunshine.parts;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
final class SystemUiLite {
    private static final String TAG = "SunshinePartsUI";
    private static volatile long coalesced;
    private static boolean coalescerInstalled;
    private static LiteGlass glass;
    private static volatile long mediaRenderers;
    private static Field mediaStepField;
    private static ClassLoader motionLoader;
    private static volatile boolean radiusApplied;
    private static volatile boolean waveApplied;
    private static final AtomicBoolean installed = new AtomicBoolean();
    private static final ClassLoader MODULE = SystemUiLite.class.getClassLoader();
    private static final WeakHashMap<Object, Integer> renderers = new WeakHashMap<>();
    private static volatile int cachedReduceTransparency = 0;
    private static volatile boolean reduceTransparencyObserverInstalled = false;

    SystemUiLite() {
    }

    static /* synthetic */ long access$208() {
        long j = mediaRenderers;
        mediaRenderers = 1 + j;
        return j;
    }

    static /* synthetic */ long access$408() {
        long j = coalesced;
        coalesced = 1 + j;
        return j;
    }

    static void configureMotion() {
        if (motionLoader == null) {
            return;
        }
        NoShadeMotion.configure(motionLoader);
        QsComposeMotion.configure(motionLoader);
    }

    static void workloadChanged() {
        applyMediaStep();
        configureMotion();
        ControlBus.syncLegacy();
        refreshBlur();
        PreparedBlur.policyChanged();
        MediaWork.configure();
        PanelMaterialWork.configure();
    }

    static void releaseRuntime() {
        SmartRam.configure();
        SoundAliveWork.configure();
        PreparedBlur.reload();
        MediaWork.configure();
        PanelMaterialWork.configure();
        configureMotion();
        ControlBus.syncLegacy();
        if (mediaStepField != null) {
            synchronized (renderers) {
                for (Map.Entry<Object, Integer> entry : renderers.entrySet()) {
                    try {
                        mediaStepField.setInt(entry.getKey(), entry.getValue().intValue());
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    static void refreshBlur() {
        if (glass != null) {
            glass.refreshMode();
        }
    }

    static void applyMediaStep() {
        if (mediaStepField == null) {
            return;
        }
        synchronized (renderers) {
            Iterator it = new ArrayList(renderers.keySet()).iterator();
            while (it.hasNext()) {
                mediaStepField.setInt(it.next(), AdaptiveWorkload.mediaStep());
            }
        }
    }

    static void install(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (installed.compareAndSet(false, true)) {
            if (!Config.supported((String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties", (ClassLoader) null), "get", new Object[]{"ro.build.display.id", ""}))) {
                Log.i(TAG, "SKIPPED: this SystemUI profile requires a supported ROM");
                return;
            }
            try {
                glass = new LiteGlass(loadPackageParam.classLoader);
                glass.install();
                ControlBus.feature("static_glass", "ready");
            } catch (Throwable th) {
                glass = null;
                ControlBus.feature("static_glass", "unavailable");
                Log.w(TAG, "Static glass unavailable; native blur fallback", th);
            }
            if (glass != null) {
                installCurve(loadPackageParam.classLoader);
            }
            motionLoader = loadPackageParam.classLoader;
            configureMotion();
            BlurDelivery.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            QsGestureWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            ControlBus.feature("tile_graphics_layers", "Native placement; inactive experimental hooks removed");
            ControlBus.feature("gpu_panel_blur", "Native ROM blur; experimental GPU drawable disabled");
            PreparedBlur.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            QsHotPaths.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            FrameVoteWork.install(loadPackageParam.classLoader);
            SurfaceDiagnosticWork.install(loadPackageParam.classLoader);
            GestureExclusionWork.install(loadPackageParam.classLoader);
            SoundAliveWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            EdgeExtensionWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            TransitionEffects.installShell(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            NavDispatchWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            BlurAnimatorState.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            ControlBus.feature("icon_dimensions", "Native resource path; speculative cache retired");
            ControlBus.feature("density_queries", "Native query path; per-frame wrapper retired");
            ControlBus.feature("opening_window_batch", "Native batching; extra input wrapper retired");
            PanelDiagnostics.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            StackTranslationWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            installTransparencyOptimization(loadPackageParam.classLoader);
            boolean zInstallMedia = installMedia(loadPackageParam.classLoader);
            MediaWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            PanelMaterialWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            CardTone.install();
            Class<?> cls = Class.forName("nihil.lain.sunshine.SunshineModule", false, MODULE);
            skip(cls.getDeclaredMethod("hookAsync", ClassLoader.class, String.class));
            skip(cls.getDeclaredMethod("hookMediaWave", ClassLoader.class));
            skip(cls.getDeclaredMethod("hookShade", ClassLoader.class));
            skip(cls.getDeclaredMethod("hookComposeLayout", ClassLoader.class, String.class));
            skip(cls.getDeclaredMethod("hookComposeHost", ClassLoader.class, String.class));
            RuntimeHooks.hookMethod(Class.forName("nihil.lain.sunshine.Policy", false, MODULE).getDeclaredMethod("allowed", String.class, String.class, String.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    boolean z = false;
                    if ("com.android.systemui".equals(methodHookParam.args[0]) && "com.android.systemui".equals(methodHookParam.args[1]) && Config.supported((String) methodHookParam.args[2])) {
                        z = true;
                    }
                    methodHookParam.setResult(Boolean.valueOf(z));
                }
            });
            SplitPanelWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
            installLeanQs();
            ((IXposedHookLoadPackage) cls.getDeclaredConstructor(new Class[0]).newInstance(new Object[0])).handleLoadPackage(loadPackageParam);
            try {
                Field field = field(Class.forName("nihil.lain.sunshine.RuntimeOptions", false, MODULE), "values");
                boolean[] zArr = (boolean[]) ((boolean[]) field.get(null)).clone();
                if (glass != null) {
                    zArr[0] = false;
                }
                zArr[5] = false;
                field.set(null, zArr);
            } catch (Throwable th2) {
                Log.w(TAG, "Optional legacy state labels unavailable", th2);
            }
            ControlBus.syncLegacy();
            installDump(loadPackageParam.classLoader, cls);
            try {
                Field declaredField = cls.getDeclaredField("hooks");
                declaredField.setAccessible(true);
                for (String str : String.valueOf(declaredField.get(null)).split(";")) {
                    String[] strArrSplit = str.trim().split("=", 2);
                    if (strArrSplit.length == 2) {
                        ControlBus.feature(strArrSplit[0], strArrSplit[1].equals("ok") ? "ready" : "native fallback");
                    }
                }
            } catch (Throwable th3) {
            }
            ControlBus.feature("qs-capture-timing", "disabled");
            ControlBus.systemReady = true;
            ControlBus.feature("legacy_layout_engine", "attached");
            Log.i(TAG, "READY: unified 2.31.1-r; SystemUI 1.7.0 layout/flex/tile fixes; staticGlass=" + (glass != null) + "; mediaStep=" + (zInstallMedia ? 4 : "native") + "; leanQs=" + coalescerInstalled + "; no screenshot worker when static glass is active");
        }
    }

    private static void skip(Method method) {
        method.setAccessible(true);
        RuntimeHooks.hookMethod(method, new XC_MethodHook(10000) { // from class: nihil.sunshine.parts.SystemUiLite.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                methodHookParam.setResult((Object) null);
            }
        });
    }

    private static void installCurve(ClassLoader classLoader) {
        try {
            Class<?> cls = Class.forName("com.android.systemui.blur.QSColorCurve", false, classLoader);
            final Field field = field(cls, "radius");
            Method declaredMethod = cls.getDeclaredMethod("setFraction", Float.TYPE);
            if (field.getType() != Float.TYPE || declaredMethod.getReturnType() != Void.TYPE) {
                throw new NoSuchFieldException("QS color curve");
            }
            RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.3
                private boolean failed;

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (!VisualPolicy.nativeBlur() && !this.failed && !methodHookParam.hasThrowable()) {
                        try {
                            field.setFloat(methodHookParam.thisObject, 0.0f);
                            if (!SystemUiLite.radiusApplied) {
                                boolean unused = SystemUiLite.radiusApplied = true;
                                Log.i(SystemUiLite.TAG, "APPLIED: QS curve blur radius 0");
                            }
                        } catch (Throwable th) {
                            this.failed = true;
                            Log.w(SystemUiLite.TAG, "QS radius hook disabled", th);
                        }
                    }
                }
            });
        } catch (Throwable th) {
            Log.w(TAG, "QS radius path unavailable", th);
        }
    }

    private static boolean installMedia(ClassLoader classLoader) {
        try {
            Class<?> cls = Class.forName("com.android.systemui.media.audiovisseekbar.renderer.track.auto.MultiWaveAreaTrackRenderer", false, classLoader);
            Constructor<?> declaredConstructor = cls.getDeclaredConstructor(View.class, Class.forName("com.android.systemui.media.audiovisseekbar.config.AudioVisSeekBarConfig", false, classLoader));
            final Field field = field(cls, "stepX");
            mediaStepField = field;
            if (field.getType() != Integer.TYPE) {
                throw new NoSuchFieldException("Media step");
            }
            RuntimeHooks.hookMethod(declaredConstructor, new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.4
                private boolean failed;

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (this.failed || methodHookParam.hasThrowable()) {
                        return;
                    }
                    try {
                        int i = field.getInt(methodHookParam.thisObject);
                        if (i == 2 || i == 10 || i == 24 || i == 4 || i == 8) {
                            synchronized (SystemUiLite.renderers) {
                                SystemUiLite.renderers.put(methodHookParam.thisObject, Integer.valueOf(i));
                            }
                            field.setInt(methodHookParam.thisObject, AdaptiveWorkload.mediaStep());
                            SystemUiLite.access$208();
                            if (!SystemUiLite.waveApplied) {
                                boolean unused = SystemUiLite.waveApplied = true;
                                Log.i(SystemUiLite.TAG, "APPLIED: native media motion retained; adaptive spatial sampling");
                            }
                        }
                    } catch (Throwable th) {
                        this.failed = true;
                        Log.w(SystemUiLite.TAG, "Media step unchanged", th);
                    }
                }
            });
            ControlBus.feature("media_wave", "ready");
            return true;
        } catch (Throwable th) {
            ControlBus.feature("media_wave", "native");
            Log.w(TAG, "Native media wave retained", th);
            return false;
        }
    }

    private static void installLeanQs() {
        try {
            Class<?> cls = Class.forName("nihil.lain.sunshine.QsWork", false, MODULE);
            Method declaredMethod = cls.getDeclaredMethod("install", new Class[0]);
            final Field field = field(cls, "run");
            final Field field2 = field(cls, "owner");
            final Field field3 = field(cls, "updateJob");
            final Field field4 = field(cls, "panel");
            final Field field5 = field(cls, "enabled");
            final Field field6 = field(cls, "skipped");
            RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook(10000) { // from class: nihil.sunshine.parts.SystemUiLite.5
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Throwable th;
                    XC_MethodHook.Unhook unhook = null;
                    try {
                        final Object obj = methodHookParam.thisObject;
                        final Field field7 = (Field) field2.get(obj);
                        final Field field8 = (Field) field3.get(obj);
                        final Field field9 = (Field) field4.get(obj);
                        XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod((Method) field.get(obj), new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.5.1
                            private boolean failed;

                            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam2) {
                                if (!VisualPolicy.leanQs() || this.failed || Looper.myLooper() != Looper.getMainLooper()) {
                                    return;
                                }
                                try {
                                    Object obj2 = field7.get(methodHookParam2.thisObject);
                                    if (obj2 != null && field8.get(obj2) == methodHookParam2.thisObject) {
                                        View view = (View) field9.get(obj2);
                                        Handler handler = view == null ? null : view.getHandler();
                                        if (handler != null && handler.hasCallbacks((Runnable) methodHookParam2.thisObject)) {
                                            field6.setLong(obj, field6.getLong(obj) + 1);
                                            SystemUiLite.access$408();
                                            methodHookParam2.setResult((Object) null);
                                        }
                                    }
                                } catch (Throwable th2) {
                                    this.failed = true;
                                    Log.w(SystemUiLite.TAG, "QS coalescing disabled", th2);
                                }
                            }
                        });
                        try {
                            field5.setBoolean(obj, true);
                            boolean unused = SystemUiLite.coalescerInstalled = true;
                            methodHookParam.setResult((Object) null);
                        } catch (Throwable th2) {
                            th = th2;
                            unhook = unhookHookMethod;
                            if (unhook != null) {
                                RuntimeHooks.unhook(unhook);
                            }
                            Log.w(SystemUiLite.TAG, "QS falls back to 1.7.0 implementation", th);
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
            });
            skip(cls.getDeclaredMethod("installCaptureTimer", ClassLoader.class));
        } catch (Throwable th) {
            Log.w(TAG, "Lean QS wrapper unavailable", th);
        }
    }

    private static void installDump(ClassLoader classLoader, Class<?> cls) {
        try {
            Method declaredMethod = Class.forName("com.android.systemui.shade.NotificationShadeWindowControllerImpl", false, classLoader).getDeclaredMethod("dump", PrintWriter.class, String[].class);
            final Method declaredMethod2 = cls.getDeclaredMethod("lambda$handleLoadPackage$1", new Class[0]);
            declaredMethod2.setAccessible(true);
            RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.6
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    try {
                        PrintWriter printWriter = (PrintWriter) methodHookParam.args[0];
                        printWriter.println("SunshineParts 2.31.1-r: staticGlass=" + (SystemUiLite.glass != null) + " radiusApplied=" + SystemUiLite.radiusApplied + " mediaRenderers=" + SystemUiLite.mediaRenderers + " leanQs=" + SystemUiLite.coalescerInstalled + " coalesced=" + SystemUiLite.coalesced);
                        if (SystemUiLite.glass != null) {
                            printWriter.println("SunshineParts: " + SystemUiLite.glass.report());
                        }
                        printWriter.println("SunshineParts retained 1.7.0: " + declaredMethod2.invoke(null, new Object[0]));
                    } catch (Throwable th) {
                        Log.w(SystemUiLite.TAG, "Diagnostic report unavailable", th);
                    }
                }
            });
        } catch (Throwable th) {
            Log.w(TAG, "Diagnostic dump hook unavailable", th);
        }
    }

    private static void installTransparencyOptimization(ClassLoader classLoader) {
        try {
            RuntimeHooks.hookMethod(Settings.System.class.getDeclaredMethod("getInt", ContentResolver.class, String.class, Integer.TYPE), new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.7
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if ("accessibility_reduce_transparency".equals(methodHookParam.args[1])) {
                        SystemUiLite.ensureTransparencyObserver((ContentResolver) methodHookParam.args[0], ((Integer) methodHookParam.args[2]).intValue());
                        methodHookParam.setResult(Integer.valueOf(SystemUiLite.cachedReduceTransparency));
                    }
                }
            });
            try {
                RuntimeHooks.hookMethod(Settings.System.class.getDeclaredMethod("getInt", ContentResolver.class, String.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.8
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if ("accessibility_reduce_transparency".equals(methodHookParam.args[1])) {
                            SystemUiLite.ensureTransparencyObserver((ContentResolver) methodHookParam.args[0], 0);
                            methodHookParam.setResult(Integer.valueOf(SystemUiLite.cachedReduceTransparency));
                        }
                    }
                });
            } catch (Throwable th) {
            }
            try {
                RuntimeHooks.hookMethod(Settings.System.class.getDeclaredMethod("getString", ContentResolver.class, String.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.9
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if ("accessibility_reduce_transparency".equals(methodHookParam.args[1])) {
                            SystemUiLite.ensureTransparencyObserver((ContentResolver) methodHookParam.args[0], 0);
                            methodHookParam.setResult(String.valueOf(SystemUiLite.cachedReduceTransparency));
                        }
                    }
                });
            } catch (Throwable th2) {
            }
            try {
                RuntimeHooks.hookMethod(Settings.System.class.getDeclaredMethod("getUriFor", String.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.SystemUiLite.10
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if ("accessibility_reduce_transparency".equals(methodHookParam.args[0])) {
                            methodHookParam.setResult(Settings.Global.getUriFor("accessibility_reduce_transparency"));
                        }
                    }
                });
            } catch (Throwable th3) {
            }
            ControlBus.feature("transparency_hotpath", "ready");
        } catch (Throwable th4) {
            ControlBus.feature("transparency_hotpath", "native fallback");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void ensureTransparencyObserver(final ContentResolver contentResolver, int i) {
        if (!reduceTransparencyObserverInstalled && contentResolver != null) {
            reduceTransparencyObserverInstalled = true;
            try {
                cachedReduceTransparency = Settings.Global.getInt(contentResolver, "accessibility_reduce_transparency", i);
                contentResolver.registerContentObserver(Settings.Global.getUriFor("accessibility_reduce_transparency"), false, new ContentObserver(new Handler(Looper.getMainLooper())) { // from class: nihil.sunshine.parts.SystemUiLite.11
                    @Override // android.database.ContentObserver
                    public void onChange(boolean z) {
                        try {
                            int unused = SystemUiLite.cachedReduceTransparency = Settings.Global.getInt(contentResolver, "accessibility_reduce_transparency", 0);
                        } catch (Throwable th) {
                        }
                    }
                });
            } catch (Throwable th) {
            }
        }
    }

    private static Field field(Class<?> cls, String str) throws NoSuchFieldException {
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
}
