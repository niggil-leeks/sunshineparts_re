package nihil.sunshine.parts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.SparseArray;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class RefreshBoundary {
    private static Access access;
    private static boolean available;
    private static PowerManager power;
    private static volatile boolean safe;
    private static volatile WeakReference<Object> director = new WeakReference<>(null);
    private static String status = "";

    static boolean available() {
        return available;
    }

    static void director(Object obj) {
        director = new WeakReference<>(obj);
    }

    static String state() {
        return status;
    }

    static void attach(Context context) {
        power = (PowerManager) context.getSystemService(PowerManager.class);
        if (power == null) {
            return;
        }
        updateSafety();
        context.registerReceiver(new BroadcastReceiver() { // from class: nihil.sunshine.parts.RefreshBoundary.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                RefreshBoundary.updateSafety();
                RefreshHook.reload();
            }
        }, new IntentFilter() { // from class: nihil.sunshine.parts.RefreshBoundary.2
            {
                addAction("android.intent.action.SCREEN_ON");
                addAction("android.intent.action.SCREEN_OFF");
                addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
            }
        }, 4);
        power.addThermalStatusListener(new PowerManager.OnThermalStatusChangedListener() { // from class: nihil.sunshine.parts.RefreshBoundary$$ExternalSyntheticLambda0
            @Override // android.os.PowerManager.OnThermalStatusChangedListener
            public final void onThermalStatusChanged(int i) {
                RefreshBoundary.lambda$attach$0(i);
            }
        });
        RefreshHook.reload();
    }

    static /* synthetic */ void lambda$attach$0(int i) {
        updateSafety();
        RefreshHook.reload();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateSafety() {
        try {
            safe = power.isInteractive() && !power.isPowerSaveMode() && power.getCurrentThermalStatus() < 3;
        } catch (Exception e) {
            safe = false;
        }
    }

    static void install(ClassLoader classLoader) {
        try {
            final Access access2 = new Access(classLoader);
            access = access2;
            if (RuntimeHooks.hookMethod(access2.setter, new XC_MethodHook() { // from class: nihil.sunshine.parts.RefreshBoundary.3
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (Config.refresh && !LicenseRuntime.blocked() && methodHookParam.args[0] != null && methodHookParam.args[1] != null) {
                        if (!RefreshBoundary.safe) {
                            String unused = RefreshBoundary.status = "SUSPENDED: native power, thermal or display state";
                            return;
                        }
                        try {
                            Object obj = RefreshBoundary.director.get();
                            if (obj != null && access2.safeVotes(obj)) {
                                Object objPin = access2.pin(methodHookParam.args[0], methodHookParam.args[1]);
                                if (objPin != null) {
                                    methodHookParam.args[1] = objPin;
                                    methodHookParam.setObjectExtra("parts.refresh.pin", Boolean.TRUE);
                                }
                                return;
                            }
                            String unused2 = RefreshBoundary.status = "SUSPENDED: native safety vote";
                        } catch (Throwable th) {
                            String unused3 = RefreshBoundary.status = "Native fallback: display boundary unavailable";
                        }
                    }
                }

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (Boolean.TRUE.equals(methodHookParam.getObjectExtra("parts.refresh.pin"))) {
                        String unused = RefreshBoundary.status = (methodHookParam.hasThrowable() || !Boolean.TRUE.equals(methodHookParam.getResult())) ? "Native fallback: display request rejected" : "ACTIVE: built-in display physical and render ranges pinned at native 120 Hz";
                    }
                }
            }) == null) {
                throw new IllegalStateException("Display hook refused");
            }
            available = true;
        } catch (Throwable th) {
            status = "Native fallback: display boundary unavailable";
        }
    }

    static final class Access {
        final Field app;
        int cachedMode = -1;
        float cachedRate;
        Object cachedToken;
        final Constructor<?> copy;
        final Method dynamicInfo;
        final Field group;
        final Field idle;
        final Method ids;
        final Field isInternal;
        final Field mode;
        final Field primary;
        final Method priorityName;
        final Method setter;
        final Class<?> spec;
        final Method staticInfo;
        final Field supported;
        final Method token;
        final Class<?> voteClass;

        Access(ClassLoader classLoader) throws Exception {
            Class<?> cls = Class.forName("android.view.SurfaceControl", false, classLoader);
            this.spec = Class.forName("android.view.SurfaceControl$DesiredDisplayModeSpecs", false, classLoader);
            this.setter = cls.getDeclaredMethod("setDesiredDisplayModeSpecs", IBinder.class, this.spec);
            if (this.setter.getReturnType() != Boolean.TYPE) {
                throw new NoSuchMethodException();
            }
            this.ids = cls.getDeclaredMethod("getPhysicalDisplayIds", new Class[0]);
            this.token = cls.getDeclaredMethod("getPhysicalDisplayToken", Long.TYPE);
            this.staticInfo = cls.getDeclaredMethod("getStaticDisplayInfo", Long.TYPE);
            this.dynamicInfo = cls.getDeclaredMethod("getDynamicDisplayInfo", Long.TYPE);
            this.isInternal = RefreshBoundary.field(this.staticInfo.getReturnType(), "isInternal");
            this.supported = RefreshBoundary.field(this.dynamicInfo.getReturnType(), "supportedDisplayModes");
            this.primary = RefreshBoundary.field(this.spec, "primaryRanges");
            this.app = RefreshBoundary.field(this.spec, "appRequestRanges");
            this.mode = RefreshBoundary.field(this.spec, "defaultMode");
            this.group = RefreshBoundary.field(this.spec, "allowGroupSwitching");
            this.idle = RefreshBoundary.field(this.spec, "idleScreenRefreshRateConfig");
            this.copy = this.spec.getDeclaredConstructor(this.spec);
            this.copy.setAccessible(true);
            if (this.isInternal.getType() != Boolean.TYPE || this.mode.getType() != Integer.TYPE || this.group.getType() != Boolean.TYPE || !this.supported.getType().isArray()) {
                throw new NoSuchFieldException();
            }
            this.voteClass = Class.forName("com.android.server.display.mode.Vote", false, classLoader);
            this.priorityName = this.voteClass.getDeclaredMethod("priorityToString", Integer.TYPE);
            this.priorityName.setAccessible(true);
            Method[] methodArr = {this.setter, this.ids, this.token, this.staticInfo, this.dynamicInfo};
            for (int i = 0; i < 5; i++) {
                methodArr[i].setAccessible(true);
            }
        }

        boolean safeVotes(Object obj) throws Exception {
            synchronized (RefreshBoundary.field(obj.getClass(), "mLock").get(obj)) {
                Object obj2 = RefreshBoundary.field(obj.getClass(), "mVotesStorage").get(obj);
                if (obj2 == null) {
                    return false;
                }
                Method declaredMethod = obj2.getClass().getDeclaredMethod("getVotes", Integer.TYPE);
                declaredMethod.setAccessible(true);
                Object objInvoke = declaredMethod.invoke(obj2, 0);
                if (!(objInvoke instanceof SparseArray)) {
                    return false;
                }
                SparseArray sparseArray = (SparseArray) objInvoke;
                for (int i = 0; i < sparseArray.size(); i++) {
                    if (sparseArray.valueAt(i) != null) {
                        String strValueOf = String.valueOf(this.priorityName.invoke(null, Integer.valueOf(sparseArray.keyAt(i))));
                        if (RatePolicy.protectedVote(strValueOf) && !RatePolicy.compatibleLimit(strValueOf, sparseArray.valueAt(i))) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }

        Object pin(Object obj, Object obj2) throws Exception {
            float f;
            int i;
            Object obj3;
            Object objInvoke;
            Object obj4;
            int i2;
            int i3;
            long[] jArr = (long[]) this.ids.invoke(null, new Object[0]);
            int length = jArr.length;
            int i4 = 0;
            while (true) {
                f = 0.0f;
                i = -1;
                if (i4 < length) {
                    long j = jArr[i4];
                    if (obj.equals(this.token.invoke(null, Long.valueOf(j)))) {
                        Object objInvoke2 = this.staticInfo.invoke(null, Long.valueOf(j));
                        if (objInvoke2 != null && this.isInternal.getBoolean(objInvoke2) && (objInvoke = this.dynamicInfo.invoke(null, Long.valueOf(j))) != null && (obj4 = this.supported.get(objInvoke)) != null) {
                            int i5 = this.mode.getInt(obj2);
                            int i6 = 0;
                            while (true) {
                                if (i6 >= Array.getLength(obj4)) {
                                    i2 = -1;
                                    i3 = -1;
                                    break;
                                }
                                Object obj5 = Array.get(obj4, i6);
                                Class<?> cls = obj5.getClass();
                                if (RefreshBoundary.field(cls, "id").getInt(obj5) == i5) {
                                    i2 = RefreshBoundary.field(cls, "width").getInt(obj5);
                                    i3 = RefreshBoundary.field(cls, "height").getInt(obj5);
                                    break;
                                }
                                i6++;
                            }
                            if (i2 > 0 && i3 > 0) {
                                int i7 = 0;
                                while (true) {
                                    if (i7 >= Array.getLength(obj4)) {
                                        obj3 = null;
                                        break;
                                    }
                                    obj3 = Array.get(obj4, i7);
                                    Class<?> cls2 = obj3.getClass();
                                    float f2 = RefreshBoundary.field(cls2, "peakRefreshRate").getFloat(obj3);
                                    if (RatePolicy.isTargetMode(RefreshBoundary.field(cls2, "width").getInt(obj3), RefreshBoundary.field(cls2, "height").getInt(obj3), f2, RefreshBoundary.field(cls2, "vsyncRate").getFloat(obj3), i2, i3)) {
                                        i = RefreshBoundary.field(cls2, "id").getInt(obj3);
                                        f = f2;
                                        break;
                                    }
                                    i7++;
                                }
                            } else {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    } else {
                        i4++;
                    }
                } else {
                    obj3 = null;
                    break;
                }
            }
            if (obj3 == null || i < 0) {
                return null;
            }
            Object objNewInstance = this.copy.newInstance(obj2);
            if (this.primary.get(objNewInstance) == this.primary.get(obj2) || this.app.get(objNewInstance) == this.app.get(obj2)) {
                throw new IllegalStateException("Shared ranges");
            }
            pinRange(this.primary.get(objNewInstance), f);
            pinRange(this.app.get(objNewInstance), f);
            this.mode.setInt(objNewInstance, i);
            this.group.setBoolean(objNewInstance, false);
            this.idle.set(objNewInstance, null);
            return objNewInstance;
        }

        static void pinRange(Object obj, float f) throws Exception {
            String[] strArr = {"physical", "render"};
            for (int i = 0; i < 2; i++) {
                Object obj2 = RefreshBoundary.field(obj.getClass(), strArr[i]).get(obj);
                RefreshBoundary.field(obj2.getClass(), "min").setFloat(obj2, f);
                RefreshBoundary.field(obj2.getClass(), "max").setFloat(obj2, f);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Field field(Class<?> cls, String str) throws Exception {
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

    private RefreshBoundary() {
    }
}
