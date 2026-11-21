package nihil.sunshine.parts;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class TransitionEffects {
    private static int appliedMode;
    private static Binding binding;
    private static final Float ZERO = Float.valueOf(0.0f);
    private static final ThreadLocal<Boolean> restoring = new ThreadLocal<>();
    private static final Tracker tracker = new Tracker();
    private static final List<XC_MethodHook.Unhook> installed = new ArrayList();

    interface Restorer {
        void restore(List<State> list);
    }

    static final class State {
        boolean changed;
        final int mode;
        int owners;
        float requested = Float.NaN;
        volatile boolean retired;
        final Object surface;

        State(Object obj, int i) {
            this.surface = obj;
            this.mode = i;
        }
    }

    static final class Cycle {
        final int mode;
        final List<State> states = new ArrayList();

        Cycle(int i) {
            this.mode = i;
        }
    }

    static final class Tracker {
        Restorer restorer;
        final IdentityHashMap<Object, Cycle> cycles = new IdentityHashMap<>();
        final IdentityHashMap<Object, State> surfaces = new IdentityHashMap<>();

        Tracker() {
        }

        synchronized void add(Object obj, Collection<?> collection, int i) {
            if (obj != null && i >= 1 && i <= 2 && collection != null) {
                if (!collection.isEmpty()) {
                    Cycle cycle = this.cycles.get(obj);
                    if (cycle == null) {
                        if (this.cycles.size() >= 32) {
                            return;
                        }
                        cycle = new Cycle(i);
                        this.cycles.put(obj, cycle);
                    }
                    for (Object obj2 : collection) {
                        if (obj2 != null && cycle.states.size() < 128) {
                            State state = this.surfaces.get(obj2);
                            if (state == null) {
                                if (this.surfaces.size() < 256) {
                                    state = new State(obj2, cycle.mode);
                                    this.surfaces.put(obj2, state);
                                }
                            }
                            if (!cycle.states.contains(state)) {
                                cycle.states.add(state);
                                state.owners++;
                            }
                        }
                    }
                }
            }
        }

        synchronized State radius(Object obj, int i, float f) {
            State state = this.surfaces.get(obj);
            if (state != null && !state.retired && state.mode == i && f >= 0.0f && Float.isFinite(f)) {
                state.requested = f;
                state.changed = (f != 0.0f) | state.changed;
                return state;
            }
            return null;
        }

        void finish(Object obj) {
            ArrayList arrayList = new ArrayList();
            synchronized (this) {
                Cycle cycleRemove = this.cycles.remove(obj);
                if (cycleRemove == null) {
                    return;
                }
                for (State state : cycleRemove.states) {
                    int i = state.owners - 1;
                    state.owners = i;
                    if (i == 0) {
                        this.surfaces.remove(state.surface);
                        state.retired = true;
                        arrayList.add(state);
                    }
                }
                restore(arrayList);
            }
        }

        synchronized void merge(Object obj, Object obj2) {
            if (obj == obj2 || obj2 == null) {
                return;
            }
            Cycle cycleRemove = this.cycles.remove(obj);
            if (cycleRemove == null) {
                return;
            }
            Cycle cycle = this.cycles.get(obj2);
            if (cycle == null) {
                this.cycles.put(obj2, cycleRemove);
                return;
            }
            for (State state : cycleRemove.states) {
                if (cycle.states.contains(state)) {
                    state.owners--;
                } else {
                    cycle.states.add(state);
                }
            }
        }

        void clear() {
            ArrayList arrayList;
            synchronized (this) {
                arrayList = new ArrayList(this.surfaces.values());
                Iterator<State> it = arrayList.iterator();
                while (it.hasNext()) {
                    it.next().retired = true;
                }
                this.surfaces.clear();
                this.cycles.clear();
            }
            restore(arrayList);
        }

        private void restore(List<State> list) {
            if (this.restorer == null || list.isEmpty()) {
                return;
            }
            this.restorer.restore(list);
        }
    }

    static XC_MethodHook radiusHook(final Tracker tracker2, final int i) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.TransitionEffects.1
            private final String extra;

            {
                this.extra = "sunshine.transition.radius." + i;
            }

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                State stateRadius;
                if (!Boolean.TRUE.equals(TransitionEffects.restoring.get()) && (stateRadius = tracker2.radius(methodHookParam.args[0], i, ((Float) methodHookParam.args[1]).floatValue())) != null) {
                    methodHookParam.setObjectExtra(this.extra, stateRadius);
                    methodHookParam.args[1] = TransitionEffects.ZERO;
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra(this.extra);
                if (objectExtra instanceof State) {
                    State state = (State) objectExtra;
                    if (state.retired && !methodHookParam.hasThrowable()) {
                        TransitionEffects.restoring.set(Boolean.TRUE);
                        try {
                            ((Method) methodHookParam.method).invoke(methodHookParam.thisObject, methodHookParam.args[0], Float.valueOf(state.requested));
                        } catch (Throwable th) {
                        }
                        TransitionEffects.restoring.remove();
                    }
                }
            }
        };
    }

    private static final class NativeAccess implements Restorer {
        final Method apply;
        final Method close;
        final Method corner;
        final Constructor<?> create;
        final Method shadow;
        final Class<?> surface;
        final Class<?> transaction;
        final Method valid;

        NativeAccess(ClassLoader classLoader) throws Exception {
            this.surface = Class.forName("android.view.SurfaceControl", false, classLoader);
            this.transaction = Class.forName("android.view.SurfaceControl$Transaction", false, classLoader);
            this.create = this.transaction.getDeclaredConstructor(new Class[0]);
            this.create.setAccessible(true);
            this.valid = this.surface.getDeclaredMethod("isValid", new Class[0]);
            this.shadow = this.transaction.getDeclaredMethod("setShadowRadius", this.surface, Float.TYPE);
            this.corner = this.transaction.getDeclaredMethod("setCornerRadius", this.surface, Float.TYPE);
            this.apply = this.transaction.getDeclaredMethod("apply", new Class[0]);
            this.close = this.transaction.getDeclaredMethod("close", new Class[0]);
            Method[] methodArr = {this.valid, this.shadow, this.corner, this.apply, this.close};
            for (int i = 0; i < 5; i++) {
                methodArr[i].setAccessible(true);
            }
            if (this.shadow.getReturnType() != this.transaction || this.corner.getReturnType() != this.transaction) {
                throw new NoSuchMethodException("Native transaction ABI");
            }
        }

        @Override // nihil.sunshine.parts.TransitionEffects.Restorer
        public void restore(List<State> list) {
            TransitionEffects.restoring.set(Boolean.TRUE);
            Object objNewInstance = null;
            try {
                boolean z = false;
                for (State state : list) {
                    if (state.changed && Float.isFinite(state.requested)) {
                        try {
                            if (Boolean.TRUE.equals(this.valid.invoke(state.surface, new Object[0]))) {
                                if (objNewInstance == null) {
                                    objNewInstance = this.create.newInstance(new Object[0]);
                                }
                                (state.mode == 1 ? this.shadow : this.corner).invoke(objNewInstance, state.surface, Float.valueOf(state.requested));
                                z = true;
                            }
                        } catch (Throwable th) {
                        }
                    }
                }
                if (z) {
                    this.apply.invoke(objNewInstance, new Object[0]);
                }
                if (objNewInstance != null) {
                    try {
                        this.close.invoke(objNewInstance, new Object[0]);
                    } catch (Throwable th2) {
                    }
                }
            } catch (Throwable th3) {
                if (objNewInstance != null) {
                    try {
                        this.close.invoke(objNewInstance, new Object[0]);
                    } catch (Throwable th4) {
                    }
                }
            }
            TransitionEffects.restoring.remove();
        }
    }

    private static final class Plan {
        final XC_MethodHook hook;
        final Method method;

        Plan(Method method, XC_MethodHook xC_MethodHook) {
            this.method = method;
            this.hook = xC_MethodHook;
            method.setAccessible(true);
        }
    }

    private static final class Binding {
        final NativeAccess nativeAccess;
        final List<Plan> plans = new ArrayList();

        Binding(ClassLoader classLoader) throws Exception {
            this.nativeAccess = new NativeAccess(classLoader);
        }

        void add(Method method, XC_MethodHook xC_MethodHook) {
            this.plans.add(new Plan(method, xC_MethodHook));
        }
    }

    private static Class<?> type(ClassLoader classLoader, String str) throws ClassNotFoundException {
        return Class.forName(str, false, classLoader);
    }

    private static Field field(Class<?> cls, String str, Class<?> cls2) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        if (declaredField.getType() == cls2) {
            return declaredField;
        }
        throw new NoSuchFieldException(str);
    }

    private static boolean pinned(String str, boolean z) {
        return z ? NativeTargets.launcher(str) : NativeTargets.systemUi(str);
    }

    public static synchronized void reload() {
        int i = RuntimeProfile.recovery() ? 0 : Config.transitionEffects;
        if (binding != null && i != appliedMode) {
            tracker.clear();
            if (i == 0) {
                Iterator<XC_MethodHook.Unhook> it = installed.iterator();
                while (it.hasNext()) {
                    RuntimeHooks.unhook(it.next());
                }
                installed.clear();
                appliedMode = 0;
                return;
            }
            if (installed.isEmpty()) {
                try {
                    for (Plan plan : binding.plans) {
                        XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(plan.method, plan.hook);
                        if (unhookHookMethod == null) {
                            throw new IllegalStateException("Transition hook unavailable");
                        }
                        installed.add(unhookHookMethod);
                    }
                } catch (Throwable th) {
                    Iterator<XC_MethodHook.Unhook> it2 = installed.iterator();
                    while (it2.hasNext()) {
                        RuntimeHooks.unhook(it2.next());
                    }
                    installed.clear();
                    return;
                }
            }
            appliedMode = i;
        }
    }

    private static synchronized void ready(Binding binding2) {
        if (binding != null) {
            return;
        }
        binding = binding2;
        tracker.restorer = binding2.nativeAccess;
        reload();
    }

    public static void installShell(ClassLoader classLoader, String str) {
        try {
            if (pinned(str, false)) {
                Binding binding2 = new Binding(classLoader);
                Class<?> clsType = type(classLoader, "com.android.wm.shell.transition.Transitions");
                Class<?> clsType2 = type(classLoader, "android.os.IBinder");
                Class<?> clsType3 = type(classLoader, "android.window.TransitionInfo");
                Class<?> clsType4 = type(classLoader, "android.window.TransitionInfo$Change");
                Class<?> clsType5 = type(classLoader, "android.app.ActivityManager$RunningTaskInfo");
                Class<?> clsType6 = type(classLoader, clsType.getName() + "$ActiveTransition");
                Class<?> clsType7 = type(classLoader, clsType.getName() + "$TransitionHandler");
                Class<?> clsType8 = type(classLoader, clsType.getName() + "$TransitionFinishCallback");
                final Method declaredMethod = clsType3.getDeclaredMethod("getChanges", new Class[0]);
                final Method declaredMethod2 = clsType4.getDeclaredMethod("getTaskInfo", new Class[0]);
                final Method declaredMethod3 = clsType4.getDeclaredMethod("getLeash", new Class[0]);
                final Field declaredField = type(classLoader, "android.app.TaskInfo").getDeclaredField("taskId");
                final Field field = field(clsType6, "mToken", clsType2);
                if (declaredMethod.getReturnType() != List.class || declaredMethod2.getReturnType() != clsType5 || declaredMethod3.getReturnType() != binding2.nativeAccess.surface || declaredField.getType() != Integer.TYPE) {
                    throw new NoSuchMethodException("Transition target ABI");
                }
                Method[] methodArr = {declaredMethod, declaredMethod2, declaredMethod3};
                int i = 0;
                while (i < 3) {
                    methodArr[i].setAccessible(true);
                    i++;
                    methodArr = methodArr;
                }
                declaredField.setAccessible(true);
                Method declaredMethod4 = clsType.getDeclaredMethod("dispatchTransition", clsType2, clsType3, binding2.nativeAccess.transaction, binding2.nativeAccess.transaction, clsType8, clsType7, clsType7);
                if (declaredMethod4.getReturnType() != clsType7) {
                    throw new NoSuchMethodException("Transition dispatch ABI");
                }
                binding2.add(declaredMethod4, new XC_MethodHook() { // from class: nihil.sunshine.parts.TransitionEffects.2
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (Config.transitionEffects == 0) {
                            return;
                        }
                        try {
                            ArrayList arrayList = new ArrayList();
                            for (Object obj : (List) declaredMethod.invoke(methodHookParam.args[1], new Object[0])) {
                                Object objInvoke = declaredMethod2.invoke(obj, new Object[0]);
                                if (objInvoke != null && declaredField.getInt(objInvoke) >= 0) {
                                    arrayList.add(declaredMethod3.invoke(obj, new Object[0]));
                                }
                            }
                            TransitionEffects.tracker.add(methodHookParam.args[0], arrayList, Config.transitionEffects);
                        } catch (Throwable th) {
                        }
                    }

                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (methodHookParam.hasThrowable() || methodHookParam.getResult() == null) {
                            TransitionEffects.tracker.finish(methodHookParam.args[0]);
                        }
                    }
                });
                binding2.add(clsType.getDeclaredMethod("onFinish", clsType2, type(classLoader, "android.window.WindowContainerTransaction")), new XC_MethodHook() { // from class: nihil.sunshine.parts.TransitionEffects.3
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        TransitionEffects.tracker.finish(methodHookParam.args[0]);
                    }
                });
                binding2.add(clsType.getDeclaredMethod("onAbort", clsType6), new XC_MethodHook() { // from class: nihil.sunshine.parts.TransitionEffects.4
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        try {
                            TransitionEffects.tracker.finish(field.get(methodHookParam.args[0]));
                        } catch (Throwable th) {
                        }
                    }
                });
                final Field declaredField2 = clsType.getDeclaredField("mKnownTransitions");
                final Field field2 = field(clsType6, "mMerged", ArrayList.class);
                declaredField2.setAccessible(true);
                if (!Map.class.isAssignableFrom(declaredField2.getType())) {
                    throw new NoSuchFieldException("Known transition map");
                }
                binding2.add(clsType.getDeclaredMethod("onMerged", clsType2, clsType2), new XC_MethodHook() { // from class: nihil.sunshine.parts.TransitionEffects.5
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        try {
                            Map map = (Map) declaredField2.get(methodHookParam.thisObject);
                            Object obj = map.get(methodHookParam.args[1]);
                            if (obj == null || map.get(methodHookParam.args[0]) == null) {
                                return;
                            }
                            methodHookParam.setObjectExtra("sunshine.transition.merge", obj);
                        } catch (Throwable th) {
                        }
                    }

                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object objectExtra = methodHookParam.getObjectExtra("sunshine.transition.merge");
                        if (objectExtra == null || methodHookParam.hasThrowable()) {
                            return;
                        }
                        try {
                            Object obj = ((Map) declaredField2.get(methodHookParam.thisObject)).get(methodHookParam.args[0]);
                            if (obj == null) {
                                TransitionEffects.tracker.finish(methodHookParam.args[1]);
                                return;
                            }
                            List list = (List) field2.get(obj);
                            if (list != null && list.contains(objectExtra)) {
                                TransitionEffects.tracker.merge(methodHookParam.args[1], methodHookParam.args[0]);
                            }
                        } catch (Throwable th) {
                        }
                    }
                });
                binding2.add(binding2.nativeAccess.shadow, radiusHook(tracker, 1));
                binding2.add(binding2.nativeAccess.corner, radiusHook(tracker, 2));
                ready(binding2);
            }
        } catch (Throwable th) {
        }
    }

    public static void installLauncher(ClassLoader classLoader, String str) {
        try {
            if (pinned(str, true)) {
                Binding binding2 = new Binding(classLoader);
                Class<?> clsType = type(classLoader, "com.honeyspace.transition.remote.RemoteAnimationTargets");
                Class<?> clsType2 = type(classLoader, "android.view.RemoteAnimationTarget");
                Class<?> cls = Array.newInstance(clsType2, 0).getClass();
                final Field field = field(clsType2, "leash", binding2.nativeAccess.surface);
                final Field field2 = field(clsType2, "taskId", Integer.TYPE);
                final Field field3 = field(clsType, "released", Boolean.TYPE);
                XC_MethodHook xC_MethodHook = new XC_MethodHook() { // from class: nihil.sunshine.parts.TransitionEffects.6
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (methodHookParam.hasThrowable() || Config.transitionEffects == 0) {
                            return;
                        }
                        try {
                            if (field3.getBoolean(methodHookParam.thisObject)) {
                                return;
                            }
                            Object obj = methodHookParam.args[0];
                            ArrayList arrayList = new ArrayList();
                            if (obj instanceof List) {
                                Iterator it = ((List) obj).iterator();
                                while (it.hasNext()) {
                                    append(it.next(), arrayList);
                                }
                            } else if (obj == null || !obj.getClass().isArray()) {
                                append(obj, arrayList);
                            } else {
                                for (int i = 0; i < Array.getLength(obj); i++) {
                                    append(Array.get(obj, i), arrayList);
                                }
                            }
                            TransitionEffects.tracker.add(methodHookParam.thisObject, arrayList, Config.transitionEffects);
                        } catch (Throwable th) {
                        }
                    }

                    private void append(Object obj, List<Object> list) throws Exception {
                        if (obj == null || field2.getInt(obj) < 0) {
                            return;
                        }
                        list.add(field.get(obj));
                    }
                };
                binding2.add(clsType.getDeclaredMethod("setUnfilteredApps", cls), xC_MethodHook);
                binding2.add(clsType.getDeclaredMethod("setApps", cls), xC_MethodHook);
                binding2.add(clsType.getDeclaredMethod("setLeashOverlayTarget", clsType2), xC_MethodHook);
                binding2.add(clsType.getDeclaredMethod("release", cls), new XC_MethodHook() { // from class: nihil.sunshine.parts.TransitionEffects.7
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        TransitionEffects.tracker.finish(methodHookParam.thisObject);
                    }
                });
                Class<?> clsType3 = type(classLoader, "com.android.systemui.shared.launcher.ScTransactionCompat");
                binding2.add(clsType3.getDeclaredMethod("setShadowRadius", binding2.nativeAccess.surface, Float.TYPE), radiusHook(tracker, 1));
                binding2.add(clsType3.getDeclaredMethod("setCornerRadius", binding2.nativeAccess.surface, Float.TYPE), radiusHook(tracker, 2));
                binding2.add(type(classLoader, "com.android.systemui.shared.launcher.TransactionCompat").getDeclaredMethod("setCornerRadius", binding2.nativeAccess.surface, Float.TYPE), radiusHook(tracker, 2));
                ready(binding2);
            }
        } catch (Throwable th) {
        }
    }

    private TransitionEffects() {
    }
}
