package nihil.sunshine.parts;

import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class RefreshHook implements IXposedHookLoadPackage {
    private static final String TAG = "SunshineParts";
    private static volatile boolean installed;
    private static Layout layout;
    private static volatile boolean unsupported;
    private static String lastState = "";
    private static volatile WeakReference<Object> activeDirector = new WeakReference<>(null);

    /* JADX WARN: Bottom block not found for handler: all -> 0x0027 */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0046, code lost:
    
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static void reload() {
        /*
            java.lang.ref.WeakReference<java.lang.Object> r0 = nihil.sunshine.parts.RefreshHook.activeDirector
            java.lang.Object r0 = r0.get()
            if (r0 != 0) goto L9
            return
        L9:
            java.lang.String r1 = "mLock"
            java.lang.Object r1 = de.robv.android.xposed.XposedHelpers.getObjectField(r0, r1)     // Catch: java.lang.Throwable -> L27
            monitor-enter(r1)     // Catch: java.lang.Throwable -> L27
            java.lang.Class r2 = r0.getClass()     // Catch: java.lang.Throwable -> L24
            java.lang.String r3 = "notifyDesiredDisplayModeSpecsChangedLocked"
            r4 = 0
            java.lang.Class[] r5 = new java.lang.Class[r4]     // Catch: java.lang.Throwable -> L24
            java.lang.reflect.Method r2 = method(r2, r3, r5)     // Catch: java.lang.Throwable -> L24
            java.lang.Object[] r3 = new java.lang.Object[r4]     // Catch: java.lang.Throwable -> L24
            r2.invoke(r0, r3)     // Catch: java.lang.Throwable -> L24
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L24
            goto L46
        L24:
            r0 = move-exception
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L24
            throw r0     // Catch: java.lang.Throwable -> L27
        L27:
            r0 = move-exception
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "Preference saved; refresh reevaluation unavailable: "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.Class r0 = r0.getClass()
            java.lang.String r0 = r0.getSimpleName()
            java.lang.StringBuilder r0 = r1.append(r0)
            java.lang.String r0 = r0.toString()
            reportState(r0)
        L46:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: nihil.sunshine.parts.RefreshHook.reload():void");
    }

    static String state() {
        if (!Config.refresh) {
            return "DISABLED: preference off";
        }
        if (!RefreshBoundary.available() || RefreshBoundary.state().isEmpty()) {
            return lastState.isEmpty() ? "Framework hook not initialized" : lastState;
        }
        return RefreshBoundary.state();
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!"android".equals(loadPackageParam.packageName) || Build.VERSION.SDK_INT != 36 || installed) {
            return;
        }
        RefreshBoundary.install(loadPackageParam.classLoader);
        try {
            Class clsFindClass = XposedHelpers.findClass("com.android.server.display.mode.DisplayModeDirector", loadPackageParam.classLoader);
            Method declaredMethod = clsFindClass.getDeclaredMethod("getDesiredDisplayModeSpecs", Integer.TYPE);
            if (Modifier.isStatic(declaredMethod.getModifiers()) || declaredMethod.getReturnType() == Void.TYPE) {
                throw new IllegalStateException("Unexpected director method shape");
            }
            declaredMethod.setAccessible(true);
            for (Constructor<?> constructor : clsFindClass.getDeclaredConstructors()) {
                RuntimeHooks.hookMethod(constructor, new XC_MethodHook() { // from class: nihil.sunshine.parts.RefreshHook.1
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (methodHookParam.hasThrowable()) {
                            return;
                        }
                        WeakReference unused = RefreshHook.activeDirector = new WeakReference(methodHookParam.thisObject);
                        RefreshBoundary.director(methodHookParam.thisObject);
                    }
                });
            }
            RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.RefreshHook.2
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    String str;
                    Object obj;
                    if (RefreshHook.activeDirector.get() != methodHookParam.thisObject) {
                        WeakReference unused = RefreshHook.activeDirector = new WeakReference(methodHookParam.thisObject);
                    }
                    RefreshBoundary.director(methodHookParam.thisObject);
                    if (!Config.refresh || RefreshHook.unsupported || methodHookParam.hasThrowable() || methodHookParam.getResult() == null || methodHookParam.args.length != 1) {
                        return;
                    }
                    Integer num = 0;
                    if (!num.equals(methodHookParam.args[0])) {
                        return;
                    }
                    try {
                        synchronized (XposedHelpers.getObjectField(methodHookParam.thisObject, "mLock")) {
                            if (RefreshHook.layout == null) {
                                Layout unused2 = RefreshHook.layout = new Layout(methodHookParam.thisObject.getClass(), methodHookParam.getResult().getClass(), loadPackageParam.classLoader);
                            }
                            Decision decisionDecide = RefreshHook.layout.decide(methodHookParam.thisObject, methodHookParam.getResult());
                            obj = decisionDecide.specs;
                            str = decisionDecide.state;
                        }
                        if (obj != null) {
                            methodHookParam.setResult(obj);
                        }
                    } catch (Throwable th) {
                        boolean unused3 = RefreshHook.unsupported = true;
                        ControlBus.feature("display_policy", "unavailable");
                        str = "INACTIVE: unsupported framework shape: " + th.getClass().getSimpleName() + ": " + th.getMessage();
                    }
                    RefreshHook.reportState(str);
                }
            });
            installed = true;
            ControlBus.feature("display_policy", "ready");
            reportState("Hook installed; awaiting display policy. Android framework scope only.");
        } catch (Throwable th) {
            unsupported = true;
            ControlBus.feature("display_policy", "unavailable");
            reportState("INACTIVE: cannot install hook: " + th.getClass().getSimpleName() + ": " + th.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized void reportState(String str) {
        if (str.equals(lastState)) {
            return;
        }
        lastState = str;
        Log.i(TAG, str);
    }

    private static final class Decision {
        final Object specs;
        final String state;

        Decision(Object obj, String str) {
            this.specs = obj;
            this.state = str;
        }
    }

    private static final class Layout {
        final Field appRequest;
        final Field baseModeId;
        final Field brightness;
        final Constructor<?> constructor;
        final Field defaults;
        final Field groupSwitch;
        final Field idle;
        final Field modes;
        final Field primary;
        final Method priorityName;
        final Field[] scalarFields;
        final Field votes;
        final Map<Class<?>, ModeAccess> modeAccess = new HashMap();
        final Map<Class<?>, Method> voteMethods = new HashMap();
        final Map<Class<?>, BrightnessAccess> brightnessAccess = new HashMap();
        final Map<Class<?>, RangeAccess> ranges = new HashMap();

        Layout(Class<?> cls, Class<?> cls2, ClassLoader classLoader) throws Exception {
            this.modes = RefreshHook.field(cls, "mSupportedModesByDisplay");
            this.defaults = RefreshHook.field(cls, "mDefaultModeByDisplay");
            this.brightness = RefreshHook.field(cls, "mBrightnessObserver");
            this.votes = RefreshHook.field(cls, "mVotesStorage");
            this.baseModeId = RefreshHook.field(cls2, "baseModeId");
            this.primary = RefreshHook.field(cls2, "primary");
            this.appRequest = RefreshHook.field(cls2, "appRequest");
            this.groupSwitch = RefreshHook.field(cls2, "allowGroupSwitching");
            this.idle = RefreshHook.optionalField(cls2, "mIdleScreenRefreshRateConfig");
            this.constructor = cls2.getDeclaredConstructor(new Class[0]);
            this.constructor.setAccessible(true);
            this.priorityName = XposedHelpers.findClass("com.android.server.display.mode.Vote", classLoader).getDeclaredMethod("priorityToString", Integer.TYPE);
            this.priorityName.setAccessible(true);
            ArrayList arrayList = new ArrayList();
            while (cls2 != null && cls2 != Object.class) {
                for (Field field : cls2.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getName().equals("primary") && !field.getName().equals("appRequest")) {
                        if (Modifier.isFinal(field.getModifiers())) {
                            throw new IllegalStateException("Unsupported final sideband field " + field.getName());
                        }
                        field.setAccessible(true);
                        arrayList.add(field);
                    }
                }
                cls2 = cls2.getSuperclass();
            }
            this.scalarFields = (Field[]) arrayList.toArray(new Field[0]);
        }

        Decision decide(Object obj, Object obj2) throws Exception {
            Object obj3;
            ModeAccess modeAccess;
            Object obj4 = this.brightness.get(obj);
            if (obj4 == null) {
                return new Decision(null, "SUSPENDED: brightness observer not ready");
            }
            BrightnessAccess brightnessAccess = this.brightnessAccess.get(obj4.getClass());
            if (brightnessAccess == null) {
                brightnessAccess = new BrightnessAccess(obj4.getClass());
                this.brightnessAccess.put(obj4.getClass(), brightnessAccess);
            }
            if (brightnessAccess.state.getInt(obj4) != 2) {
                return new Decision(null, "SUSPENDED: display off/dozing");
            }
            if (brightnessAccess.lowPower.getBoolean(obj4)) {
                return new Decision(null, "SUSPENDED: battery saver");
            }
            Object obj5 = this.votes.get(obj);
            if (obj5 == null) {
                return new Decision(null, "SUSPENDED: votes not ready");
            }
            Method method = this.voteMethods.get(obj5.getClass());
            if (method == null) {
                method = RefreshHook.method(obj5.getClass(), "getVotes", Integer.TYPE);
                this.voteMethods.put(obj5.getClass(), method);
            }
            Object objInvoke = method.invoke(obj5, 0);
            if (!(objInvoke instanceof SparseArray)) {
                throw new IllegalStateException("Unexpected vote snapshot");
            }
            SparseArray sparseArray = (SparseArray) objInvoke;
            for (int i = 0; i < sparseArray.size(); i++) {
                if (sparseArray.valueAt(i) != null) {
                    String strValueOf = String.valueOf(this.priorityName.invoke(null, Integer.valueOf(sparseArray.keyAt(i))));
                    if (RatePolicy.protectedVote(strValueOf) && !RatePolicy.compatibleLimit(strValueOf, sparseArray.valueAt(i))) {
                        return new Decision(null, "SUSPENDED: " + strValueOf);
                    }
                }
            }
            Object obj6 = this.modes.get(obj);
            Object obj7 = this.defaults.get(obj);
            if ((obj6 instanceof SparseArray) && (obj7 instanceof SparseArray)) {
                Object obj8 = ((SparseArray) obj6).get(0);
                Object obj9 = ((SparseArray) obj7).get(0);
                if (!(obj8 instanceof Object[]) || obj9 == null) {
                    return new Decision(null, "SUSPENDED: display modes not ready");
                }
                ModeAccess modeAccess2 = this.modeAccess.get(obj9.getClass());
                if (modeAccess2 == null) {
                    modeAccess2 = new ModeAccess(obj9.getClass());
                    this.modeAccess.put(obj9.getClass(), modeAccess2);
                }
                int iIntValue = ((Number) modeAccess2.width.invoke(obj9, new Object[0])).intValue();
                int iIntValue2 = ((Number) modeAccess2.height.invoke(obj9, new Object[0])).intValue();
                Object[] objArr = (Object[]) obj8;
                int length = objArr.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        obj3 = null;
                        modeAccess = null;
                        break;
                    }
                    obj3 = objArr[i2];
                    if (obj3 != null) {
                        ModeAccess modeAccess3 = this.modeAccess.get(obj3.getClass());
                        if (modeAccess3 != null) {
                            modeAccess = modeAccess3;
                        } else {
                            ModeAccess modeAccess4 = new ModeAccess(obj3.getClass());
                            this.modeAccess.put(obj3.getClass(), modeAccess4);
                            modeAccess = modeAccess4;
                        }
                        if (RatePolicy.isTargetMode(((Number) modeAccess.width.invoke(obj3, new Object[0])).intValue(), ((Number) modeAccess.height.invoke(obj3, new Object[0])).intValue(), ((Number) modeAccess.rate.invoke(obj3, new Object[0])).floatValue(), ((Number) modeAccess.vsync.invoke(obj3, new Object[0])).floatValue(), iIntValue, iIntValue2)) {
                            break;
                        }
                    }
                    i2++;
                }
                if (obj3 == null) {
                    return new Decision(null, "INACTIVE: no real 120 Hz mode at the native resolution");
                }
                int iIntValue3 = ((Number) modeAccess.id.invoke(obj3, new Object[0])).intValue();
                float fFloatValue = ((Number) modeAccess.rate.invoke(obj3, new Object[0])).floatValue();
                if (iIntValue3 > 0) {
                    Object objNewInstance = this.constructor.newInstance(new Object[0]);
                    for (Field field : this.scalarFields) {
                        field.set(objNewInstance, field.get(obj2));
                    }
                    Object obj10 = this.primary.get(objNewInstance);
                    Object obj11 = this.appRequest.get(objNewInstance);
                    if (obj10 == null || obj11 == null || obj10 == this.primary.get(obj2) || obj11 == this.appRequest.get(obj2)) {
                        throw new IllegalStateException("Fresh specs did not own their ranges");
                    }
                    pinRanges(obj10, fFloatValue);
                    pinRanges(obj11, fFloatValue);
                    this.baseModeId.setInt(objNewInstance, iIntValue3);
                    this.groupSwitch.setBoolean(objNewInstance, false);
                    if (this.idle != null) {
                        this.idle.set(objNewInstance, null);
                    }
                    return new Decision(objNewInstance, "ACTIVE: real mode=" + iIntValue3 + " rate=" + fFloatValue + " Hz; primary/app physical+render pinned; awake display only");
                }
                throw new IllegalStateException("Invalid real mode ID");
            }
            throw new IllegalStateException("Unexpected display mode storage");
        }

        void pinRanges(Object obj, float f) throws Exception {
            RangeAccess rangeAccess = this.ranges.get(obj.getClass());
            if (rangeAccess == null) {
                rangeAccess = new RangeAccess(obj.getClass());
                this.ranges.put(obj.getClass(), rangeAccess);
            }
            rangeAccess.pin(obj, f);
        }
    }

    private static final class ModeAccess {
        final Method height;
        final Method id;
        final Method rate;
        final Method vsync;
        final Method width;

        ModeAccess(Class<?> cls) throws Exception {
            this.id = RefreshHook.method(cls, "getModeId", new Class[0]);
            this.width = RefreshHook.method(cls, "getPhysicalWidth", new Class[0]);
            this.height = RefreshHook.method(cls, "getPhysicalHeight", new Class[0]);
            this.rate = RefreshHook.method(cls, "getRefreshRate", new Class[0]);
            this.vsync = RefreshHook.method(cls, "getVsyncRate", new Class[0]);
        }
    }

    private static final class BrightnessAccess {
        final Field lowPower;
        final Field state;

        BrightnessAccess(Class<?> cls) throws Exception {
            this.state = RefreshHook.field(cls, "mDefaultDisplayState");
            this.lowPower = RefreshHook.field(cls, "mLowPowerModeEnabled");
        }
    }

    private static final class RangeAccess {
        final Map<Class<?>, PairAccess> pairs = new HashMap();
        final Field physical;
        final Field render;

        RangeAccess(Class<?> cls) throws Exception {
            this.physical = RefreshHook.field(cls, "physical");
            this.render = RefreshHook.field(cls, "render");
        }

        private void pinPair(Object obj, float f) throws Exception {
            if (obj == null) {
                throw new IllegalStateException("Missing refresh range");
            }
            PairAccess pairAccess = this.pairs.get(obj.getClass());
            if (pairAccess == null) {
                pairAccess = new PairAccess(obj.getClass());
                this.pairs.put(obj.getClass(), pairAccess);
            }
            pairAccess.minimum.setFloat(obj, f);
            pairAccess.maximum.setFloat(obj, f);
        }

        void pin(Object obj, float f) throws Exception {
            pinPair(this.physical.get(obj), f);
            pinPair(this.render.get(obj), f);
        }
    }

    private static final class PairAccess {
        final Field maximum;
        final Field minimum;

        PairAccess(Class<?> cls) throws Exception {
            this.minimum = RefreshHook.field(cls, "min");
            this.maximum = RefreshHook.field(cls, "max");
            if (this.minimum.getType() != Float.TYPE || this.maximum.getType() != Float.TYPE) {
                throw new NoSuchFieldException("Refresh range pair");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Field field(Class<?> cls, String str) throws NoSuchFieldException {
        for (Class<?> superclass = cls; superclass != null; superclass = superclass.getSuperclass()) {
            try {
                Field declaredField = superclass.getDeclaredField(str);
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException e) {
            }
        }
        throw new NoSuchFieldException(cls.getName() + "." + str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Field optionalField(Class<?> cls, String str) {
        try {
            return field(cls, str);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Method method(Class<?> cls, String str, Class<?>... clsArr) throws NoSuchMethodException {
        for (Class<?> superclass = cls; superclass != null; superclass = superclass.getSuperclass()) {
            try {
                Method declaredMethod = superclass.getDeclaredMethod(str, clsArr);
                declaredMethod.setAccessible(true);
                return declaredMethod;
            } catch (NoSuchMethodException e) {
            }
        }
        throw new NoSuchMethodException(cls.getName() + "." + str);
    }
}
