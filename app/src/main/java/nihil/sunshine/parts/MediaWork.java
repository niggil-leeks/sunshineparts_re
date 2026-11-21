package nihil.sunshine.parts;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.View;
import android.widget.SeekBar;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
final class MediaWork {
    private static Access access;
    private static boolean failed;
    private static final WeakIdentityCache<State> states = new WeakIdentityCache<>(64, false);
    private static final ArrayList<WeakReference<Drawable>> drawables = new ArrayList<>();
    private static final HotScope invalidating = new HotScope();

    private static final class State {
        int bottom;
        int density;
        int height;
        boolean known;
        int left;
        int level;
        final Paint paint;
        int right;
        int top;
        int uiMode;
        boolean ultra;
        int width;

        private State() {
            this.paint = new Paint(1);
        }

        boolean same(Drawable drawable, View view) {
            Rect bounds = drawable.getBounds();
            return this.known && this.left == bounds.left && this.top == bounds.top && this.right == bounds.right && this.bottom == bounds.bottom && this.level == drawable.getLevel() && this.height == view.getHeight() && this.width == view.getWidth() && this.density == Float.floatToIntBits(view.getResources().getDisplayMetrics().density) && this.uiMode == view.getResources().getConfiguration().uiMode;
        }

        void remember(Drawable drawable, View view) {
            Rect bounds = drawable.getBounds();
            this.left = bounds.left;
            this.top = bounds.top;
            this.right = bounds.right;
            this.bottom = bounds.bottom;
            this.level = drawable.getLevel();
            this.height = view.getHeight();
            this.width = view.getWidth();
            this.density = Float.floatToIntBits(view.getResources().getDisplayMetrics().density);
            this.uiMode = view.getResources().getConfiguration().uiMode;
            this.known = true;
        }
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

    /* JADX INFO: Access modifiers changed from: private */
    public static Class<?> type(ClassLoader classLoader, String str) throws Exception {
        return Class.forName(str, false, classLoader);
    }

    static final class Access {
        final Field active;
        final Method bounds;
        final Field config;
        final Field corner;
        final Field motion;
        final Class<?> owner;
        final Field primary;
        final Field remain;
        final Field renderer;
        final Field secondary;
        final Field step;
        final Field view;

        Access(ClassLoader classLoader) throws Exception {
            this.owner = MediaWork.type(classLoader, "com.android.systemui.media.audiovisseekbar.AudioVisSeekBarProgressDrawable");
            Class clsType = MediaWork.type(classLoader, "com.android.systemui.media.audiovisseekbar.config.AudioVisSeekBarConfig");
            Class clsType2 = MediaWork.type(classLoader, "com.android.systemui.media.audiovisseekbar.renderer.track.auto.MultiWaveAreaTrackRenderer");
            this.view = MediaWork.field(this.owner, "view", SeekBar.class);
            this.config = MediaWork.field(this.owner, "config", clsType);
            this.active = MediaWork.field(this.owner, "active", Boolean.TYPE);
            this.primary = MediaWork.field(clsType, "primaryColor", Integer.TYPE);
            this.secondary = MediaWork.field(clsType, "secondaryColor", Integer.TYPE);
            this.remain = MediaWork.field(clsType, "remainTrackColor", Integer.TYPE);
            this.motion = MediaWork.field(MediaWork.type(classLoader, "com.android.systemui.media.audiovisseekbar.renderer.BaseRenderer"), "motionActivity", Float.TYPE);
            this.renderer = MediaWork.field(this.owner, "trackRenderer", clsType2);
            this.corner = MediaWork.field(clsType2, "leftCornerBounds", RectF.class);
            this.step = MediaWork.field(clsType2, "stepX", Integer.TYPE);
            this.bounds = this.owner.getDeclaredMethod("onBoundsChange", Rect.class);
            this.bounds.setAccessible(true);
            if (!Drawable.class.isAssignableFrom(this.owner) || this.bounds.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("Media drawable ABI");
            }
        }

        View view(Object obj) throws Exception {
            return (View) this.view.get(obj);
        }

        void cap(Object obj) throws Exception {
            Object obj2 = this.renderer.get(obj);
            int iMin = Math.min(AdaptiveWorkload.mediaStep(), this.motion.getFloat(obj2) < 1.0f ? Math.max(1, (int) Math.floor(((RectF) this.corner.get(obj2)).width() / 8.0f)) : Integer.MAX_VALUE);
            if (this.step.getInt(obj2) != iMin) {
                this.step.setInt(obj2, iMin);
            }
        }

        boolean flat(Object obj, Canvas canvas, State state) throws Exception {
            Drawable drawable = (Drawable) obj;
            View view = view(obj);
            float fWidth = drawable.getBounds().width();
            float f = view.getResources().getDisplayMetrics().density;
            if (fWidth <= 0.0f || view.getHeight() <= 0 || f <= 0.0f || !Float.isFinite(f)) {
                return false;
            }
            Object obj2 = this.config.get(obj);
            int i = (this.active.getBoolean(obj) ? this.primary : this.secondary).getInt(obj2);
            int i2 = this.remain.getInt(obj2);
            float f2 = f * 4.0f;
            float height = view.getHeight() - (f * 8.0f);
            float fMax = Math.max(0.0f, Math.min(1.0f, drawable.getLevel() / 10000.0f)) * fWidth;
            Paint paint = state.paint;
            paint.setColor(i2);
            float f3 = height - f2;
            float f4 = height + f2;
            canvas.drawRoundRect(0.0f, f3, fWidth, f4, f2, f2, paint);
            if (fMax > 0.0f) {
                int iSave = canvas.save();
                canvas.clipRect(0.0f, f3, fMax, f4);
                paint.setColor(i);
                canvas.drawRoundRect(0.0f, f3, fWidth, f4, f2, f2, paint);
                canvas.restoreToCount(iSave);
                return true;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean main() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean ultra() {
        return AdaptiveWorkload.level() == 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static State state(Object obj) {
        State state = states.get(obj);
        if (state != null) {
            return state;
        }
        State state2 = new State();
        states.put(obj, state2);
        if (drawables.size() >= 64) {
            drawables.remove(0);
        }
        drawables.add(new WeakReference<>((Drawable) obj));
        return state2;
    }

    static void configure() {
        if (!main() || access == null) {
            return;
        }
        Iterator<WeakReference<Drawable>> it = drawables.iterator();
        while (it.hasNext()) {
            Drawable drawable = it.next().get();
            if (drawable == null) {
                it.remove();
            } else {
                drawable.invalidateSelf();
            }
        }
    }

    static void install(ClassLoader classLoader, String str) {
        ArrayList<XC_MethodHook.Unhook> arrayList = new ArrayList();
        try {
            if (DensityQueries.pinned(str) && IconDimensions.pinned(str)) {
                final Access access2 = new Access(classLoader);
                Class<?> cls = access2.owner;
                Method declaredMethod = cls.getDeclaredMethod("draw", Canvas.class);
                Method declaredMethod2 = cls.getDeclaredMethod("invalidateSelf", new Class[0]);
                Method declaredMethod3 = cls.getDeclaredMethod("onLevelChange", Integer.TYPE);
                if (declaredMethod.getReturnType() != Void.TYPE || declaredMethod2.getReturnType() != Void.TYPE || declaredMethod3.getReturnType() != Boolean.TYPE) {
                    throw new NoSuchMethodException("Media method ABI");
                }
                Method[] methodArr = {declaredMethod, declaredMethod2, declaredMethod3};
                for (int i = 0; i < 3; i++) {
                    methodArr[i].setAccessible(true);
                }
                arrayList.add(RuntimeHooks.hookMethod(declaredMethod2, new XC_MethodHook() { // from class: nihil.sunshine.parts.MediaWork.1
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        MediaWork.invalidating.enter(methodHookParam.thisObject);
                    }

                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        MediaWork.invalidating.leave();
                    }
                }));
                arrayList.add(RuntimeHooks.hookMethod(access2.bounds, new XC_MethodHook() { // from class: nihil.sunshine.parts.MediaWork.2
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (!MediaWork.failed && MediaWork.main() && methodHookParam.thisObject.getClass() == access2.owner) {
                            try {
                                State state = MediaWork.state(methodHookParam.thisObject);
                                if (!MediaWork.ultra()) {
                                    if (!MediaWork.invalidating.is(methodHookParam.thisObject) || !state.same((Drawable) methodHookParam.thisObject, access2.view(methodHookParam.thisObject))) {
                                        return;
                                    }
                                    methodHookParam.setResult((Object) null);
                                    return;
                                }
                                state.ultra = true;
                                state.known = false;
                                methodHookParam.setResult((Object) null);
                            } catch (Throwable th) {
                                boolean unused = MediaWork.failed = true;
                            }
                        }
                    }

                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (!MediaWork.failed && MediaWork.main() && !MediaWork.ultra() && !methodHookParam.hasThrowable() && methodHookParam.thisObject.getClass() == access2.owner) {
                            try {
                                State state = MediaWork.state(methodHookParam.thisObject);
                                if (((Rect) methodHookParam.args[0]).equals(((Drawable) methodHookParam.thisObject).getBounds())) {
                                    state.remember((Drawable) methodHookParam.thisObject, access2.view(methodHookParam.thisObject));
                                    state.ultra = false;
                                } else {
                                    state.known = false;
                                }
                            } catch (Throwable th) {
                                boolean unused = MediaWork.failed = true;
                            }
                        }
                    }
                }));
                arrayList.add(RuntimeHooks.hookMethod(declaredMethod3, new XC_MethodHook() { // from class: nihil.sunshine.parts.MediaWork.3
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (!MediaWork.failed && MediaWork.main() && MediaWork.ultra() && methodHookParam.thisObject.getClass() == access2.owner) {
                            State state = MediaWork.state(methodHookParam.thisObject);
                            state.ultra = true;
                            state.known = false;
                            methodHookParam.setResult(true);
                        }
                    }
                }));
                arrayList.add(RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.MediaWork.4
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (!MediaWork.failed && MediaWork.main() && methodHookParam.thisObject.getClass() == access2.owner) {
                            try {
                                State state = MediaWork.state(methodHookParam.thisObject);
                                if (MediaWork.ultra()) {
                                    if (access2.flat(methodHookParam.thisObject, (Canvas) methodHookParam.args[0], state)) {
                                        state.ultra = true;
                                        state.known = false;
                                        methodHookParam.setResult((Object) null);
                                        return;
                                    }
                                    return;
                                }
                                if (state.ultra) {
                                    state.ultra = false;
                                    state.known = false;
                                    access2.bounds.invoke(methodHookParam.thisObject, ((Drawable) methodHookParam.thisObject).getBounds());
                                }
                                if (access2.motion.getFloat(access2.renderer.get(methodHookParam.thisObject)) == 0.0f && access2.flat(methodHookParam.thisObject, (Canvas) methodHookParam.args[0], state)) {
                                    methodHookParam.setResult((Object) null);
                                } else {
                                    access2.cap(methodHookParam.thisObject);
                                }
                            } catch (Throwable th) {
                                boolean unused = MediaWork.failed = true;
                            }
                        }
                    }
                }));
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    if (((XC_MethodHook.Unhook) it.next()) == null) {
                        throw new IllegalStateException("Media hook unavailable");
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

    private MediaWork() {
    }
}
