package nihil.sunshine.parts.launcher;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import nihil.sunshine.parts.Config;
import nihil.sunshine.parts.ControlBus;
import nihil.sunshine.parts.RuntimeHooks;
import nihil.sunshine.parts.TransitionEffects;

/* JADX INFO: loaded from: classes.dex */
public final class LauncherHook implements IXposedHookLoadPackage {
    private static final String PACKAGE = "com.sec.android.app.launcher";
    private static final String ROOT = "com.honeyspace.transition.data.";
    public static long diagnosticsSkipped;
    private static final AtomicBoolean installed = new AtomicBoolean();
    private static final Map<Object, Object[][]> owners = Collections.synchronizedMap(new WeakHashMap());
    public static long presetReads;

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!PACKAGE.equals(loadPackageParam.packageName) || !PACKAGE.equals(loadPackageParam.processName)) {
            return;
        }
        XposedHelpers.findAndHookMethod(Application.class, "attach", new Object[]{Context.class, new XC_MethodHook() { // from class: nihil.sunshine.parts.launcher.LauncherHook.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (methodHookParam.hasThrowable() || !LauncherHook.installed.compareAndSet(false, true)) {
                    return;
                }
                try {
                    PackageInfo packageInfo = ((Context) methodHookParam.args[0]).getPackageManager().getPackageInfo(LauncherHook.PACKAGE, 0);
                    if (packageInfo.getLongVersionCode() != 1700705013 || !"17.0.07.13".equals(packageInfo.versionName)) {
                        throw new IllegalStateException("Unsupported launcher version");
                    }
                    TransitionEffects.installLauncher(loadPackageParam.classLoader, packageInfo.applicationInfo == null ? null : packageInfo.applicationInfo.sourceDir);
                    LauncherHook.install(loadPackageParam.classLoader);
                } catch (Throwable th) {
                    ControlBus.feature("launcher_motion", "Native fallback: " + th.getClass().getSimpleName());
                }
            }
        }});
    }

    public static int presetIndex(String str) {
        if ("lowest".equals(str)) {
            return 1;
        }
        return ("high".equals(str) || "mass".equals(str)) ? 2 : 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void install(ClassLoader classLoader) throws Exception {
        Class<?> cls = Class.forName("com.honeyspace.transition.data.AppTransitionParams", false, classLoader);
        int i = 3;
        final Constructor[][] constructorArr = (Constructor[][]) Array.newInstance((Class<?>) Constructor.class, 2, 3);
        String[] strArr = {"LowEnd", "LowestEnd", "HighEnd"};
        Method[] methodArr = new Method[2];
        int i2 = 0;
        while (i2 < 2) {
            String str = i2 == 0 ? "open" : "close";
            String str2 = i2 == 0 ? "OpenParams" : "CloseParams";
            methodArr[i2] = cls.getDeclaredMethod(i2 == 0 ? "getOpenParams" : "getCloseParams", new Class[0]);
            int i3 = 0;
            while (i3 < i) {
                Class<?> cls2 = Class.forName(ROOT + str + "." + strArr[i3] + str2, false, classLoader);
                if (!methodArr[i2].getReturnType().isAssignableFrom(cls2)) {
                    throw new NoSuchMethodException("Native preset return type");
                }
                for (Field field : cls2.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                        throw new NoSuchFieldException("Mutable preset");
                    }
                }
                constructorArr[i2][i3] = cls2.getDeclaredConstructor(new Class[0]);
                constructorArr[i2][i3].setAccessible(true);
                i3++;
                i = 3;
            }
            i2++;
            i = 3;
        }
        ArrayList arrayList = new ArrayList();
        for (final int i4 = 0; i4 < 2; i4++) {
            try {
                XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(methodArr[i4], new XC_MethodHook() { // from class: nihil.sunshine.parts.launcher.LauncherHook.2
                    boolean failed;

                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object objNewInstance;
                        if (!this.failed && methodHookParam.thisObject != null) {
                            try {
                                int iPresetIndex = LauncherHook.presetIndex(Config.launcherProfile());
                                synchronized (LauncherHook.owners) {
                                    Object[][] objArr = (Object[][]) LauncherHook.owners.get(methodHookParam.thisObject);
                                    if (objArr == null) {
                                        objArr = (Object[][]) Array.newInstance((Class<?>) Object.class, 2, 3);
                                        LauncherHook.owners.put(methodHookParam.thisObject, objArr);
                                    }
                                    objNewInstance = objArr[i4][iPresetIndex];
                                    if (objNewInstance == null) {
                                        Object[] objArr2 = objArr[i4];
                                        objNewInstance = constructorArr[i4][iPresetIndex].newInstance(new Object[0]);
                                        objArr2[iPresetIndex] = objNewInstance;
                                    }
                                }
                                methodHookParam.setResult(objNewInstance);
                                LauncherHook.presetReads++;
                            } catch (Throwable th) {
                                this.failed = true;
                                ControlBus.feature("launcher_motion", "Native fallback: " + th.getClass().getSimpleName());
                            }
                        }
                    }
                });
                if (unhookHookMethod != null) {
                    arrayList.add(unhookHookMethod);
                } else {
                    throw new IllegalStateException("Preset hook rejected");
                }
            } catch (Throwable th) {
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    RuntimeHooks.unhook((XC_MethodHook.Unhook) it.next());
                }
                throw new IllegalStateException(th);
            }
        }
        ControlBus.feature("launcher_motion", "Native LowEnd/LowestEnd/HighEnd presets; Mass follows native HighEnd branch");
        installDiagnostics(classLoader);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean transitionTag(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof String)) {
            return obj.getClass().getName().startsWith("com.honeyspace.transition.");
        }
        String str = (String) obj;
        return str.equals("RectFAnimation") || str.equals("Player") || str.equals("AnimationSession") || str.equals("OtherActivityInputConsumer");
    }

    private static void installDiagnostics(ClassLoader classLoader) {
        try {
            Class<?> cls = Class.forName("com.honeyspace.common.log.LogTagBuildersKt", false, classLoader);
            Class<?> cls2 = Class.forName("com.honeyspace.common.log.LogTag", false, classLoader);
            Method[] methodArr = new Method[4];
            String[] strArr = {"info", "debug"};
            int i = 0;
            for (int i2 = 0; i2 < 2; i2++) {
                String str = strArr[i2];
                Class[] clsArr = {cls2, String.class};
                int i3 = 0;
                while (i3 < 2) {
                    Method declaredMethod = cls.getDeclaredMethod(str, clsArr[i3], String.class);
                    if (Modifier.isStatic(declaredMethod.getModifiers()) && declaredMethod.getReturnType() == Void.TYPE) {
                        methodArr[i] = declaredMethod;
                        i3++;
                        i++;
                    } else {
                        throw new NoSuchMethodException("Diagnostic wrapper");
                    }
                }
            }
            for (int i4 = 0; i4 < 4; i4++) {
                RuntimeHooks.hookMethod(methodArr[i4], new XC_MethodHook() { // from class: nihil.sunshine.parts.launcher.LauncherHook.3
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (methodHookParam.args[1] == null || !LauncherHook.transitionTag(methodHookParam.args[0])) {
                            return;
                        }
                        LauncherHook.diagnosticsSkipped++;
                        methodHookParam.setResult((Object) null);
                    }
                });
            }
            ControlBus.feature("launcher_diagnostic_work", "Transition info/debug wrappers bypassed; warnings/errors native");
        } catch (Throwable th) {
            ControlBus.feature("launcher_diagnostic_work", "Native fallback");
        }
    }
}
