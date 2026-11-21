package nihil.sunshine.parts;

import android.content.res.Resources;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
final class IconDimensions {
    private static final int LIMIT = 8;
    private static final String MISS_OWNER = "sunshine.icon.dimensions.owner";
    private static final String MISS_PASS = "sunshine.icon.dimensions.pass";
    private static final int NESTING = 4;
    static long hits;
    static long nativeLoads;
    static long passes;
    static long snapshotsCreated;
    static long unchangedHits;
    static String error = "";
    private static final ThreadLocal<Passes> local = new ThreadLocal<>();

    static String sourceApk(Object obj) {
        try {
            Object obj2 = obj.getClass().getField("appInfo").get(obj);
            if (obj2 == null) {
                return null;
            }
            return (String) obj2.getClass().getField("sourceDir").get(obj2);
        } catch (Throwable th) {
            return null;
        }
    }

    static boolean pinned(String str) {
        return NativeTargets.systemUi(str);
    }

    private static final class Values {
        int drawing;
        int increased;
        int original;
        int radius;
        float scale;
        int size;

        Values() {
            IconDimensions.snapshotsCreated++;
        }

        void read(Fields fields, Object obj) throws IllegalAccessException {
            this.radius = fields.radius.getInt(obj);
            this.original = fields.original.getInt(obj);
            this.size = fields.size.getInt(obj);
            this.increased = fields.increased.getInt(obj);
            this.drawing = fields.drawing.getInt(obj);
            this.scale = fields.scale.getFloat(obj);
        }
    }

    private static final class Frame {
        int count;
        final Resources[] resources;
        final Values[] values;

        private Frame() {
            this.resources = new Resources[IconDimensions.LIMIT];
            this.values = new Values[IconDimensions.LIMIT];
        }

        int find(Resources resources) {
            for (int i = 0; i < this.count; i++) {
                if (this.resources[i] == resources) {
                    return i;
                }
            }
            return -1;
        }

        void clear() {
            for (int i = 0; i < this.count; i++) {
                this.resources[i] = null;
            }
            this.count = 0;
        }
    }

    private static final class Passes {
        int depth;
        final Frame[] frames;

        private Passes() {
            this.frames = new Frame[IconDimensions.NESTING];
        }

        Frame current() {
            if (this.depth <= 0 || this.depth > IconDimensions.NESTING) {
                return null;
            }
            return this.frames[this.depth - 1];
        }

        void begin() {
            int i = this.depth + 1;
            this.depth = i;
            if (i <= IconDimensions.NESTING) {
                if (this.frames[this.depth - 1] == null) {
                    this.frames[this.depth - 1] = new Frame();
                }
                this.frames[this.depth - 1].clear();
            }
        }

        void end() {
            Frame frameCurrent = current();
            if (frameCurrent != null) {
                frameCurrent.clear();
            }
            if (this.depth > 0) {
                this.depth--;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Frame current() {
        Passes passes2 = local.get();
        if (passes2 == null) {
            return null;
        }
        return passes2.current();
    }

    static XC_MethodHook passHook() {
        return new XC_MethodHook(Integer.MIN_VALUE) { // from class: nihil.sunshine.parts.IconDimensions.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Passes passes2 = (Passes) IconDimensions.local.get();
                if (passes2 == null) {
                    passes2 = new Passes();
                    IconDimensions.local.set(passes2);
                }
                passes2.begin();
                IconDimensions.passes++;
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                ((Passes) IconDimensions.local.get()).end();
            }
        };
    }

    static final class Fields {
        final Class<?> iconClass;
        final Field radius = field("mStaticDotRadius", Integer.TYPE);
        final Field original = field("mOriginalStatusBarIconSize", Integer.TYPE);
        final Field size = field("mNewStatusBarIconSize", Integer.TYPE);
        final Field increased = field("mStatusBarIconDrawingSizeIncreased", Integer.TYPE);
        final Field drawing = field("mStatusBarIconDrawingSize", Integer.TYPE);
        final Field scale = field("mScaleToFitNewIconSize", Float.TYPE);
        final Field dot = field("mDotRadius", Float.TYPE);

        private Field field(String str, Class<?> cls) throws Exception {
            Field declaredField = this.iconClass.getDeclaredField(str);
            declaredField.setAccessible(true);
            if (declaredField.getType() == cls) {
                return declaredField;
            }
            throw new NoSuchFieldException(str);
        }

        Fields(Class<?> cls) throws Exception {
            this.iconClass = cls;
        }

        void apply(Object obj, Values values) throws Exception {
            int i = this.radius.getInt(obj);
            int i2 = this.original.getInt(obj);
            int i3 = this.size.getInt(obj);
            int i4 = this.increased.getInt(obj);
            int i5 = this.drawing.getInt(obj);
            float f = this.scale.getFloat(obj);
            float f2 = this.dot.getFloat(obj);
            float f3 = f2 == ((float) i) ? values.radius : f2;
            boolean z = Float.floatToRawIntBits(f) != Float.floatToRawIntBits(values.scale);
            boolean z2 = Float.floatToRawIntBits(f2) != Float.floatToRawIntBits(f3);
            if (i == values.radius && i2 == values.original && i3 == values.size && i4 == values.increased && i5 == values.drawing && !z && !z2) {
                IconDimensions.unchangedHits++;
                return;
            }
            try {
                if (i != values.radius) {
                    this.radius.setInt(obj, values.radius);
                }
                if (i2 != values.original) {
                    this.original.setInt(obj, values.original);
                }
                if (i3 != values.size) {
                    this.size.setInt(obj, values.size);
                }
                if (z) {
                    this.scale.setFloat(obj, values.scale);
                }
                if (i4 != values.increased) {
                    this.increased.setInt(obj, values.increased);
                }
                if (i5 != values.drawing) {
                    this.drawing.setInt(obj, values.drawing);
                }
                if (z2) {
                    this.dot.setFloat(obj, f3);
                }
            } catch (IllegalAccessException | RuntimeException e) {
                try {
                    this.radius.setInt(obj, i);
                    this.original.setInt(obj, i2);
                    this.size.setInt(obj, i3);
                    this.increased.setInt(obj, i4);
                    this.drawing.setInt(obj, i5);
                    this.scale.setFloat(obj, f);
                    this.dot.setFloat(obj, f2);
                } catch (Throwable th) {
                    e.addSuppressed(th);
                }
                throw e;
            }
        }
    }

    static XC_MethodHook reloadHook(final Fields fields) {
        return new XC_MethodHook(Integer.MIN_VALUE) { // from class: nihil.sunshine.parts.IconDimensions.2
            private boolean disabled;

            private void fallback(Throwable th) {
                this.disabled = true;
                IconDimensions.error = th.getClass().getSimpleName();
                ControlBus.feature("icon_dimensions", "native fallback");
            }

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Frame frameCurrent;
                if (this.disabled || !Config.leanQs || methodHookParam.thisObject.getClass() != fields.iconClass || (frameCurrent = IconDimensions.current()) == null) {
                    return;
                }
                try {
                    Resources resources = ((View) methodHookParam.thisObject).getResources();
                    int iFind = frameCurrent.find(resources);
                    if (iFind >= 0) {
                        fields.apply(methodHookParam.thisObject, frameCurrent.values[iFind]);
                        IconDimensions.hits++;
                        methodHookParam.setResult((Object) null);
                    } else if (frameCurrent.count < IconDimensions.LIMIT) {
                        methodHookParam.setObjectExtra(IconDimensions.MISS_OWNER, resources);
                        methodHookParam.setObjectExtra(IconDimensions.MISS_PASS, frameCurrent);
                    }
                } catch (Throwable th) {
                    fallback(th);
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Resources resources = (Resources) methodHookParam.getObjectExtra(IconDimensions.MISS_OWNER);
                Frame frame = (Frame) methodHookParam.getObjectExtra(IconDimensions.MISS_PASS);
                if (resources != null && frame != null) {
                    methodHookParam.setObjectExtra(IconDimensions.MISS_OWNER, (Object) null);
                    methodHookParam.setObjectExtra(IconDimensions.MISS_PASS, (Object) null);
                    if (this.disabled || !Config.leanQs || methodHookParam.hasThrowable() || IconDimensions.current() != frame || frame.find(resources) >= 0 || frame.count == IconDimensions.LIMIT) {
                        return;
                    }
                    try {
                        Values values = frame.values[frame.count];
                        if (values == null) {
                            Values[] valuesArr = frame.values;
                            int i = frame.count;
                            Values values2 = new Values();
                            valuesArr[i] = values2;
                            values = values2;
                        }
                        values.read(fields, methodHookParam.thisObject);
                        frame.resources[frame.count] = resources;
                        frame.values[frame.count] = values;
                        frame.count++;
                        IconDimensions.nativeLoads++;
                    } catch (Throwable th) {
                        fallback(th);
                    }
                }
            }
        };
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            if (!pinned(str)) {
                throw new IllegalStateException("SystemUI DEX differs from inspected target");
            }
            Class<?> cls = Class.forName("com.android.systemui.statusbar.StatusBarIconView", false, classLoader);
            Class<?> cls2 = Class.forName("com.android.systemui.statusbar.phone.NotificationIconContainer", false, classLoader);
            Method declaredMethod = cls.getDeclaredMethod("reloadDimens$2", new Class[0]);
            Method declaredMethod2 = cls2.getDeclaredMethod("resetViewStates", new Class[0]);
            if (declaredMethod.getReturnType() != Void.TYPE || declaredMethod2.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("Icon dimension contract");
            }
            if (RuntimeHooks.hookMethod(declaredMethod, reloadHook(new Fields(cls))) != null && RuntimeHooks.hookMethod(declaredMethod2, passHook()) != null) {
                ControlBus.feature("icon_dimensions", "per-pass native values");
                return;
            }
            throw new IllegalStateException("Icon hook rejected");
        } catch (Throwable th) {
            if (0 != 0) {
                RuntimeHooks.unhook(null);
            }
            error = th.getClass().getSimpleName();
            ControlBus.feature("icon_dimensions", "native fallback");
        }
    }

    private IconDimensions() {
    }
}
