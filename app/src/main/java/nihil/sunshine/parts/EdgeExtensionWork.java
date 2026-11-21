package nihil.sunshine.parts;

import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class EdgeExtensionWork {
    private static final String EXTRA = "sunshine.edge.scope";
    private static boolean attempted;
    private static long nextId;
    private static int recentCount;
    private static int recentNext;
    private static long requests;
    private static volatile boolean supported;
    private static long transitions;
    private static final ThreadLocal<Scope> local = new ThreadLocal<>();
    private static final Object lock = new Object();
    private static volatile String error = "";
    private static final long[][] recent = (long[][]) Array.newInstance((Class<?>) Long.TYPE, 8, 3);

    static /* synthetic */ long access$304() {
        long j = nextId + 1;
        nextId = j;
        return j;
    }

    static /* synthetic */ long access$408() {
        long j = transitions;
        transitions = 1 + j;
        return j;
    }

    static /* synthetic */ long access$514(long j) {
        long j2 = requests + j;
        requests = j2;
        return j2;
    }

    private static final class Scope {
        long adjusted;
        final boolean enabled;
        long id;
        final IdentityHashMap<Object, Boolean> leashes = new IdentityHashMap<>();
        final Scope previous;

        Scope(Scope scope, boolean z) {
            this.previous = scope;
            this.enabled = z;
        }
    }

    private static boolean pinned(String str) {
        return NativeTargets.systemUi(str);
    }

    private static Class<?> type(ClassLoader classLoader, String str) throws ClassNotFoundException {
        return Class.forName(str, false, classLoader);
    }

    static synchronized void install(ClassLoader classLoader, String str) {
        if (attempted) {
            return;
        }
        attempted = true;
        ArrayList arrayList = new ArrayList();
        try {
            if (RuntimeProfile.recovery()) {
                throw new IllegalStateException("recovery");
            }
            if (!pinned(str)) {
                throw new IllegalStateException("unsupported_system_ui");
            }
            Class<?> clsType = type(classLoader, "com.android.wm.shell.transition.DefaultTransitionHandler");
            Class<?> clsType2 = type(classLoader, "android.view.SurfaceControl");
            Class<?> clsType3 = type(classLoader, "android.view.SurfaceControl$Transaction");
            Class<?> clsType4 = type(classLoader, "android.window.TransitionInfo");
            Class<?> clsType5 = type(classLoader, "android.window.TransitionInfo$Change");
            Class<?> clsType6 = type(classLoader, "android.os.IBinder");
            Class<?> clsType7 = type(classLoader, "com.android.wm.shell.transition.Transitions$TransitionFinishCallback");
            final Method declaredMethod = clsType4.getDeclaredMethod("getChanges", new Class[0]);
            final Method declaredMethod2 = clsType5.getDeclaredMethod("getLeash", new Class[0]);
            Method declaredMethod3 = clsType.getDeclaredMethod("startAnimation", clsType6, clsType4, clsType3, clsType3, clsType7);
            Method declaredMethod4 = clsType3.getDeclaredMethod("setEdgeExtensionEffect", clsType2, Integer.TYPE);
            if (declaredMethod3.getReturnType() != Boolean.TYPE || Modifier.isStatic(declaredMethod3.getModifiers()) || declaredMethod.getReturnType() != List.class || declaredMethod2.getReturnType() != clsType2 || declaredMethod4.getReturnType() != clsType3 || Modifier.isStatic(declaredMethod4.getModifiers())) {
                throw new NoSuchMethodException("edge_extension_abi");
            }
            Method[] methodArr = {declaredMethod3, declaredMethod4, declaredMethod, declaredMethod2};
            for (int i = 0; i < 4; i++) {
                methodArr[i].setAccessible(true);
            }
            Object[][] objArr = {new Object[]{declaredMethod4, new XC_MethodHook() { // from class: nihil.sunshine.parts.EdgeExtensionWork.2
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Scope scope = (Scope) EdgeExtensionWork.local.get();
                    if (scope == null || !scope.enabled || ((Integer) methodHookParam.args[1]).intValue() == 0 || !scope.leashes.containsKey(methodHookParam.args[0])) {
                        return;
                    }
                    methodHookParam.args[1] = 0;
                    methodHookParam.setObjectExtra(EdgeExtensionWork.EXTRA, scope);
                }

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objectExtra = methodHookParam.getObjectExtra(EdgeExtensionWork.EXTRA);
                    if (!(objectExtra instanceof Scope) || methodHookParam.hasThrowable()) {
                        return;
                    }
                    ((Scope) objectExtra).adjusted++;
                }
            }}, new Object[]{declaredMethod3, new XC_MethodHook() { // from class: nihil.sunshine.parts.EdgeExtensionWork.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Scope scope = new Scope((Scope) EdgeExtensionWork.local.get(), Config.reduceEdgeExtension && !RuntimeProfile.recovery());
                    if (scope.enabled) {
                        try {
                            List list = (List) declaredMethod.invoke(methodHookParam.args[1], new Object[0]);
                            if (list.size() > 4096) {
                                throw new IllegalStateException("too_many_changes");
                            }
                            Iterator it = list.iterator();
                            while (it.hasNext()) {
                                Object objInvoke = declaredMethod2.invoke(it.next(), new Object[0]);
                                if (objInvoke != null) {
                                    scope.leashes.put(objInvoke, Boolean.TRUE);
                                }
                            }
                        } catch (Throwable th) {
                            scope.leashes.clear();
                            String unused = EdgeExtensionWork.error = "scope_unavailable";
                        }
                    }
                    synchronized (EdgeExtensionWork.lock) {
                        scope.id = EdgeExtensionWork.access$304();
                    }
                    methodHookParam.setObjectExtra(EdgeExtensionWork.EXTRA, scope);
                    EdgeExtensionWork.local.set(scope);
                }

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objectExtra = methodHookParam.getObjectExtra(EdgeExtensionWork.EXTRA);
                    if (objectExtra instanceof Scope) {
                        Scope scope = (Scope) objectExtra;
                        if (scope.previous == null) {
                            EdgeExtensionWork.local.remove();
                        } else {
                            EdgeExtensionWork.local.set(scope.previous);
                        }
                        if (scope.adjusted > 0) {
                            synchronized (EdgeExtensionWork.lock) {
                                EdgeExtensionWork.access$408();
                                EdgeExtensionWork.access$514(scope.adjusted);
                                long[] jArr = EdgeExtensionWork.recent[EdgeExtensionWork.recentNext];
                                jArr[0] = scope.id;
                                jArr[1] = scope.adjusted;
                                jArr[2] = methodHookParam.hasThrowable() ? 1L : 0L;
                                int unused = EdgeExtensionWork.recentNext = (EdgeExtensionWork.recentNext + 1) % EdgeExtensionWork.recent.length;
                                int unused2 = EdgeExtensionWork.recentCount = Math.min(EdgeExtensionWork.recentCount + 1, EdgeExtensionWork.recent.length);
                            }
                        }
                    }
                }
            }}};
            for (int i2 = 0; i2 < 2; i2++) {
                Object[] objArr2 = objArr[i2];
                XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod((Method) objArr2[0], (XC_MethodHook) objArr2[1]);
                if (unhookHookMethod != null) {
                    arrayList.add(unhookHookMethod);
                } else {
                    throw new IllegalStateException("hook_unavailable");
                }
            }
            supported = true;
            error = "";
            Log.i("SunshinePartsUI", "Edge extension: exact Shell runner and transaction ABI resolved");
        } catch (Throwable th) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                RuntimeHooks.unhook((XC_MethodHook.Unhook) it.next());
            }
            error = th.getMessage() == null ? th.getClass().getSimpleName() : th.getMessage();
            Log.w("SunshinePartsUI", "Edge extension unavailable: " + error);
        }
    }

    static JSONObject status() throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("supported", supported);
        jSONObject.put("enabled", Config.reduceEdgeExtension && !RuntimeProfile.recovery());
        jSONObject.put("error", error);
        synchronized (lock) {
            jSONObject.put("adjusted_requests", requests);
            jSONObject.put("adjusted_transitions", transitions);
            JSONArray jSONArray = new JSONArray();
            for (int i = 0; i < recentCount; i++) {
                long[] jArr = recent[(((recentNext - recentCount) + i) + recent.length) % recent.length];
                JSONObject jSONObject2 = new JSONObject();
                jSONObject2.put("call", jArr[0]);
                jSONObject2.put("requests", jArr[1]);
                jSONObject2.put("threw", jArr[2] != 0);
                jSONArray.put(jSONObject2);
            }
            jSONObject.put("recent_transitions", jSONArray);
        }
        return jSONObject;
    }

    private EdgeExtensionWork() {
    }
}
