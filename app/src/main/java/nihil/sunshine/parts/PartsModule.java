package nihil.sunshine.parts;

import android.os.Build;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import nihil.sunshine.parts.launcher.LauncherHook;

/* JADX INFO: loaded from: classes.dex */
public final class PartsModule implements IXposedHookLoadPackage {
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!Config.target(Config.property("ro.build.display.id"), Config.property("ro.build.version.oneui"), Build.VERSION.SDK_INT)) {
            return;
        }
        Config.load();
        RuntimeProfile.capture(Config.recoveryMode);
        if ("com.android.systemui".equals(loadPackageParam.packageName) && "com.android.systemui:screenshot".equals(loadPackageParam.processName)) {
            if (!RuntimeProfile.recovery()) {
                ScreenshotWork.install(loadPackageParam.classLoader, IconDimensions.sourceApk(loadPackageParam));
                return;
            }
            return;
        }
        if ((loadPackageParam.packageName.equals(loadPackageParam.processName) || ("android".equals(loadPackageParam.packageName) && "system_server".equals(loadPackageParam.processName))) && ("android".equals(loadPackageParam.packageName) || "com.android.systemui".equals(loadPackageParam.packageName) || "com.sec.android.app.launcher".equals(loadPackageParam.packageName))) {
            ControlBus.prepare(loadPackageParam.packageName, loadPackageParam.classLoader);
            if (RuntimeProfile.recovery()) {
                ControlBus.systemReady = true;
                ControlBus.feature("runtime_profile", "Recovery: native UI, display policy and launcher; rendering hooks not installed");
                return;
            }
        }
        if (RuntimeProfile.recovery()) {
            return;
        }
        try {
            if ("android".equals(loadPackageParam.packageName) && ("android".equals(loadPackageParam.processName) || "system_server".equals(loadPackageParam.processName))) {
                new RefreshHook().handleLoadPackage(loadPackageParam);
                NotificationHistoryGuard.install(loadPackageParam.classLoader);
            } else if ("com.sec.android.app.launcher".equals(loadPackageParam.packageName)) {
                new LauncherHook().handleLoadPackage(loadPackageParam);
            } else {
                if (!"com.android.systemui".equals(loadPackageParam.packageName) || !"com.android.systemui".equals(loadPackageParam.processName)) {
                    return;
                }
                SystemUiLite.install(loadPackageParam);
            }
        } catch (Throwable th) {
            Log.e("SunshineParts", "Scope skipped: " + loadPackageParam.packageName, th);
        }
    }
}
