package nihil.lain.sunshine;

import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
public final class SunshineModule implements IXposedHookLoadPackage {
    static final String TAG = "SunshineUI";
    static AsyncBlur asyncBlur;
    static ComposeHost composeHost;
    static ComposeLayout composeLayout;
    static FlexGuard flexGuard;
    private static boolean installed;
    static MediaWave mediaWave;
    static QsWork qsWork;
    static ShadeRate rate;
    static TileWork tileWork;
    static final AtomicLong warningCount = new AtomicLong();
    static final AtomicLong alphaLogCount = new AtomicLong();
    static volatile String hooks = "";
    static final AtomicLong qsLogCount = new AtomicLong();

    public synchronized void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!installed && "com.android.systemui".equals(loadPackageParam.packageName) && "com.android.systemui".equals(loadPackageParam.processName)) {
            try {
                String str = null;
                String str2 = (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties", (ClassLoader) null), "get", new Object[]{"ro.build.display.id", ""});
                if (Policy.allowed(loadPackageParam.packageName, loadPackageParam.processName, str2) && Build.VERSION.SDK_INT == 36) {
                    installed = true;
                    Log.w(TAG, "v1.7.0 autorizado por ro.build.display.id=" + str2);
                    hookWarnings(loadPackageParam.classLoader);
                    hookAlphaLog(loadPackageParam.classLoader);
                    hookMediaWave(loadPackageParam.classLoader);
                    hookFlexGuard(loadPackageParam.classLoader, loadPackageParam.appInfo == null ? null : loadPackageParam.appInfo.sourceDir);
                    hookQsWork(loadPackageParam.classLoader, loadPackageParam.appInfo == null ? null : loadPackageParam.appInfo.sourceDir);
                    hookAsync(loadPackageParam.classLoader, loadPackageParam.appInfo == null ? null : loadPackageParam.appInfo.sourceDir);
                    hookTiles(loadPackageParam.classLoader, loadPackageParam.appInfo == null ? null : loadPackageParam.appInfo.sourceDir);
                    hookComposeLayout(loadPackageParam.classLoader, loadPackageParam.appInfo == null ? null : loadPackageParam.appInfo.sourceDir);
                    ClassLoader classLoader = loadPackageParam.classLoader;
                    if (loadPackageParam.appInfo != null) {
                        str = loadPackageParam.appInfo.sourceDir;
                    }
                    hookComposeHost(classLoader, str);
                    hookShade(loadPackageParam.classLoader);
                    RuntimeOptions.changed = new Runnable() { // from class: nihil.lain.sunshine.SunshineModule$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            SunshineModule.lambda$handleLoadPackage$0();
                        }
                    };
                    RuntimeOptions.reporter = new RuntimeOptions.Reporter() { // from class: nihil.lain.sunshine.SunshineModule$$ExternalSyntheticLambda1
                        @Override // nihil.lain.sunshine.RuntimeOptions.Reporter
                        public final String get() {
                            return SunshineModule.lambda$handleLoadPackage$1();
                        }
                    };
                    Log.w(TAG, "hooks: " + hooks + "; blur-agenda=removida");
                }
            } catch (Throwable unused) {
            }
        }
    }

    static /* synthetic */ void lambda$handleLoadPackage$0() {
        if (rate == null || RuntimeOptions.enabled(5)) {
            return;
        }
        rate.suspend();
    }

    static /* synthetic */ String lambda$handleLoadPackage$1() {
        StringBuilder sb = new StringBuilder("modulo=1.7.0; hooks=[");
        sb.append(hooks);
        sb.append("]; ");
        ComposeLayout composeLayout2 = composeLayout;
        sb.append(composeLayout2 == null ? "qs-compose-layout=ignorado" : composeLayout2.report());
        sb.append("; ");
        TileWork tileWork2 = tileWork;
        sb.append(tileWork2 == null ? "tile-work=ignorado" : tileWork2.report());
        sb.append("; ");
        ComposeHost composeHost2 = composeHost;
        sb.append(composeHost2 == null ? "qs-compose-host=ignorado" : composeHost2.report());
        return sb.toString();
    }

    static void diagnostic(String str) {
        Log.w(TAG, str);
    }

    static void log(String str) {
        Log.i(TAG, str);
    }

    static void status(String str, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append(hooks);
        sb.append(hooks.isEmpty() ? "" : "; ");
        sb.append(str);
        sb.append("=");
        sb.append(z ? "ok" : "ignorado");
        hooks = sb.toString();
    }

    static Method method(ClassLoader classLoader, String str, String str2, Class<?>... clsArr) throws Exception {
        Method declaredMethod = Class.forName(str, false, classLoader).getDeclaredMethod(str2, clsArr);
        declaredMethod.setAccessible(true);
        return declaredMethod;
    }

    static Field field(Class<?> cls, String str) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private static void hookWarnings(ClassLoader classLoader) {
        try {
            Method method = method(classLoader, "com.android.systemui.flags.RefactorFlagUtils", "assertOnEngBuild", String.class);
            if (method.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("return");
            }
            RuntimeHooks.hookMethod(method, new XC_MethodHook() { // from class: nihil.lain.sunshine.SunshineModule.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (Policy.WARNING.equals(methodHookParam.args[0])) {
                        SunshineModule.warningCount.incrementAndGet();
                        methodHookParam.setResult((Object) null);
                    }
                }
            });
            status("scene-warning", true);
        } catch (Throwable unused) {
            status("scene-warning", false);
        }
    }

    private static void hookAlphaLog(ClassLoader classLoader) {
        try {
            Method method = method(classLoader, "com.android.systemui.blur.SecQpBlurController$1$1$2$$ExternalSyntheticOutline0", "m", Float.TYPE, String.class, String.class);
            if (method.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("return");
            }
            RuntimeHooks.hookMethod(method, new XC_MethodHook() { // from class: nihil.lain.sunshine.SunshineModule.2
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if ("setAlpha = ".equals(methodHookParam.args[1]) && "SecPanelBackgroundBinder".equals(methodHookParam.args[2])) {
                        SunshineModule.alphaLogCount.incrementAndGet();
                        methodHookParam.setResult((Object) null);
                    } else if (Policy.qsFrameLog((String) methodHookParam.args[1], (String) methodHookParam.args[2])) {
                        SunshineModule.qsLogCount.incrementAndGet();
                        methodHookParam.setResult((Object) null);
                    }
                }
            });
            status("blur-frame-log", true);
        } catch (Throwable unused) {
            status("blur-frame-log", false);
        }
    }

    private static void hookMediaWave(ClassLoader classLoader) {
        try {
            MediaWave mediaWave2 = new MediaWave(classLoader);
            mediaWave = mediaWave2;
            mediaWave2.install();
            status("music-wave", true);
        } catch (Throwable unused) {
            mediaWave = null;
            status("music-wave", false);
        }
    }

    private static void hookFlexGuard(ClassLoader classLoader, String str) {
        try {
            FlexGuard flexGuard2 = new FlexGuard(classLoader, ApkContract.matches(str));
            flexGuard = flexGuard2;
            flexGuard2.install();
            status("flex-guard", true);
        } catch (Throwable unused) {
            flexGuard = null;
            status("flex-guard", false);
        }
    }

    private static void hookQsWork(ClassLoader classLoader, String str) {
        try {
            QsWork qsWork2 = new QsWork(classLoader, ApkContract.matchesQs(str));
            qsWork = qsWork2;
            qsWork2.install();
            status("qs-job", true);
            try {
                qsWork.installCaptureTimer(classLoader);
                status("qs-capture-timing", true);
            } catch (Throwable unused) {
                status("qs-capture-timing", false);
            }
        } catch (Throwable unused2) {
            qsWork = null;
            status("qs-job", false);
        }
    }

    private static void hookShade(ClassLoader classLoader) {
        try {
            Class<?> cls = Class.forName("com.android.systemui.shade.NotificationShadeWindowControllerImpl", false, classLoader);
            rate = new ShadeRate(cls);
            Method method = method(classLoader, cls.getName(), "applyWindowLayoutParams", new Class[0]);
            if (method.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("return");
            }
            RuntimeHooks.hookMethod(method, new XC_MethodHook() { // from class: nihil.lain.sunshine.SunshineModule.3
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    View view;
                    try {
                        if (!RuntimeOptions.registered && (view = (View) SunshineModule.rate.rootField.get(methodHookParam.thisObject)) != null) {
                            RuntimeOptions.register(view.getContext());
                        }
                        methodHookParam.setObjectExtra("sunshine.rates", SunshineModule.rate.beforeApply(methodHookParam.thisObject));
                    } catch (Throwable th) {
                        SunshineModule.rate.fail(th);
                    }
                }

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    try {
                        SunshineModule.rate.afterApply(methodHookParam.getObjectExtra("sunshine.rates"));
                    } catch (Throwable th) {
                        SunshineModule.rate.fail(th);
                    }
                }
            });
            status("shade-refresh", true);
            try {
                RuntimeHooks.hookMethod(method(classLoader, "com.android.systemui.shade.NotificationShadeWindowView", "dispatchTouchEvent", MotionEvent.class), new XC_MethodHook() { // from class: nihil.lain.sunshine.SunshineModule.4
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        try {
                            SunshineModule.rate.touch((View) methodHookParam.thisObject, (MotionEvent) methodHookParam.args[0]);
                        } catch (Throwable th) {
                            SunshineModule.rate.fail(th);
                        }
                    }
                });
                status("shade-touch", true);
            } catch (Throwable unused) {
                status("shade-touch", false);
            }
            try {
                RuntimeHooks.hookMethod(method(classLoader, "androidx.compose.ui.platform.Api35Impl", "setRequestedFrameRate", View.class, Float.TYPE), new XC_MethodHook() { // from class: nihil.lain.sunshine.SunshineModule.5
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        try {
                            methodHookParam.args[1] = Float.valueOf(SunshineModule.rate.composeRate((View) methodHookParam.args[0], ((Float) methodHookParam.args[1]).floatValue()));
                        } catch (Throwable th) {
                            SunshineModule.rate.fail(th);
                        }
                    }
                });
                status("shade-compose", true);
            } catch (Throwable unused2) {
                status("shade-compose", false);
            }
            try {
                RuntimeHooks.hookMethod(method(classLoader, cls.getName(), "dump", PrintWriter.class, String[].class), new XC_MethodHook() { // from class: nihil.lain.sunshine.SunshineModule.6
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        PrintWriter printWriter = (PrintWriter) methodHookParam.args[0];
                        StringBuilder sb = new StringBuilder("SunshineUI: ");
                        sb.append(SunshineModule.rate.report());
                        sb.append("; ");
                        sb.append(SunshineModule.mediaWave == null ? "music-wave=ignorado" : SunshineModule.mediaWave.report());
                        sb.append("; blur-agenda=removida; ");
                        sb.append(SunshineModule.flexGuard == null ? "flex-guard=ignorado" : SunshineModule.flexGuard.report());
                        sb.append("; logsQsEvitados=");
                        sb.append(SunshineModule.qsLogCount.get());
                        sb.append("; ");
                        sb.append(SunshineModule.qsWork == null ? "qs-job=ignorado" : SunshineModule.qsWork.report());
                        sb.append("; ");
                        sb.append(SunshineModule.asyncBlur == null ? "qs-async=ignorado" : SunshineModule.asyncBlur.report());
                        sb.append("; ");
                        sb.append(SunshineModule.tileWork == null ? "tile-work=ignorado" : SunshineModule.tileWork.report());
                        sb.append("; ");
                        sb.append(RuntimeOptions.report());
                        printWriter.println(sb.toString());
                    }
                });
                status("diagnostico", true);
            } catch (Throwable unused3) {
                status("diagnostico", false);
            }
        } catch (Throwable unused4) {
            rate = null;
            status("shade-refresh", false);
        }
    }

    private static void hookAsync(ClassLoader classLoader, String str) {
        try {
            AsyncBlur asyncBlur2 = new AsyncBlur(classLoader, ApkContract.matchesBlur(str));
            asyncBlur = asyncBlur2;
            asyncBlur2.install();
            status("qs-async", true);
        } catch (Throwable th) {
            asyncBlur = null;
            status("qs-async", false);
            diagnostic("qs-async contrato indisponivel: " + th.getClass().getSimpleName());
        }
    }

    private static void hookComposeLayout(ClassLoader classLoader, String str) {
        try {
            ComposeLayout composeLayout2 = new ComposeLayout(classLoader, ApkContract.matchesQs(str) && ApkContract.matches(str));
            composeLayout = composeLayout2;
            composeLayout2.install();
            status("qs-compose-layout", true);
        } catch (Throwable th) {
            composeLayout = null;
            status("qs-compose-layout", false);
            diagnostic("qs-compose-layout contrato indisponivel: " + th.getClass().getSimpleName());
        }
    }

    private static void hookTiles(ClassLoader classLoader, String str) {
        try {
            TileWork tileWork2 = new TileWork(classLoader, ApkContract.matchesQs(str));
            tileWork = tileWork2;
            tileWork2.install();
            status("tile-work", true);
        } catch (Throwable th) {
            tileWork = null;
            status("tile-work", false);
            diagnostic("tile-work contrato indisponivel: " + th.getClass().getSimpleName());
        }
    }

    private static void hookComposeHost(ClassLoader classLoader, String str) {
        try {
            ComposeHost composeHost2 = new ComposeHost(classLoader, ApkContract.matchesQs(str));
            composeHost = composeHost2;
            composeHost2.install();
            status("qs-compose-host", true);
        } catch (Throwable th) {
            composeHost = null;
            status("qs-compose-host", false);
            diagnostic("qs-compose-host contrato indisponivel: " + th.getClass().getSimpleName());
        }
    }
}
