package nihil.sunshine.parts;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.view.Display;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class RootOps {
    final Context app;
    final SharedPreferences backup;

    RootOps(Context context) {
        this.app = context.getApplicationContext();
        this.backup = this.app.getSharedPreferences("flight_baseline", 0);
    }

    static final class Result {
        final int code;
        final String text;

        Result(int i, String str) {
            this.code = i;
            this.text = str;
        }

        void require() throws IOException {
            if (this.code != 0) {
                throw new IOException(this.text.isEmpty() ? "Root command failed (" + this.code + ")" : this.text);
            }
        }
    }

    Result run(String str, int i) throws Exception {
        Result result;
        final Process processStart = new ProcessBuilder("su", "-c", str).redirectErrorStream(true).start();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Thread thread = new Thread(new Runnable() { // from class: nihil.sunshine.parts.RootOps$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RootOps.lambda$run$0(processStart, byteArrayOutputStream);
            }
        }, "flight-root-output");
        thread.setDaemon(true);
        thread.start();
        if (!processStart.waitFor(i, TimeUnit.SECONDS)) {
            processStart.destroyForcibly();
            thread.join(500L);
            throw new IOException("Root request timed out. Check your root manager.");
        }
        thread.join(1500L);
        synchronized (byteArrayOutputStream) {
            result = new Result(processStart.exitValue(), new String(byteArrayOutputStream.toByteArray(), "UTF-8").trim());
        }
        return result;
    }

    static /* synthetic */ void lambda$run$0(Process process, ByteArrayOutputStream byteArrayOutputStream) {
        try {
            InputStream inputStream = process.getInputStream();
            try {
                byte[] bArr = new byte[4096];
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i == -1) {
                        break;
                    }
                    synchronized (byteArrayOutputStream) {
                        try {
                            int iMin = Math.min(i, 262144 - byteArrayOutputStream.size());
                            if (iMin > 0) {
                                byteArrayOutputStream.write(bArr, 0, iMin);
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable th2) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable th3) {
                        th2.addSuppressed(th3);
                    }
                }
                throw th2;
            }
        } catch (IOException e) {
        }
    }

    String value(String str) throws Exception {
        Result resultRun = run(str, 15);
        resultRun.require();
        return resultRun.text;
    }

    void verifyRoot() throws Exception {
        Result resultRun = run("if [ \"$(id -u)\" = 0 ]; then echo SUNSHINE_ROOT_OK; else exit 13; fi", 15);
        if (!RootProbe.granted(resultRun.code, resultRun.text)) {
            throw new IOException(I18n.text(this.app, "The root shell did not grant access.") + "\n" + resultRun.text);
        }
    }

    void verifyPlatform() throws Exception {
        if (!Config.target(Build.DISPLAY, value("getprop ro.build.version.oneui"), Build.VERSION.SDK_INT)) {
            throw new IOException(I18n.text(this.app, "This build requires Retriever, One UI 8.0 and Android 16."));
        }
    }

    private String nonce() {
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(bArr[i] & 255)));
        }
        return sb.toString();
    }

    JSONObject control(String str, String str2) throws Exception {
        if (!Arrays.asList("android", "com.android.systemui", "com.sec.android.app.launcher").contains(str)) {
            throw new IllegalArgumentException();
        }
        if (!Arrays.asList("status", "reload").contains(str2)) {
            throw new IllegalArgumentException();
        }
        String strNonce = nonce();
        Matcher matcher = Pattern.compile("SP250:" + strNonce + ":([A-Za-z0-9+/=]+)").matcher(value("[ \"$(id -u)\" = 0 ] || exit 13\nam broadcast --receiver-foreground -a nihil.sunshine.parts.FLIGHT_CONTROL -p " + str + " --es op " + str2 + " --es nonce " + strNonce));
        if (!matcher.find()) {
            throw new IOException("No response from " + str + ". Enable its LSPosed scope and restart the process.");
        }
        JSONObject jSONObject = new JSONObject(new String(Base64.decode(matcher.group(1), 0), "UTF-8"));
        if (!Config.compatibleRuntimeVersion(jSONObject.optString("version"))) {
            throw new IOException(I18n.format(this.app, "The process is running module %1$s; this app is %2$s. Restart the device to load the installed module.", jSONObject.optString("version", "?"), Config.VERSION));
        }
        if (!str.equals(jSONObject.optString("scope")) || !jSONObject.optBoolean("active") || !Config.target(jSONObject.optString("display"), jSONObject.optString("oneui"), 36)) {
            throw new IOException("Module is not ready in " + str);
        }
        return jSONObject;
    }

    private int bootCount() {
        try {
            return Settings.Global.getInt(this.app.getContentResolver(), "boot_count", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    boolean reducedLoggingActive() {
        return this.backup.getBoolean("reduced_logging", true);
    }

    void ensureReducedLogging() throws Exception {
        ensureReducedLogging(15);
    }

    void ensureReducedLogging(int i) throws Exception {
        if (this.backup.getBoolean("reduced_logging", true)) {
            setReducedLogging(true, i);
        }
    }

    void setReducedLogging(boolean z) throws Exception {
        setReducedLogging(z, 15);
    }

    private void setReducedLogging(boolean z, int i) throws Exception {
        synchronized (DiagnosticLogs.class) {
            int iBootCount = bootCount();
            if (z) {
                Result resultRun = run(DiagnosticLogs.read(), i);
                resultRun.require();
                Map<String, String> map = DiagnosticLogs.parse(resultRun.text);
                if (map.size() != DiagnosticLogs.sources().size()) {
                    throw new IOException("Couldn't read the logging profile");
                }
                SharedPreferences.Editor editorEdit = this.backup.edit();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (!this.backup.contains("logbaseline:" + entry.getKey())) {
                        editorEdit.putString("logbaseline:" + entry.getKey(), entry.getValue());
                    }
                }
                editorEdit.putInt("logging_baseline_boot", iBootCount);
                if (!editorEdit.commit()) {
                    throw new IOException("Couldn't save the previous logging profile");
                }
                Result resultRun2 = run(DiagnosticLogs.write(map, true) + DiagnosticLogs.read(), i);
                resultRun2.require();
                if (!DiagnosticLogs.reduced(DiagnosticLogs.parse(resultRun2.text))) {
                    throw new IOException("Logging filter could not be verified");
                }
                if (!this.backup.edit().putBoolean("reduced_logging", true).putInt("logging_applied_boot", iBootCount).commit()) {
                    throw new IOException("Logging applied but its status could not be saved");
                }
            } else {
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                for (String str : DiagnosticLogs.sources()) {
                    if (this.backup.contains("logbaseline:" + str)) {
                        linkedHashMap.put(str, this.backup.getString("logbaseline:" + str, ""));
                    }
                }
                if (!linkedHashMap.isEmpty()) {
                    run(DiagnosticLogs.write(linkedHashMap, false), i).require();
                }
                SharedPreferences.Editor editorRemove = this.backup.edit().putBoolean("reduced_logging", false).remove("logging_applied_boot").remove("logging_baseline_boot");
                Iterator<String> it = DiagnosticLogs.sources().iterator();
                while (it.hasNext()) {
                    editorRemove.remove("logbaseline:" + it.next());
                }
                if (!editorRemove.commit()) {
                    throw new IOException("Logging restored but its preference could not be saved");
                }
            }
        }
    }

    JSONObject reload() throws Exception {
        JSONObject jSONObjectControl = control("com.android.systemui", "reload");
        JSONArray jSONArray = new JSONArray();
        String[] strArr = {"android", "com.sec.android.app.launcher"};
        for (int i = 0; i < 2; i++) {
            String str = strArr[i];
            try {
                JSONObject jSONObjectControl2 = control(str, "reload");
                if ("android".equals(str)) {
                    jSONObjectControl.put("framework_status", jSONObjectControl2);
                } else {
                    jSONObjectControl.put("launcher_status", jSONObjectControl2);
                }
            } catch (Exception e) {
                jSONArray.put(str);
            }
        }
        jSONObjectControl.put("pending_scopes", jSONArray);
        return jSONObjectControl;
    }

    JSONObject quickInfo() {
        JSONObject jSONObject;
        try {
            jSONObject = new JSONObject(this.backup.getString("dashboard_snapshot", "{}"));
        } catch (Exception e) {
            jSONObject = new JSONObject();
        }
        try {
            jSONObject.put("DISPLAY", Build.DISPLAY);
            jSONObject.put("MODEL", Build.MODEL);
            jSONObject.put("DEVICE", Build.DEVICE);
            jSONObject.put("ANDROID", Build.VERSION.RELEASE);
            jSONObject.put("KERNEL", System.getProperty("os.version", ""));
            jSONObject.remove("available_kb");
            jSONObject.remove("total_kb");
            jSONObject.remove("memory_threshold_kb");
            jSONObject.remove("low_memory");
            ActivityManager activityManager = (ActivityManager) this.app.getSystemService(ActivityManager.class);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            if (activityManager != null) {
                activityManager.getMemoryInfo(memoryInfo);
                jSONObject.put("total_kb", memoryInfo.totalMem / 1024);
                jSONObject.put("available_kb", memoryInfo.availMem / 1024);
                jSONObject.put("memory_threshold_kb", memoryInfo.threshold / 1024);
                jSONObject.put("low_memory", memoryInfo.lowMemory);
            }
            try {
                Intent intentRegisterReceiver = this.app.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
                jSONObject.put("battery_temperature", intentRegisterReceiver != null ? intentRegisterReceiver.getIntExtra("temperature", -1) : -1);
            } catch (Exception e2) {
                jSONObject.remove("battery_temperature");
            }
            if (this.backup.contains("performance_mode")) {
                jSONObject.put("PROCESSING", this.backup.getInt("performance_mode", 0));
            }
            if (this.backup.contains("performance_light")) {
                jSONObject.put("LIGHT", this.backup.getBoolean("performance_light", false) ? "1" : "0");
            }
            if (!jSONObject.has("CACHED")) {
                jSONObject.put("CACHED", "null");
            }
        } catch (Exception e3) {
        }
        return jSONObject;
    }

    JSONObject info() throws Exception {
        return info(false);
    }

    JSONObject info(boolean z) throws Exception {
        String str = z ? "printf 'ONEUI='; getprop ro.build.version.oneui\nprintf 'CACHED='; device_config get activity_manager max_cached_processes\nprintf 'FREEZER='; settings get global cached_apps_freezer\nprintf 'PROCESSING='; settings get global enhanced_processing\nprintf 'LIGHT='; settings get global sem_low_heat_mode\nprintf 'RENDERER='; getprop debug.hwui.renderer\nprintf 'POWER_SAVING='; settings get global low_power\nprintf 'ADAPTIVE='; settings get global adaptive_battery_management_enabled\nprintf 'MIN_HZ='; settings get system min_refresh_rate\nprintf 'PEAK_HZ='; settings get system peak_refresh_rate\ndumpsys activity settings | grep 'CUR_MAX_CACHED_PROCESSES=' | head -n 1\nfor p in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do [ -d \"$p\" ] || continue; read -r lo < \"$p/scaling_min_freq\" || lo=0; read -r hi < \"$p/scaling_max_freq\" || hi=0; read -r cur < \"$p/scaling_cur_freq\" || cur=0; read -r hw < \"$p/cpuinfo_max_freq\" || hw=0; read -r cores < \"$p/related_cpus\" || cores=''; printf 'CPU_%s=%s|%s|%s|%s|%s\\n' \"${p##*/}\" \"$lo\" \"$hi\" \"$cur\" \"$hw\" \"$cores\"; done\n" : "printf 'ONEUI='; getprop ro.build.version.oneui\nprintf 'CACHED='; device_config get activity_manager max_cached_processes\nprintf 'FREEZER='; settings get global cached_apps_freezer\nprintf 'PROCESSING='; settings get global enhanced_processing\nprintf 'LIGHT='; settings get global sem_low_heat_mode\nprintf 'RENDERER='; getprop debug.hwui.renderer\nprintf 'POWER_SAVING='; settings get global low_power\nprintf 'ADAPTIVE='; settings get global adaptive_battery_management_enabled\nprintf 'MIN_HZ='; settings get system min_refresh_rate\nprintf 'PEAK_HZ='; settings get system peak_refresh_rate\ndumpsys activity settings | grep 'CUR_MAX_CACHED_PROCESSES=' | head -n 1\n";
        JSONObject jSONObjectQuickInfo = quickInfo();
        StringBuilder sb = new StringBuilder();
        JSONArray jSONArray = new JSONArray();
        for (String str2 : value(str).split("\\n")) {
            int iIndexOf = str2.indexOf(61);
            if (iIndexOf > 0) {
                String strTrim = str2.substring(0, iIndexOf).trim();
                String strTrim2 = str2.substring(iIndexOf + 1).trim();
                if (strTrim.startsWith("CPU_")) {
                    String[] strArrSplit = strTrim2.split("\\|", -1);
                    if (strArrSplit.length == 5) {
                        try {
                            JSONObject jSONObject = new JSONObject();
                            jSONObject.put("name", strTrim.substring(4));
                            jSONObject.put("minimum", Long.parseLong(strArrSplit[0]));
                            jSONObject.put("maximum", Long.parseLong(strArrSplit[1]));
                            jSONObject.put("current", Long.parseLong(strArrSplit[2]));
                            jSONObject.put("hardware", Long.parseLong(strArrSplit[3]));
                            jSONObject.put("cores", strArrSplit[4]);
                            jSONArray.put(jSONObject);
                        } catch (NumberFormatException e) {
                        }
                    }
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(strTrim.substring(4)).append(": ").append(strTrim2);
                } else {
                    jSONObjectQuickInfo.put(strTrim, strTrim2);
                }
            }
        }
        if (z) {
            jSONObjectQuickInfo.put("cpu_limits", sb.length() == 0 ? "Not reported" : sb.toString());
            jSONObjectQuickInfo.put("cpu_policies", jSONArray);
            jSONObjectQuickInfo.put("cpu_read_at", System.currentTimeMillis());
        }
        if (this.backup.contains("performance_mode")) {
            jSONObjectQuickInfo.put("PROCESSING", this.backup.getInt("performance_mode", 0));
        }
        if (this.backup.contains("performance_light")) {
            jSONObjectQuickInfo.put("LIGHT", this.backup.getBoolean("performance_light", false) ? "1" : "0");
        }
        this.backup.edit().putString("dashboard_snapshot", jSONObjectQuickInfo.toString()).apply();
        return jSONObjectQuickInfo;
    }

    void transitionEffects(String str) throws Exception {
        if (!Arrays.asList("default", "shadow", "corners").contains(str)) {
            throw new IllegalArgumentException("Invalid transition effects");
        }
        run(FlightCommands.property("persist.sys.sunshineparts.transition_effects", str), 15).require();
    }

    void launcherPreset(String str) throws Exception {
        if (!Arrays.asList("auto", "low", "lowest", "mass", "high").contains(str)) {
            throw new IllegalArgumentException("Invalid launcher preset");
        }
        run(FlightCommands.property("persist.sys.sunshineparts.launcher_preset", str), 15).require();
    }

    void workload(String str) throws Exception {
        run(FlightCommands.workload(str), 15).require();
    }

    void flag(String str, boolean z) throws Exception {
        run(FlightCommands.flag(str, z), 15).require();
    }

    String displayPolicy(boolean z) throws Exception {
        boolean z2;
        if (z) {
            DisplayManager displayManager = (DisplayManager) this.app.getSystemService(DisplayManager.class);
            Display display = displayManager == null ? null : displayManager.getDisplay(0);
            if (display == null) {
                z2 = false;
            } else {
                Display.Mode mode = display.getMode();
                z2 = false;
                for (Display.Mode mode2 : display.getSupportedModes()) {
                    if (mode2.getPhysicalWidth() == mode.getPhysicalWidth() && mode2.getPhysicalHeight() == mode.getPhysicalHeight() && Math.abs(mode2.getRefreshRate() - 120.0f) < 0.1f) {
                        z2 = true;
                    }
                }
            }
            if (!z2) {
                throw new IOException("No native 120 Hz mode at the active resolution");
            }
            saveSetting("system", "min_refresh_rate");
            saveSetting("system", "peak_refresh_rate");
        }
        String strValue = value("settings get system min_refresh_rate");
        String strValue2 = value("settings get system peak_refresh_rate");
        String strValue3 = value("getprop persist.sys.sunshineparts.refresh");
        boolean z3 = this.backup.getBoolean("display_policy_owned", false);
        String string = z ? "120.0" : this.backup.getString("setting:system:min_refresh_rate", "null");
        String string2 = z ? "120.0" : this.backup.getString("setting:system:peak_refresh_rate", "null");
        if (z || z3) {
            try {
                run(FlightCommands.refreshSettings(string, string2), 15).require();
                if (!value("settings get system min_refresh_rate").equals(string) || !value("settings get system peak_refresh_rate").equals(string2)) {
                    throw new IOException("Android did not confirm the refresh settings");
                }
            } catch (Exception e) {
                try {
                    run(FlightCommands.refreshSettings(strValue, strValue2), 15).require();
                    run(FlightCommands.property("persist.sys.sunshineparts.refresh", strValue3), 15).require();
                } catch (Exception e2) {
                    e.addSuppressed(e2);
                }
                throw e;
            }
        }
        flag("refresh", z);
        if (this.backup.edit().putBoolean("display_policy_owned", z).commit()) {
            return z ? "120 Hz preference saved. Native minimum and peak settings were verified. Open Module report for framework policy and the app display rate." : "Original native refresh settings restored.";
        }
        throw new IOException("Could not save display preference");
    }

    void mediaStep(int i) throws Exception {
        if (i != 2 && i != 4 && i != 8) {
            throw new IllegalArgumentException();
        }
        run(FlightCommands.property("persist.sys.sunshineparts.media_step", Integer.toString(i)), 15).require();
    }

    private void remember(String str, String str2) throws IOException {
        if (!this.backup.contains(str) && !this.backup.edit().putString(str, str2).commit()) {
            throw new IOException("Could not save the original setting; no change applied.");
        }
    }

    private void saveSetting(String str, String str2) throws Exception {
        remember("setting:" + str + ":" + str2, value("settings get " + str + " " + str2));
    }

    void cached(String str) throws Exception {
        remember("device:activity_manager:max_cached_processes", value("device_config get activity_manager max_cached_processes"));
        run(FlightCommands.maximum(str), 15).require();
        String strValue = value("device_config get activity_manager max_cached_processes");
        if (str.equals("default")) {
            str = "null";
        }
        if (!strValue.equals(str)) {
            throw new IOException("ActivityManager rejected the limit");
        }
    }

    void freezer(String str) throws Exception {
        saveSetting("global", "cached_apps_freezer");
        run(FlightCommands.freezer(str), 15).require();
        if (!value("settings get global cached_apps_freezer").equals(str)) {
            throw new IOException("Freezer setting rejected");
        }
    }

    /* JADX WARN: Code duplicated, block: B:33:0x0051 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    private File asset(String str) throws Exception {
        if (!Arrays.asList("performance-engine.sh", "restart-systemui.sh").contains(str)) {
            throw new IllegalArgumentException();
        }
        File file = new File(this.app.getFilesDir(), str);
        InputStream inputStreamOpen = this.app.getAssets().open(str);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = inputStreamOpen.read(bArr);
                    if (i <= 0) {
                        break;
                    }
                    fileOutputStream.write(bArr, 0, i);
                    if (inputStreamOpen != null) {
                        try {
                            inputStreamOpen.close();
                        } catch (Throwable th) {
                            th.addSuppressed(th);
                        }
                    }
                    throw th;
                }
                fileOutputStream.close();
                if (inputStreamOpen != null) {
                    inputStreamOpen.close();
                }
                return file;
            } catch (Throwable th2) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th3) {
                    th2.addSuppressed(th3);
                }
                throw th2;
            }
        } catch (Throwable th4) {
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            throw th4;
        }
    }

    private String performance(int i, boolean z, int i2) throws Exception {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException();
        }
        Result resultRun = run("sh " + FlightCommands.quote(asset("performance-engine.sh").getAbsolutePath()) + " " + FlightCommands.quote(new File(this.app.getFilesDir(), "performance-baseline").getAbsolutePath()) + " " + i + " " + (z ? 1 : 0), i2);
        resultRun.require();
        return resultRun.text;
    }

    void applySavedPerformance(int i) throws Exception {
        if (this.backup.getBoolean("performance_active", false)) {
            control("com.android.systemui", "status");
            performance(this.backup.getInt("performance_mode", 0), this.backup.getBoolean("performance_light", false), i);
        }
    }

    String processing(int i) throws Exception {
        boolean zEquals;
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException();
        }
        saveSetting("global", "enhanced_processing");
        saveSetting("global", "sem_enhanced_cpu_responsiveness");
        if (this.backup.contains("performance_light")) {
            zEquals = this.backup.getBoolean("performance_light", false);
        } else {
            zEquals = value("settings get global sem_low_heat_mode").equals("1");
        }
        String strPerformance = performance(i, zEquals, 20);
        if (!this.backup.edit().putInt("performance_mode", i).putBoolean("performance_light", zEquals).putBoolean("performance_active", true).commit()) {
            throw new IOException("CPU profile applied, but it could not be saved for boot.");
        }
        try {
            run(FlightCommands.processing(i), 15).require();
            return strPerformance;
        } catch (Exception e) {
            return strPerformance + "\nSamsung setting unavailable; the root CPU profile is verified.";
        }
    }

    String light(boolean z) throws Exception {
        saveSetting("global", "sem_low_heat_mode");
        int i = this.backup.getInt("performance_mode", 0);
        String strPerformance = performance(i, z, 20);
        if (!this.backup.edit().putInt("performance_mode", i).putBoolean("performance_light", z).putBoolean("performance_active", true).commit()) {
            throw new IOException("CPU profile applied, but it could not be saved for boot.");
        }
        try {
            value("settings put global sem_low_heat_mode " + (z ? 1 : 0));
            return strPerformance;
        } catch (Exception e) {
            return strPerformance + "\nSamsung setting unavailable; the root CPU profile is verified.";
        }
    }

    void adaptive(boolean z) throws Exception {
        saveSetting("global", "adaptive_battery_management_enabled");
        String str = z ? "1" : "0";
        value("settings put global adaptive_battery_management_enabled " + str);
        if (!value("settings get global adaptive_battery_management_enabled").equals(str)) {
            throw new IOException("Adaptive battery did not accept the change.");
        }
    }

    void powerSaving(boolean z) throws Exception {
        saveSetting("global", "low_power");
        if (z && this.backup.getInt("performance_mode", 0) > 0) {
            processing(0);
        }
        value("cmd power set-mode " + (z ? 1 : 0));
        if (!value("settings get global low_power").equals(z ? "1" : "0")) {
            throw new IOException("Power saving did not accept the change.");
        }
    }

    String restartSystemUi() throws Exception {
        String strValue = value("sh " + FlightCommands.quote(asset("restart-systemui.sh").getAbsolutePath()));
        for (int i = 0; i < 8; i++) {
            try {
                control("com.android.systemui", "status");
                return strValue + "\nModule attached to the replacement process.";
            } catch (Exception e) {
                Thread.sleep(500L);
            }
        }
        throw new IOException(strValue + "\nThe module has not replied yet. Reopen Parts when SystemUI is ready.");
    }

    void renderer(String str) throws Exception {
        if (!Arrays.asList("default", "skiavk", "skiagl").contains(str)) {
            throw new IllegalArgumentException();
        }
        saveProp("debug.hwui.renderer");
        if (str.equals("default")) {
            str = "";
        }
        run(FlightCommands.property("debug.hwui.renderer", str), 15).require();
    }

    private void saveProp(String str) throws Exception {
        remember("prop:" + str, value("getprop " + str));
    }

    String ram() throws Exception {
        return value("am kill-all; printf '\\n'; head -n 3 /proc/meminfo");
    }

    String shaders() throws Exception {
        return value(FlightCommands.shaderQuarantine());
    }

    String restoreSettings() throws Exception {
        String str;
        setReducedLogging(false);
        if (this.backup.getBoolean("display_policy_owned", false)) {
            displayPolicy(false);
        }
        if (this.backup.getBoolean("performance_active", false)) {
            performance(0, false, 20);
            this.backup.edit().remove("performance_active").remove("performance_mode").remove("performance_light").commit();
        }
        int i = 0;
        for (Map.Entry<String, ?> entry : this.backup.getAll().entrySet()) {
            String[] strArrSplit = entry.getKey().split(":", 3);
            String string = entry.getValue().toString();
            if (strArrSplit.length == 3 && strArrSplit[0].equals("setting") && strArrSplit[1].equals("global") && Arrays.asList("cached_apps_freezer", "enhanced_processing", "sem_enhanced_cpu_responsiveness", "sem_low_heat_mode", "adaptive_battery_management_enabled", "low_power").contains(strArrSplit[2])) {
                if (strArrSplit[2].equals("low_power")) {
                    value("cmd power set-mode " + (string.equals("1") ? 1 : 0));
                    i++;
                } else {
                    value("settings " + (string.equals("null") ? "delete" : "put") + " global " + strArrSplit[2] + (string.equals("null") ? "" : " " + FlightCommands.quote(string)));
                    i++;
                }
            } else if (entry.getKey().equals("device:activity_manager:max_cached_processes")) {
                if (!string.equals("null") && !string.matches("[0-9]{1,5}")) {
                    throw new IOException("Invalid saved app limit");
                }
                if (string.equals("null")) {
                    str = "device_config delete activity_manager max_cached_processes";
                } else {
                    str = "device_config put activity_manager max_cached_processes " + string;
                }
                value(str);
                i++;
            } else if (entry.getKey().startsWith("prop:")) {
                run(FlightCommands.property(entry.getKey().substring(5), string), 15).require();
                i++;
            }
        }
        return i + " original settings restored. Restart affected processes or reboot.";
    }

    void checkpointAccessBaseline() throws Exception {
        String str;
        Properties properties = new Properties();
        for (Map.Entry<String, ?> entry : this.backup.getAll().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                str = "s";
            } else if (value instanceof Boolean) {
                str = "b";
            } else if (value instanceof Integer) {
                str = "i";
            } else if (value instanceof Long) {
                str = "l";
            } else {
                str = value instanceof Float ? "f" : "";
            }
            if (!str.isEmpty()) {
                properties.setProperty(entry.getKey(), str + value.toString());
            }
        }
        StringWriter stringWriter = new StringWriter();
        properties.store(stringWriter, "Parts owned baseline");
        String strQuote = FlightCommands.quote(new File(this.app.getFilesDir(), "performance-baseline").getAbsolutePath());
        run("umask 077\nmkdir -p " + PassageGate.DIR + "\nchmod 700 " + PassageGate.DIR + "\nprintf '%s' " + FlightCommands.quote(stringWriter.toString()) + " > " + PassageGate.DIR + "/owned.tmp\nchmod 600 " + PassageGate.DIR + "/owned.tmp\nmv " + PassageGate.DIR + "/owned.tmp " + PassageGate.DIR + "/owned\nif [ -d " + strQuote + " ]; then cp -a " + strQuote + " " + PassageGate.DIR + "/; fi", 5).require();
    }

    void restoreProtectedBaseline() throws Exception {
        if (this.backup.getAll().isEmpty()) {
            Result resultRun = run("if [ -f /data/adb/sunshineparts-access/owned ]; then cat /data/adb/sunshineparts-access/owned; fi", 5);
            resultRun.require();
            if (resultRun.text.isEmpty()) {
                return;
            }
            Properties properties = new Properties();
            properties.load(new StringReader(resultRun.text));
            if (properties.size() > 1024) {
                throw new IOException("Invalid protected baseline");
            }
            SharedPreferences.Editor editorEdit = this.backup.edit();
            for (String str : properties.stringPropertyNames()) {
                String property = properties.getProperty(str);
                if (str.length() > 256 || property.length() > 4097 || property.isEmpty()) {
                    throw new IOException("Invalid protected baseline");
                }
                String strSubstring = property.substring(1);
                switch (property.charAt(0)) {
                    case 'b':
                        if (!strSubstring.equals("true") && !strSubstring.equals("false")) {
                            throw new IOException("Invalid protected baseline");
                        }
                        editorEdit.putBoolean(str, Boolean.parseBoolean(strSubstring));
                        break;
                        break;
                    case 'f':
                        editorEdit.putFloat(str, Float.parseFloat(strSubstring));
                        break;
                    case 'i':
                        editorEdit.putInt(str, Integer.parseInt(strSubstring));
                        break;
                    case 'l':
                        editorEdit.putLong(str, Long.parseLong(strSubstring));
                        break;
                    case 's':
                        editorEdit.putString(str, strSubstring);
                        break;
                    default:
                        throw new IOException("Invalid protected baseline");
                }
            }
            if (!editorEdit.commit()) {
                throw new IOException("Couldn't restore protected baseline");
            }
            run("if [ -d /data/adb/sunshineparts-access/performance-baseline ]; then cp -a /data/adb/sunshineparts-access/performance-baseline " + FlightCommands.quote(this.app.getFilesDir().getAbsolutePath()) + "/; fi", 5).require();
        }
    }

    void snapshotAuthorizationTargets() throws Exception {
        snapshotAuthorizationTargets(false);
    }

    void snapshotAuthorizationTargets(boolean z) throws Exception {
        if (this.backup.getBoolean("access_suspended", false)) {
            return;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        Iterator<Map.Entry<String, ?>> it = this.backup.getAll().entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if (z || !this.backup.contains("access_target:" + key)) {
                String[] strArrSplit = key.split(":", 3);
                if (strArrSplit.length == 3 && strArrSplit[0].equals("setting") && (strArrSplit[1].equals("global") || strArrSplit[1].equals("system"))) {
                    linkedHashMap.put(key, "settings get " + strArrSplit[1] + " " + FlightCommands.quote(strArrSplit[2]));
                } else if (key.equals("device:activity_manager:max_cached_processes")) {
                    linkedHashMap.put(key, "device_config get activity_manager max_cached_processes");
                } else if (key.startsWith("prop:") && !key.substring(5).startsWith(Config.PREFIX)) {
                    linkedHashMap.put(key, "getprop " + FlightCommands.quote(key.substring(5)));
                }
            }
        }
        if (linkedHashMap.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            sb.append("printf '%s=' ").append(FlightCommands.quote((String) entry.getKey())).append("; ").append((String) entry.getValue()).append('\n');
        }
        Result resultRun = run(sb.toString(), 5);
        resultRun.require();
        HashMap map = new HashMap();
        for (String str : resultRun.text.split(String.valueOf('\n'))) {
            int iIndexOf = str.indexOf(61);
            if (iIndexOf > 0 && linkedHashMap.containsKey(str.substring(0, iIndexOf))) {
                map.put(str.substring(0, iIndexOf), str.substring(iIndexOf + 1).trim());
            }
        }
        if (map.size() != linkedHashMap.size()) {
            throw new IOException("Couldn't read active settings");
        }
        SharedPreferences.Editor editorEdit = this.backup.edit();
        for (Map.Entry entry2 : map.entrySet()) {
            editorEdit.putString("access_target:" + ((String) entry2.getKey()), (String) entry2.getValue());
        }
        if (!editorEdit.commit()) {
            throw new IOException("Couldn't save active settings");
        }
    }

    private String accessSettings(boolean z) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> entry : this.backup.getAll().entrySet()) {
            if (entry.getKey().startsWith("access_target:")) {
                String strSubstring = entry.getKey().substring(14);
                String strValueOf = String.valueOf(entry.getValue());
                String string = this.backup.getString(strSubstring, "null");
                String str = z ? strValueOf : string;
                String[] strArrSplit = strSubstring.split(":", 3);
                if (strArrSplit.length == 3 && strArrSplit[0].equals("setting") && (strArrSplit[1].equals("global") || strArrSplit[1].equals("system"))) {
                    String str2 = "settings get " + strArrSplit[1] + " " + FlightCommands.quote(strArrSplit[2]);
                    String str3 = "settings " + (str.equals("null") ? "delete" : "put") + " " + strArrSplit[1] + " " + FlightCommands.quote(strArrSplit[2]) + (str.equals("null") ? "" : " " + FlightCommands.quote(str));
                    StringBuilder sbAppend = sb.append("if [ \"$(").append(str2).append(")\" = ");
                    if (z) {
                        strValueOf = string;
                    }
                    sbAppend.append(FlightCommands.quote(strValueOf)).append(" ]; then ").append(str3).append("; fi\n");
                } else if (strSubstring.equals("device:activity_manager:max_cached_processes")) {
                    StringBuilder sbAppend2 = sb.append("if [ \"$(device_config get activity_manager max_cached_processes)\" = ");
                    if (z) {
                        strValueOf = string;
                    }
                    sbAppend2.append(FlightCommands.quote(strValueOf)).append(" ]; then device_config ").append(str.equals("null") ? "delete activity_manager max_cached_processes" : "put activity_manager max_cached_processes " + FlightCommands.quote(str)).append("; fi\n");
                } else if (strSubstring.startsWith("prop:") && !strSubstring.substring(5).startsWith(Config.PREFIX)) {
                    StringBuilder sbAppend3 = sb.append("if [ \"$(getprop ").append(FlightCommands.quote(strSubstring.substring(5))).append(")\" = ");
                    if (z) {
                        strValueOf = string;
                    }
                    StringBuilder sbAppend4 = sbAppend3.append(FlightCommands.quote(strValueOf)).append(" ]; then ");
                    String strSubstring2 = strSubstring.substring(5);
                    if (str.equals("null")) {
                        str = "";
                    }
                    sbAppend4.append(FlightCommands.property(strSubstring2, str)).append("\nfi\n");
                }
            }
        }
        return sb.length() == 0 ? ":\n" : sb.toString();
    }

    void suspendForAuthorization() throws Exception {
        if (this.backup.getBoolean("access_suspended", false) && this.backup.getBoolean("access_cleanup_complete", false)) {
            return;
        }
        snapshotAuthorizationTargets();
        if (!this.backup.edit().putBoolean("access_suspended", true).putBoolean("access_cleanup_complete", false).commit()) {
            throw new IOException("Couldn't retain suspended state");
        }
        if (this.backup.getBoolean("performance_active", false)) {
            performance(0, false, 10);
        }
        run(accessSettings(false), 10).require();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (String str : DiagnosticLogs.sources()) {
            if (this.backup.contains("logbaseline:" + str)) {
                linkedHashMap.put(str, this.backup.getString("logbaseline:" + str, ""));
            }
        }
        if (!linkedHashMap.isEmpty()) {
            try {
                run(DiagnosticLogs.write(linkedHashMap, false), 5).require();
            } catch (Exception e) {
            }
        }
        this.backup.edit().putBoolean("access_cleanup_complete", true).commit();
        checkpointAccessBaseline();
    }

    void restoreAfterAuthorization() throws Exception {
        if (this.backup.getBoolean("access_suspended", false)) {
            run(accessSettings(true), 10).require();
            if (this.backup.getBoolean("performance_active", false)) {
                performance(this.backup.getInt("performance_mode", 0), this.backup.getBoolean("performance_light", false), 10);
            }
            if (this.backup.getBoolean("reduced_logging", true)) {
                try {
                    ensureReducedLogging(3);
                } catch (Exception e) {
                }
            }
            if (!this.backup.edit().putBoolean("access_suspended", false).remove("access_cleanup_complete").commit()) {
                throw new IOException("Couldn't retain restored state");
            }
            checkpointAccessBaseline();
        }
    }

    /* JADX WARN: Code duplicated, block: B:31:0x0095 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    File diagnostic() throws Exception {
        File file = new File("/sdcard/Download");
        File file2 = new File(this.app.getFilesDir(), "collect-flight.sh");
        InputStream inputStreamOpen = this.app.getAssets().open("collect-flight.sh");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = inputStreamOpen.read(bArr);
                    if (i <= 0) {
                        break;
                    }
                    fileOutputStream.write(bArr, 0, i);
                    if (inputStreamOpen != null) {
                        try {
                            inputStreamOpen.close();
                        } catch (Throwable th) {
                            th.addSuppressed(th);
                        }
                    }
                    throw th;
                }
                fileOutputStream.close();
                if (inputStreamOpen != null) {
                    inputStreamOpen.close();
                }
                Result resultRun = run("sh " + FlightCommands.quote(file2.getAbsolutePath()) + " " + FlightCommands.quote(file.getAbsolutePath()), 180);
                resultRun.require();
                String str = resultRun.text;
                return new File(str.substring(str.lastIndexOf(10) + 1).trim());
            } catch (Throwable th2) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th3) {
                    th2.addSuppressed(th3);
                }
                throw th2;
            }
        } catch (Throwable th4) {
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            throw th4;
        }
    }
}
