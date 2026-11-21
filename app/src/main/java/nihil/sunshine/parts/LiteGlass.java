package nihil.sunshine.parts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
final class LiteGlass {
    private final Field context;
    private long created;
    private boolean failed;
    private final Method generator;
    private boolean logged;
    private final Object quick;
    private long served;
    private final WeakHashMap<Object, Entry> entries = new WeakHashMap<>();
    private final WeakHashMap<View, Boolean> opaqueViews = new WeakHashMap<>();
    private final WeakHashMap<View, Float> quickViews = new WeakHashMap<>();
    private final WeakHashMap<View, WeakReference<Drawable>> originals = new WeakHashMap<>();
    private final ArrayList<XC_MethodHook.Unhook> alphaHooks = new ArrayList<>();

    private static final class Entry {
        final int density;
        final BitmapDrawable drawable;
        final boolean night;
        final Resources resources;

        Entry(Resources resources, boolean z, BitmapDrawable bitmapDrawable) {
            this.resources = resources;
            this.night = z;
            this.density = resources.getDisplayMetrics().densityDpi;
            this.drawable = bitmapDrawable;
        }
    }

    LiteGlass(ClassLoader classLoader) throws Exception {
        Class<?> cls = Class.forName("com.android.systemui.blur.di.SecPanelBlurBinding$BlurType", false, classLoader);
        Object obj = null;
        for (Object obj2 : cls.getEnumConstants()) {
            if (((Enum) obj2).name().equals("QUICK_PANEL")) {
                obj = obj2;
            }
        }
        if (obj == null) {
            throw new NoSuchFieldException("QUICK_PANEL");
        }
        this.quick = obj;
        Class<?> cls2 = Class.forName("com.android.systemui.blur.domain.interactor.SecCapturedBlurBitmapGenerator", false, classLoader);
        this.generator = cls2.getDeclaredMethod("getBlurredBitmapWithEffect", cls);
        this.context = cls2.getDeclaredField("context");
        this.generator.setAccessible(true);
        this.context.setAccessible(true);
        if (this.generator.getReturnType() != BitmapDrawable.class || this.context.getType() != Context.class) {
            throw new NoSuchMethodException("Captured blur contract");
        }
    }

    void install() {
        installOpacity();
        syncAlphaHooks();
        RuntimeHooks.hookMethod(this.generator, new XC_MethodHook() { // from class: nihil.sunshine.parts.LiteGlass.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (!VisualPolicy.nativeBlur() && !LiteGlass.this.failed && methodHookParam.args[0] == LiteGlass.this.quick) {
                    try {
                        methodHookParam.setResult(LiteGlass.this.material(methodHookParam.thisObject, ((Context) LiteGlass.this.context.get(methodHookParam.thisObject)).getResources()));
                    } catch (Throwable th) {
                        LiteGlass.this.failed = true;
                        Log.e("SunshinePartsUI", "Lite glass disabled; native path retained", th);
                    }
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized BitmapDrawable material(Object obj, Resources resources) {
        BitmapDrawable bitmapDrawable;
        boolean z = (resources.getConfiguration().uiMode & 48) == 32;
        Entry entry = this.entries.get(obj);
        if (entry == null || entry.resources != resources || entry.night != z || entry.density != resources.getDisplayMetrics().densityDpi || entry.drawable.getBitmap().isRecycled()) {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            bitmapCreateBitmap.setDensity(0);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Paint paint = new Paint();
            paint.setShader(new LinearGradient(0.0f, 0.0f, 64.0f, 64.0f, z ? LiteStyle.DARK : LiteStyle.LIGHT, new float[]{0.0f, 0.45f, 1.0f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0.0f, 0.0f, 64.0f, 64.0f, paint);
            paint.setShader(null);
            paint.setColor(LiteStyle.edge(z));
            canvas.drawRect(0.0f, 0.0f, 64.0f, 1.0f, paint);
            BitmapDrawable bitmapDrawable2 = new BitmapDrawable(resources, bitmapCreateBitmap);
            bitmapDrawable2.setGravity(119);
            bitmapDrawable2.setFilterBitmap(true);
            bitmapDrawable2.setDither(false);
            entry = new Entry(resources, z, bitmapDrawable2);
            this.entries.put(obj, entry);
            this.created++;
        }
        this.served++;
        if (!this.logged) {
            this.logged = true;
            Log.i("SunshinePartsUI", "APPLIED: QS static glass; capture and bitmap blur bypassed");
        }
        bitmapDrawable = new BitmapDrawable(resources, entry.drawable.getBitmap());
        bitmapDrawable.setGravity(119);
        bitmapDrawable.setFilterBitmap(true);
        bitmapDrawable.setDither(false);
        return bitmapDrawable;
    }

    synchronized String report() {
        return "liteGlassServed=" + this.served + " materialsCreated=" + this.created + " failed=" + this.failed;
    }

    void refreshMode() {
        syncAlphaHooks();
        synchronized (this.opaqueViews) {
            this.opaqueViews.clear();
        }
        for (Map.Entry entry : new ArrayList(this.quickViews.entrySet())) {
            View view = (View) entry.getKey();
            if (view != null) {
                if (((Float) entry.getValue()).floatValue() <= 0.0f) {
                    view.setBackground(null);
                } else {
                    if (VisualPolicy.nativeBlur()) {
                        WeakReference<Drawable> weakReference = this.originals.get(view);
                        view.setBackground(weakReference != null ? weakReference.get() : null);
                    } else {
                        view.setBackground(material(view, view.getResources()));
                        view.setAlpha(1.0f);
                    }
                    view.postInvalidateOnAnimation();
                }
            }
        }
    }

    private void installOpacity() {
        try {
            Class<?> cls = Class.forName("com.android.systemui.blur.ui.viewbinder.SecCapturedBlurContainerBinder", false, this.generator.getDeclaringClass().getClassLoader());
            final Field declaredField = cls.getDeclaredField("view");
            declaredField.setAccessible(true);
            Method declaredMethod = cls.getDeclaredMethod("setFraction", Float.TYPE, this.quick.getClass());
            if (!View.class.isAssignableFrom(declaredField.getType())) {
                throw new NoSuchFieldException("Backdrop view");
            }
            RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.LiteGlass.2
                private void mark(XC_MethodHook.MethodHookParam methodHookParam) throws Exception {
                    View view = (View) declaredField.get(methodHookParam.thisObject);
                    if (view == null) {
                        return;
                    }
                    boolean z = false;
                    if (methodHookParam.args[1] == LiteGlass.this.quick) {
                        LiteGlass.this.quickViews.put(view, (Float) methodHookParam.args[0]);
                        if (VisualPolicy.nativeBlur() && view.getBackground() != null && (LiteGlass.this.originals.get(view) == null || ((WeakReference) LiteGlass.this.originals.get(view)).get() != view.getBackground())) {
                            LiteGlass.this.originals.put(view, new WeakReference(view.getBackground()));
                        }
                    } else {
                        LiteGlass.this.quickViews.remove(view);
                    }
                    if (!VisualPolicy.nativeBlur() && !LiteGlass.this.failed && methodHookParam.args[1] == LiteGlass.this.quick && ((Float) methodHookParam.args[0]).floatValue() > 0.0f) {
                        z = true;
                    }
                    synchronized (LiteGlass.this.opaqueViews) {
                        try {
                            if (z) {
                                LiteGlass.this.opaqueViews.put(view, true);
                            } else {
                                LiteGlass.this.opaqueViews.remove(view);
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                }

                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    try {
                        mark(methodHookParam);
                    } catch (Throwable th) {
                    }
                }

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (methodHookParam.hasThrowable()) {
                        return;
                    }
                    try {
                        View view = (View) declaredField.get(methodHookParam.thisObject);
                        if (view == null) {
                            return;
                        }
                        boolean z = false;
                        if (!VisualPolicy.nativeBlur() && !LiteGlass.this.failed && methodHookParam.args[1] == LiteGlass.this.quick && ((Float) methodHookParam.args[0]).floatValue() > 0.0f) {
                            z = true;
                        }
                        if (!z || view.getAlpha() == 1.0f) {
                            return;
                        }
                        view.setAlpha(1.0f);
                    } catch (Throwable th) {
                    }
                }
            });
        } catch (Throwable th) {
            Log.w("SunshinePartsUI", "Backdrop alpha path unavailable", th);
        }
    }

    private void syncAlphaHooks() {
        if (VisualPolicy.nativeBlur() || this.failed) {
            Iterator<XC_MethodHook.Unhook> it = this.alphaHooks.iterator();
            while (it.hasNext()) {
                RuntimeHooks.unhook(it.next());
            }
            this.alphaHooks.clear();
            ControlBus.feature("blur_alpha_scope", "native path");
            return;
        }
        if (this.alphaHooks.isEmpty()) {
            try {
                String[] strArr = {"setAlpha", "setTransitionAlpha"};
                for (int i = 0; i < 2; i++) {
                    this.alphaHooks.add(RuntimeHooks.hookMethod(View.class.getDeclaredMethod(strArr[i], Float.TYPE), new XC_MethodHook() { // from class: nihil.sunshine.parts.LiteGlass.3
                        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                            if (!VisualPolicy.nativeBlur() && Float.isFinite(((Float) methodHookParam.args[0]).floatValue())) {
                                synchronized (LiteGlass.this.opaqueViews) {
                                    if (LiteGlass.this.opaqueViews.containsKey((View) methodHookParam.thisObject)) {
                                        methodHookParam.args[0] = Float.valueOf(1.0f);
                                    }
                                }
                            }
                        }
                    }));
                }
                ControlBus.feature("blur_alpha_scope", "static glass only");
            } catch (Throwable th) {
                Iterator<XC_MethodHook.Unhook> it2 = this.alphaHooks.iterator();
                while (it2.hasNext()) {
                    RuntimeHooks.unhook(it2.next());
                }
                this.alphaHooks.clear();
                Log.w("SunshinePartsUI", "Backdrop alpha path unavailable", th);
            }
        }
    }
}
