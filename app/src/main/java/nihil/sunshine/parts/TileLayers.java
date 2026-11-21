package nihil.sunshine.parts;

import android.os.Looper;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
final class TileLayers {
    static long firstOwners;
    static long placements;
    private static final Object TOKEN = new Object();
    private static final HotScope scope = new HotScope();
    private static final WeakHashMap<Object, Boolean> owners = new WeakHashMap<>();
    static String error = "";

    static XC_MethodHook scopeHook(final Field field) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.TileLayers.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object obj = TileLayers.TOKEN;
                try {
                    Object obj2 = field.get(methodHookParam.thisObject);
                    if (obj2 != null) {
                        obj = obj2;
                    }
                } catch (Throwable th) {
                }
                TileLayers.scope.enter(obj);
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                TileLayers.scope.leave();
            }
        };
    }

    static XC_MethodHook layerHook(final Class<?>[] clsArr, final Object obj) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.TileLayers.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (Config.tileLayers && TileLayers.scope.active() && Config.leanQs && AdaptiveWorkload.level() != 0 && Looper.myLooper() == Looper.getMainLooper() && methodHookParam.thisObject != null && TileLayers.known(methodHookParam.thisObject.getClass(), clsArr) && TileLayers.scope.is(methodHookParam.thisObject) && methodHookParam.args.length == 4 && methodHookParam.args[2] == null && methodHookParam.args[3] == null && (methodHookParam.args[1] instanceof Float) && Float.floatToIntBits(((Float) methodHookParam.args[1]).floatValue()) == 0) {
                    if (!TileLayers.owners.containsKey(methodHookParam.thisObject)) {
                        if (TileLayers.owners.size() >= 64) {
                            return;
                        }
                        TileLayers.owners.put(methodHookParam.thisObject, Boolean.TRUE);
                        TileLayers.firstOwners++;
                    }
                    methodHookParam.args[2] = obj;
                    TileLayers.placements++;
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean known(Class<?> cls, Class<?>[] clsArr) {
        for (Class<?> cls2 : clsArr) {
            if (cls == cls2) {
                return true;
            }
        }
        return false;
    }

    static void install(ClassLoader classLoader, String str) {
        ArrayList arrayList = new ArrayList();
        try {
            if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
                throw new IllegalStateException("Unrecognized SystemUI DEX");
            }
            Class<?> cls = Class.forName("com.android.systemui.samsung.quicksetting.ui.base.spannablegrid.SpannableGridScopeImpl$$ExternalSyntheticLambda9", false, classLoader);
            Class<?> cls2 = Class.forName("androidx.compose.ui.layout.Placeable", false, classLoader);
            Class<?> cls3 = Class.forName("androidx.compose.ui.node.MeasurePassDelegate", false, classLoader);
            Class<?> cls4 = Class.forName("kotlin.jvm.functions.Function1", false, classLoader);
            Class<?> cls5 = Class.forName("androidx.compose.ui.graphics.layer.GraphicsLayer", false, classLoader);
            Field declaredField = cls.getDeclaredField("f$0");
            declaredField.setAccessible(true);
            if (declaredField.getType() != cls2 || !cls2.isAssignableFrom(cls3)) {
                throw new NoSuchFieldException("QS placeable ABI");
            }
            Field declaredField2 = Class.forName("androidx.compose.ui.layout.PlaceableKt", false, classLoader).getDeclaredField("DefaultLayerBlock");
            declaredField2.setAccessible(true);
            Object obj = declaredField2.get(null);
            if (!cls4.isInstance(obj)) {
                throw new IllegalStateException("Native layer block");
            }
            Method declaredMethod = cls.getDeclaredMethod("invoke", Object.class);
            Method declaredMethod2 = cls3.getDeclaredMethod("placeSelf-MLgxB_4$2", Long.TYPE, Float.TYPE, cls4, cls5);
            if (declaredMethod2.getReturnType() != Void.TYPE || Modifier.isStatic(declaredMethod2.getModifiers())) {
                throw new NoSuchMethodException("Placement ABI");
            }
            XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(declaredMethod2, layerHook(new Class[]{cls3}, obj));
            if (unhookHookMethod == null) {
                throw new IllegalStateException("Placement hook");
            }
            arrayList.add(unhookHookMethod);
            if (RuntimeHooks.hookMethod(declaredMethod, scopeHook(declaredField)) != null) {
                ControlBus.feature("tile_graphics_layers", "Scoped drawing layers with native coordinator forwarding");
                return;
            }
            throw new IllegalStateException("Grid scope hook");
        } catch (Throwable th) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                RuntimeHooks.unhook((XC_MethodHook.Unhook) it.next());
            }
            error = th.getClass().getSimpleName();
            ControlBus.feature("tile_graphics_layers", "Native fallback");
        }
    }

    private TileLayers() {
    }
}
