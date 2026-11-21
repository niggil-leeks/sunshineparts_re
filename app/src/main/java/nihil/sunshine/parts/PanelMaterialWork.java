package nihil.sunshine.parts;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
final class PanelMaterialWork {
    static final int RADIUS = 6;
    private static Access access;
    private static boolean failed;
    private static final WeakIdentityCache<State> states = new WeakIdentityCache<>(32, false);
    private static final ArrayList<WeakReference<View>> views = new ArrayList<>();

    private static final class State {
        final Object[] alphaArgument;
        WeakReference<Drawable> background;
        int density;
        int display;
        int radius;
        Object recipe;
        int uiMode;

        private State() {
            this.background = new WeakReference<>(null);
            this.radius = -1;
            this.alphaArgument = new Object[]{Float.valueOf(0.0f)};
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Class<?> type(ClassLoader classLoader, String str) throws Exception {
        return Class.forName(str, false, classLoader);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Field field(Class<?> cls, String str, Class<?> cls2) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        if (declaredField.getType() == cls2) {
            return declaredField;
        }
        throw new NoSuchFieldException(str);
    }

    static final class Access {
        final Field blocked;
        final Class<?> blur;
        final Method blurCurve;
        final Method blurRadius;
        final MethodHandle callOriginal;
        final Constructor<?> createRecipe;
        final Field curve;
        final Method fraction;
        final Field image;
        final Method originalCall;
        final Class<?> owner;
        final Method parentAlpha;
        final Field[] recipeFields = new Field[PanelMaterialWork.RADIUS];

        Access(ClassLoader classLoader) throws Exception {
            this.owner = PanelMaterialWork.type(classLoader, "com.android.systemui.blur.SecQSNewBlurView");
            Class clsType = PanelMaterialWork.type(classLoader, "com.android.systemui.blur.QSColorCurve");
            Class clsType2 = PanelMaterialWork.type(classLoader, "android.view.SemBlurInfo$ColorCurve");
            this.blur = PanelMaterialWork.type(classLoader, "com.android.internal.graphics.drawable.BackgroundBlurDrawable");
            if (View.class.isAssignableFrom(this.owner) && Drawable.class.isAssignableFrom(this.blur)) {
                this.blocked = PanelMaterialWork.field(this.owner, "isBlurBlocked", Boolean.TYPE);
                this.image = PanelMaterialWork.field(this.owner, "imageView", ImageView.class);
                this.curve = PanelMaterialWork.field(this.owner, "qsColorCurve", clsType);
                String[] strArr = {"saturation", "curve", "minX", "maxX", "minY", "maxY"};
                for (int i = 0; i < PanelMaterialWork.RADIUS; i++) {
                    this.recipeFields[i] = PanelMaterialWork.field(clsType, strArr[i], Float.TYPE);
                }
                this.fraction = clsType.getDeclaredMethod("setFraction", Float.TYPE);
                this.createRecipe = clsType2.getDeclaredConstructor(Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
                this.blurRadius = this.blur.getDeclaredMethod("setBlurRadius", Integer.TYPE);
                this.blurCurve = this.blur.getDeclaredMethod("setBlurColorCurve", clsType2);
                this.parentAlpha = View.class.getDeclaredMethod("setAlpha", Float.TYPE);
                this.originalCall = XposedBridge.class.getDeclaredMethod("invokeOriginalMethod", Member.class, Object.class, Object[].class);
                Method[] methodArr = {this.fraction, this.blurRadius, this.blurCurve, this.parentAlpha, this.originalCall};
                for (int i2 = 0; i2 < 5; i2++) {
                    methodArr[i2].setAccessible(true);
                }
                this.createRecipe.setAccessible(true);
                this.callOriginal = MethodHandles.lookup().unreflect(this.originalCall).asType(MethodType.methodType(Object.class, Member.class, Object.class, Object[].class));
                if (this.fraction.getReturnType() != Void.TYPE || this.blurRadius.getReturnType() != Void.TYPE || this.blurCurve.getReturnType() != Void.TYPE) {
                    throw new NoSuchMethodException("Native blur setters");
                }
                return;
            }
            throw new NoSuchMethodException("Panel material ABI");
        }

        /* JADX WARN: Code duplicated, block: B:38:0x0085  */
        /* JADX WARN: Code duplicated, block: B:40:0x00a2 A[LOOP:0: B:39:0x00a0->B:40:0x00a2, LOOP_END] */
        /* JADX WARN: Code duplicated, block: B:42:0x00be  */
        /* JADX WARN: Code duplicated, block: B:45:0x00c6  */
        /* JADX WARN: Code duplicated, block: B:46:0x00c8  */
        /* JADX WARN: Code duplicated, block: B:49:0x00d2  */
        /* JADX WARN: Code duplicated, block: B:51:0x00d6  */
        /* JADX WARN: Code duplicated, block: B:52:0x00db  */
        /* JADX WARN: Code duplicated, block: B:56:0x00f3  */
        /* JADX WARN: Code duplicated, block: B:62:0x0105  */
        /* JADX WARN: Code duplicated, block: B:65:0x0127  */
        boolean alpha(Object obj, float f, Object obj2, State state) throws Throwable {
            View view;
            Drawable background;
            float f2;
            boolean z;
            Object objNewInstance;
            boolean z2;
            int i;
            int i2;
            int i3;
            Object objValueOf;
            Object obj3;
            int length;
            Object[] objArr;
            int i4;
            if (!Float.isFinite(f) || f < 0.0f || f > 1.0f || (background = (view = (View) obj).getBackground()) == null || !this.blur.isInstance(background)) {
                return false;
            }
            boolean z3 = this.blocked.getBoolean(obj);
            float f3 = z3 ? 0.0f : f;
            ImageView imageView = (ImageView) this.image.get(obj);
            Configuration configuration = view.getResources().getConfiguration();
            int displayId = view.getDisplay() == null ? -1 : view.getDisplay().getDisplayId();
            if (state.recipe != null && state.background.get() == background) {
                f2 = 0.0f;
                if (state.uiMode == configuration.uiMode && state.density == configuration.densityDpi && state.display == displayId) {
                    z = false;
                }
                objNewInstance = state.recipe;
                if (z) {
                    z2 = true;
                    i = 0;
                } else {
                    obj3 = this.curve.get(obj);
                    this.fraction.invoke(obj3, Float.valueOf(1.0f));
                    length = this.recipeFields.length;
                    objArr = new Object[length];
                    i = 0;
                    for (i4 = 0; i4 < length; i4++) {
                        objArr[i4] = Float.valueOf(this.recipeFields[i4].getFloat(obj3));
                    }
                    z2 = true;
                    objNewInstance = this.createRecipe.newInstance(objArr);
                }
                if (f3 > f2) {
                    i2 = PanelMaterialWork.RADIUS;
                } else {
                    i2 = i;
                }
                if (view.getAlpha() != f3) {
                    Object[] objArr2 = state.alphaArgument;
                    if (z3) {
                        objValueOf = Float.valueOf(f2);
                    } else {
                        objValueOf = obj2;
                    }
                    objArr2[i] = objValueOf;
                    (Object) this.callOriginal.invokeExact(this.parentAlpha, view, state.alphaArgument);
                }
                i3 = (int) (255.0f * f3);
                if (background.getAlpha() != i3) {
                    background.setAlpha(i3);
                }
                if (imageView != null && imageView.getAlpha() != f3) {
                    imageView.setAlpha(f3);
                }
                if (z) {
                    this.blurCurve.invoke(background, objNewInstance);
                    state.background = new WeakReference<>(background);
                    state.recipe = objNewInstance;
                    state.uiMode = configuration.uiMode;
                    state.density = configuration.densityDpi;
                    state.display = displayId;
                    state.radius = -1;
                }
                if (state.radius != i2) {
                    this.blurRadius.invoke(background, Integer.valueOf(i2));
                    state.radius = i2;
                }
                return z2;
            }
            f2 = 0.0f;
            z = true;
            objNewInstance = state.recipe;
            if (z) {
                z2 = true;
                i = 0;
            } else {
                obj3 = this.curve.get(obj);
                this.fraction.invoke(obj3, Float.valueOf(1.0f));
                length = this.recipeFields.length;
                objArr = new Object[length];
                i = 0;
                while (i4 < length) {
                    objArr[i4] = Float.valueOf(this.recipeFields[i4].getFloat(obj3));
                }
                z2 = true;
                objNewInstance = this.createRecipe.newInstance(objArr);
            }
            if (f3 > f2) {
                i2 = PanelMaterialWork.RADIUS;
            } else {
                i2 = i;
            }
            if (view.getAlpha() != f3) {
                Object[] objArr3 = state.alphaArgument;
                if (z3) {
                    objValueOf = Float.valueOf(f2);
                } else {
                    objValueOf = obj2;
                }
                objArr3[i] = objValueOf;
                (Object) this.callOriginal.invokeExact(this.parentAlpha, view, state.alphaArgument);
            }
            i3 = (int) (255.0f * f3);
            if (background.getAlpha() != i3) {
                background.setAlpha(i3);
            }
            if (imageView != null) {
                imageView.setAlpha(f3);
            }
            if (z) {
                this.blurCurve.invoke(background, objNewInstance);
                state.background = new WeakReference<>(background);
                state.recipe = objNewInstance;
                state.uiMode = configuration.uiMode;
                state.density = configuration.densityDpi;
                state.display = displayId;
                state.radius = -1;
            }
            if (state.radius != i2) {
                this.blurRadius.invoke(background, Integer.valueOf(i2));
                state.radius = i2;
            }
            return z2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static State state(View view) {
        State state = states.get(view);
        if (state != null) {
            return state;
        }
        State state2 = new State();
        states.put(view, state2);
        if (views.size() >= 32) {
            views.remove(0);
        }
        views.add(new WeakReference<>(view));
        return state2;
    }

    static void configure() {
        if (access == null || Looper.myLooper() != Looper.getMainLooper()) {
            return;
        }
        Iterator<WeakReference<View>> it = views.iterator();
        while (it.hasNext()) {
            View view = it.next().get();
            if (view == null) {
                it.remove();
            } else {
                State state = states.get(view);
                if (state != null) {
                    state.recipe = null;
                    state.radius = -1;
                }
                if (view.isAttachedToWindow()) {
                    view.setAlpha(view.getAlpha());
                }
            }
        }
    }

    static void install(ClassLoader classLoader, String str) {
        ArrayList<XC_MethodHook.Unhook> arrayList = new ArrayList();
        try {
            if (DensityQueries.pinned(str) && IconDimensions.pinned(str)) {
                final Access access2 = new Access(classLoader);
                Method declaredMethod = access2.owner.getDeclaredMethod("setAlpha", Float.TYPE);
                Method declaredMethod2 = access2.owner.getDeclaredMethod("onConfigurationChanged", Configuration.class);
                if (declaredMethod.getReturnType() != Void.TYPE || declaredMethod2.getReturnType() != Void.TYPE) {
                    throw new NoSuchMethodException("Panel material methods");
                }
                declaredMethod.setAccessible(true);
                declaredMethod2.setAccessible(true);
                arrayList.add(RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.PanelMaterialWork.1
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (!PanelMaterialWork.failed && AdaptiveWorkload.level() == 3 && Looper.myLooper() == Looper.getMainLooper() && methodHookParam.thisObject.getClass() == access2.owner) {
                            try {
                                if (access2.alpha(methodHookParam.thisObject, ((Float) methodHookParam.args[0]).floatValue(), methodHookParam.args[0], PanelMaterialWork.state((View) methodHookParam.thisObject))) {
                                    methodHookParam.setResult((Object) null);
                                }
                            } catch (Throwable th) {
                                boolean unused = PanelMaterialWork.failed = true;
                            }
                        }
                    }
                }));
                arrayList.add(RuntimeHooks.hookMethod(declaredMethod2, new XC_MethodHook() { // from class: nihil.sunshine.parts.PanelMaterialWork.2
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        State state = (State) PanelMaterialWork.states.get(methodHookParam.thisObject);
                        if (state != null) {
                            state.recipe = null;
                            state.radius = -1;
                        }
                    }
                }));
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    if (((XC_MethodHook.Unhook) it.next()) == null) {
                        throw new IllegalStateException("Panel material hook unavailable");
                    }
                }
                access = access2;
            }
        } catch (Throwable th) {
            for (XC_MethodHook.Unhook unhook : arrayList) {
                if (unhook != null) {
                    RuntimeHooks.unhook(unhook);
                }
            }
            access = null;
        }
    }

    private PanelMaterialWork() {
    }
}
