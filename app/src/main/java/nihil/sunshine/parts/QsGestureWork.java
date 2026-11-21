package nihil.sunshine.parts;

import android.os.Looper;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class QsGestureWork {
    static String error = "";
    static long updates;

    static XC_MethodHook hook(final Class<?> cls, final Field field, final Field field2, final String str, final boolean z) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.QsGestureWork.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object obj;
                if (!VisualPolicy.leanQs() || AdaptiveWorkload.level() == 0 || Looper.myLooper() != Looper.getMainLooper() || methodHookParam.thisObject == null || methodHookParam.thisObject.getClass() != cls || methodHookParam.args.length != 1) {
                    return;
                }
                try {
                    Object obj2 = field.get(methodHookParam.thisObject);
                    if (obj2 == null || (obj = field2.get(obj2)) == null || obj.getClass() != LinkedHashMap.class) {
                        return;
                    }
                    Map map = (Map) obj;
                    if (z) {
                        map.remove(methodHookParam.args[0]);
                    } else {
                        map.put(str, methodHookParam.args[0]);
                    }
                    QsGestureWork.updates++;
                    methodHookParam.setResult((Object) null);
                } catch (Throwable th) {
                    QsGestureWork.error = th.getClass().getSimpleName();
                }
            }
        };
    }

    static void install(ClassLoader classLoader, String str) {
        ArrayList arrayList = new ArrayList();
        try {
            if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
                throw new IllegalStateException("Unrecognized SystemUI DEX");
            }
            if (NativeTargets.systemUi(str)) {
                ControlBus.feature("qs_gesture_work", "Native path");
                return;
            }
            Class<?> cls = Class.forName("com.android.systemui.samsung.quicksetting.ui.panel.SwipeToCollapseViewModelImpl", false, classLoader);
            Class<?> cls2 = Class.forName("androidx.compose.ui.geometry.Rect", false, classLoader);
            Field declaredField = cls.getDeclaredField("composeAdapter");
            declaredField.setAccessible(true);
            Field declaredField2 = declaredField.getType().getDeclaredField("collapsibleAreas");
            Field declaredField3 = declaredField.getType().getDeclaredField("nonCollapsibleAreas");
            declaredField2.setAccessible(true);
            declaredField3.setAccessible(true);
            if (declaredField2.getType() != Map.class || declaredField3.getType() != Map.class) {
                throw new NoSuchFieldException("Gesture map ABI");
            }
            String[] strArr = {"addCollapsibleArea", "addNonCollapsibleArea", "removeCollapsibleArea", "removeNonCollapsibleArea"};
            Method[] methodArr = new Method[4];
            int i = 0;
            while (i < 4) {
                methodArr[i] = cls.getDeclaredMethod(strArr[i], i < 2 ? cls2 : String.class);
                if (methodArr[i].getReturnType() != Void.TYPE || Modifier.isStatic(methodArr[i].getModifiers())) {
                    throw new NoSuchMethodException("Gesture setter ABI");
                }
                i++;
            }
            int i2 = 0;
            while (i2 < 4) {
                XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(methodArr[i2], hook(cls, declaredField, i2 % 2 == 0 ? declaredField2 : declaredField3, i2 == 0 ? "LandscapeHeader" : "TileBoxExpandButton", i2 >= 2));
                if (unhookHookMethod != null) {
                    arrayList.add(unhookHookMethod);
                    i2++;
                } else {
                    throw new IllegalStateException("Gesture hook rejected");
                }
            }
            ControlBus.feature("qs_gesture_work", "Native hit areas without diagnostic formatting");
        } catch (Throwable th) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                RuntimeHooks.unhook((XC_MethodHook.Unhook) it.next());
            }
            error = th.getClass().getSimpleName();
            ControlBus.feature("qs_gesture_work", "Native fallback");
        }
    }

    private QsGestureWork() {
    }
}
