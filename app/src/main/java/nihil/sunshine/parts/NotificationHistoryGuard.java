package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class NotificationHistoryGuard {
    static void install(ClassLoader classLoader) {
        try {
            Class<?> cls = Class.forName("com.android.server.notification.NotificationHistoryProtoHelper", false, classLoader);
            Class<?> cls2 = Class.forName("android.util.proto.ProtoOutputStream", false, classLoader);
            Class<?> cls3 = Class.forName("android.app.NotificationHistory$HistoricalNotification", false, classLoader);
            Method declaredMethod = cls.getDeclaredMethod("writeIcon", cls2, cls3);
            final Method declaredMethod2 = cls3.getDeclaredMethod("getIcon", new Class[0]);
            declaredMethod.setAccessible(true);
            declaredMethod2.setAccessible(true);
            RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.NotificationHistoryGuard.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                    Object obj = methodHookParam.args[1];
                    if (obj == null || declaredMethod2.invoke(obj, new Object[0]) == null) {
                        methodHookParam.setResult((Object) null);
                    }
                }
            });
            ControlBus.feature("notif_history_guard", "ready");
        } catch (Throwable th) {
            ControlBus.feature("notif_history_guard", "native fallback");
        }
    }

    private NotificationHistoryGuard() {
    }
}
