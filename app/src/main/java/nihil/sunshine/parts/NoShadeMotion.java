package nihil.sunshine.parts;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/* JADX INFO: loaded from: classes.dex */
final class NoShadeMotion {
    private static boolean configured;
    static int hooks;
    static long preserved;
    static long shortened;
    private static final ThreadLocal<Integer> scope = ThreadLocal.withInitial(new Supplier() { // from class: nihil.sunshine.parts.NoShadeMotion$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return NoShadeMotion.lambda$static$0();
        }
    });
    private static final ThreadLocal<ArrayDeque<Integer>> previousScopes = ThreadLocal.withInitial(new Supplier() { // from class: nihil.sunshine.parts.NoShadeMotion$$ExternalSyntheticLambda1
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ArrayDeque();
        }
    });
    private static final Set<Animator> classified = Collections.newSetFromMap(new WeakHashMap());
    private static final Set<Animator> media = Collections.newSetFromMap(new WeakHashMap());
    private static final WeakHashMap<Animator, long[]> originals = new WeakHashMap<>();
    private static final WeakHashMap<Animation, long[]> tweens = new WeakHashMap<>();
    private static final WeakHashMap<ViewPropertyAnimator, long[]> viewTimings = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> configuring = new ThreadLocal<>();
    private static final List<XC_MethodHook.Unhook> ownedHooks = new ArrayList();

    NoShadeMotion() {
    }

    static /* synthetic */ Integer lambda$static$0() {
        return 0;
    }

    static void configure(ClassLoader classLoader) {
        if (VisualPolicy.motionScale() != 1.0f) {
            if (configured) {
                return;
            }
            install(classLoader);
            configured = true;
            return;
        }
        restore();
        Iterator it = new ArrayList(ownedHooks).iterator();
        while (it.hasNext()) {
            RuntimeHooks.unhook((XC_MethodHook.Unhook) it.next());
        }
        ownedHooks.clear();
        hooks = 0;
        configured = false;
        classified.clear();
        media.clear();
        ControlBus.feature("panel_motion", "Native motion; inactive animation hooks detached");
        ControlBus.feature("view_panel_motion", "Native motion; no global animator hooks");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean main() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int context() {
        Integer num = scope.get();
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    static boolean region(View view) {
        boolean z = false;
        for (int i = 0; view != null && i < 32; i++) {
            int iViewRules = MotionPolicy.viewRules(view.getClass());
            if ((iViewRules & 1) != 0) {
                return false;
            }
            if ((iViewRules & 2) != 0) {
                z = true;
            }
            Object parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return z;
    }

    private static boolean protectedView(View view) {
        for (int i = 0; view != null && i < 32; i++) {
            if ((MotionPolicy.viewRules(view.getClass()) & 1) != 0) {
                return true;
            }
            Object parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean finite(Animator animator) {
        if (media.contains(animator)) {
            return false;
        }
        if (animator instanceof ObjectAnimator) {
            Object target = ((ObjectAnimator) animator).getTarget();
            if ((target instanceof View) && protectedView((View) target)) {
                return false;
            }
        }
        if ((animator instanceof ValueAnimator) && ((ValueAnimator) animator).getRepeatCount() == -1) {
            return false;
        }
        if (animator instanceof AnimatorSet) {
            Iterator<Animator> it = ((AnimatorSet) animator).getChildAnimations().iterator();
            while (it.hasNext()) {
                if (!finite(it.next())) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private static void rememberTree(Animator animator) {
        if (!originals.containsKey(animator)) {
            originals.put(animator, new long[]{animator.getDuration(), animator.getStartDelay()});
        }
        if (animator instanceof AnimatorSet) {
            Iterator<Animator> it = ((AnimatorSet) animator).getChildAnimations().iterator();
            while (it.hasNext()) {
                rememberTree(it.next());
            }
        }
    }

    private static void applyTree(Animator animator) {
        long[] jArr = originals.get(animator);
        animator.setStartDelay(MotionTiming.scale(jArr[1], VisualPolicy.motionScale()));
        boolean z = animator instanceof AnimatorSet;
        if (!z) {
            animator.setDuration(MotionTiming.scale(jArr[0], VisualPolicy.motionScale()));
        }
        if (z) {
            Iterator<Animator> it = ((AnimatorSet) animator).getChildAnimations().iterator();
            while (it.hasNext()) {
                applyTree(it.next());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void scale(Animator animator) {
        rememberTree(animator);
        configuring.set(true);
        try {
            applyTree(animator);
            configuring.remove();
            shortened++;
        } catch (Throwable th) {
            configuring.remove();
            throw th;
        }
    }

    private static void restoreTree(Animator animator) {
        long[] jArr = originals.get(animator);
        if (jArr != null) {
            if (jArr[0] >= 0) {
                animator.setDuration(jArr[0]);
            }
            animator.setStartDelay(jArr[1]);
        }
        if (animator instanceof AnimatorSet) {
            Iterator<Animator> it = ((AnimatorSet) animator).getChildAnimations().iterator();
            while (it.hasNext()) {
                restoreTree(it.next());
            }
        }
    }

    static long[] nativeTiming(Animator animator) {
        long[] jArr = originals.get(animator);
        return jArr == null ? new long[]{animator.getDuration(), animator.getStartDelay()} : (long[]) jArr.clone();
    }

    static void restore() {
        if (main()) {
            configuring.set(true);
            try {
                Set setNewSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
                ArrayList<Animator> arrayList = new ArrayList(originals.keySet());
                for (Animator animator : arrayList) {
                    if (animator instanceof AnimatorSet) {
                        setNewSetFromMap.addAll(((AnimatorSet) animator).getChildAnimations());
                    }
                }
                for (Animator animator2 : arrayList) {
                    if (animator2 != null && !setNewSetFromMap.contains(animator2)) {
                        restoreTree(animator2);
                    }
                }
                originals.clear();
                for (Map.Entry entry : new ArrayList(viewTimings.entrySet())) {
                    ((ViewPropertyAnimator) entry.getKey()).setDuration(((long[]) entry.getValue())[0]);
                    ((ViewPropertyAnimator) entry.getKey()).setStartDelay(((long[]) entry.getValue())[1]);
                }
                viewTimings.clear();
                for (Map.Entry entry2 : new ArrayList(tweens.entrySet())) {
                    ((Animation) entry2.getKey()).setDuration(((long[]) entry2.getValue())[0]);
                    ((Animation) entry2.getKey()).setStartOffset(((long[]) entry2.getValue())[1]);
                }
                tweens.clear();
            } finally {
                configuring.remove();
            }
        }
    }

    private static void hook(Member member, XC_MethodHook xC_MethodHook) {
        XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod(member, xC_MethodHook);
        if (unhookHookMethod != null) {
            ownedHooks.add(unhookHookMethod);
            hooks++;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r10v15 */
    /* JADX WARN: Type inference failed for: r10v8 */
    /* JADX WARN: Type inference failed for: r10v9, types: [int] */
    /* JADX WARN: Unreachable blocks removed: 2, instructions: 2 */
    static void install(ClassLoader classLoader) {
        int i;
        int i2;
        XC_MethodHook xC_MethodHook = new XC_MethodHook() { // from class: nihil.sunshine.parts.NoShadeMotion.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (NoShadeMotion.main()) {
                    int iContext = NoShadeMotion.context();
                    if (iContext == 2) {
                        NoShadeMotion.media.add((Animator) methodHookParam.thisObject);
                    } else if (iContext == 1) {
                        NoShadeMotion.classified.add((Animator) methodHookParam.thisObject);
                    }
                }
            }
        };
        Class[] clsArr = {ValueAnimator.class, AnimatorSet.class};
        boolean z = false;
        int i3 = 0;
        while (true) {
            i = 2;
            if (i3 >= 2) {
                break;
            }
            try {
                hook(clsArr[i3].getDeclaredConstructor(new Class[0]), xC_MethodHook);
            } catch (Throwable th) {
            }
            i3++;
        }
        XC_MethodHook xC_MethodHook2 = new XC_MethodHook() { // from class: nihil.sunshine.parts.NoShadeMotion.2
            /* JADX WARN: Code duplicated, block: B:16:0x003e  */
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                boolean zRegion;
                if (VisualPolicy.motionScale() == 1.0f || !NoShadeMotion.main()) {
                    return;
                }
                Animator animator = (Animator) methodHookParam.thisObject;
                if (NoShadeMotion.media.contains(animator)) {
                    NoShadeMotion.preserved++;
                    return;
                }
                if (animator instanceof ObjectAnimator) {
                    Object target = ((ObjectAnimator) animator).getTarget();
                    if (target instanceof View) {
                        zRegion = NoShadeMotion.region((View) target);
                    } else {
                        zRegion = false;
                    }
                } else {
                    zRegion = false;
                }
                if (zRegion || NoShadeMotion.classified.contains(animator) || NoShadeMotion.context() == 1) {
                    if (NoShadeMotion.context() != 2 && NoShadeMotion.finite(animator)) {
                        NoShadeMotion.scale(animator);
                    } else {
                        NoShadeMotion.preserved++;
                    }
                }
            }
        };
        Class[] clsArr2 = {ValueAnimator.class, ObjectAnimator.class, AnimatorSet.class};
        int i4 = 0;
        while (true) {
            if (i4 >= 3) {
                break;
            }
            Class cls = clsArr2[i4];
            String[] strArr = {"start", "reverse"};
            for (int i5 = 0; i5 < 2; i5++) {
                try {
                    hook(cls.getDeclaredMethod(strArr[i5], new Class[0]), xC_MethodHook2);
                } catch (Throwable th2) {
                }
            }
            i4++;
        }
        Class[] clsArr3 = {ValueAnimator.class, AnimatorSet.class};
        for (int i6 = 0; i6 < 2; i6++) {
            Class cls2 = clsArr3[i6];
            String[] strArr2 = {"setDuration", "setStartDelay"};
            for (int i7 = 0; i7 < 2; i7++) {
                try {
                    hook(cls2.getDeclaredMethod(strArr2[i7], Long.TYPE), new XC_MethodHook() { // from class: nihil.sunshine.parts.NoShadeMotion.3
                        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                            if (VisualPolicy.motionScale() != 1.0f && NoShadeMotion.main() && !Boolean.TRUE.equals(NoShadeMotion.configuring.get()) && NoShadeMotion.originals.containsKey(methodHookParam.thisObject) && !NoShadeMotion.media.contains(methodHookParam.thisObject)) {
                                ((long[]) NoShadeMotion.originals.get(methodHookParam.thisObject))[!methodHookParam.method.getName().equals("setDuration") ? 1 : 0] = ((Long) methodHookParam.args[0]).longValue();
                                if (!(methodHookParam.thisObject instanceof AnimatorSet) || methodHookParam.method.getName().equals("setStartDelay")) {
                                    methodHookParam.args[0] = Long.valueOf(MotionTiming.scale(((Long) methodHookParam.args[0]).longValue(), VisualPolicy.motionScale()));
                                }
                            }
                        }
                    });
                } catch (Throwable th3) {
                }
            }
        }
        String[] strArr3 = {"com.android.systemui.media.audiovisseekbar.renderer.track.auto.MultiWaveAreaTrackRenderer", "com.android.systemui.media.audiovisseekbar.AudioVisSeekBarProgressDrawable", "com.android.systemui.media.audiovisseekbar.utils.animator.SingleStateValueAnimator"};
        int i8 = 0;
        for (i2 = 3; i8 < i2; i2 = 3) {
            try {
                Class<?> cls3 = Class.forName(strArr3[i8], z, classLoader);
                XC_MethodHook xC_MethodHookScoped = scoped(i);
                Constructor<?>[] declaredConstructors = cls3.getDeclaredConstructors();
                int length = declaredConstructors.length;
                for (?? r10 = z; r10 < length; r10++) {
                    hook(declaredConstructors[r10], xC_MethodHookScoped);
                }
                Method[] declaredMethods = cls3.getDeclaredMethods();
                int length2 = declaredMethods.length;
                int i9 = 0;
                while (i9 < length2) {
                    Method method = declaredMethods[i9];
                    Method[] methodArr = declaredMethods;
                    if (Arrays.asList("animateTo", "setActive", "setVisible", "start", "resume", "updateAnimator", "onLayout", "onThumbLocationChanged").contains(method.getName())) {
                        hook(method, xC_MethodHookScoped);
                    }
                    i9++;
                    declaredMethods = methodArr;
                }
            } catch (Throwable th4) {
            }
            i8++;
            z = false;
            i = 2;
        }
        String[] strArr4 = {"com.android.systemui.qs.QSAnimator", "com.android.systemui.qs.TileChunkLayoutBarExpandHelper", "com.android.systemui.shade.NotificationPanelViewController", "com.android.systemui.qs.animator.LegacyQsExpandAnimator", "com.android.systemui.qs.animator.LegacyQsOpenAnimator", "com.android.systemui.qs.animator.QsExpandAnimator", "com.android.systemui.qs.animator.QsOpenAnimator", "com.android.systemui.qs.animator.PanelTransitionAnimator", "com.android.systemui.qs.animator.PanelSplitOpenAnimator", "com.android.systemui.qs.animator.QsTransitionAnimator", "com.android.systemui.qs.animator.ImmersiveScrollAnimator"};
        for (int i10 = 0; i10 < 11; i10++) {
            try {
                for (Method method2 : Class.forName(strArr4[i10], false, classLoader).getDeclaredMethods()) {
                    String name = method2.getName();
                    if (name.equals("setHeight") || name.equals("flingToHeight") || name.equals("createHeightAnimator") || name.equals("animateToFullShade") || name.equals("makeTransitionAnimator") || name.equals("showQsPanel") || name.equals("transitionDetail") || name.equals("slide") || name.equals("onPanelExpansionChanged") || name.equals("onPanelTransitionStateChanged") || name.equals("updateAnimators") || name.equals("setPosition$1") || name.equals("setPosition") || name.equals("setQsExpansionPosition") || name.equals("setPanelExpandedFraction")) {
                        hook(method2, scoped(1));
                    }
                }
            } catch (Throwable th5) {
            }
        }
        try {
            hook(View.class.getDeclaredMethod("startAnimation", Animation.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.NoShadeMotion.4
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Animation animation;
                    if (VisualPolicy.motionScale() != 1.0f && NoShadeMotion.main() && (animation = (Animation) methodHookParam.args[0]) != null && animation.getRepeatCount() != -1 && NoShadeMotion.region((View) methodHookParam.thisObject)) {
                        if (!NoShadeMotion.tweens.containsKey(animation)) {
                            NoShadeMotion.tweens.put(animation, new long[]{animation.getDuration(), animation.getStartOffset()});
                        }
                        long[] jArr = (long[]) NoShadeMotion.tweens.get(animation);
                        animation.setDuration(MotionTiming.scale(jArr[0], VisualPolicy.motionScale()));
                        animation.setStartOffset(MotionTiming.scale(jArr[1], VisualPolicy.motionScale()));
                    }
                }
            });
        } catch (Throwable th6) {
        }
        try {
            final Field declaredField = ViewPropertyAnimator.class.getDeclaredField("mView");
            declaredField.setAccessible(true);
            XC_MethodHook xC_MethodHook3 = new XC_MethodHook() { // from class: nihil.sunshine.parts.NoShadeMotion.5
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                    if (VisualPolicy.motionScale() != 1.0f && NoShadeMotion.main() && NoShadeMotion.region((View) declaredField.get(methodHookParam.thisObject))) {
                        ViewPropertyAnimator viewPropertyAnimator = (ViewPropertyAnimator) methodHookParam.thisObject;
                        if (!NoShadeMotion.viewTimings.containsKey(viewPropertyAnimator)) {
                            NoShadeMotion.viewTimings.put(viewPropertyAnimator, new long[]{viewPropertyAnimator.getDuration(), viewPropertyAnimator.getStartDelay()});
                        }
                        NoShadeMotion.configuring.set(true);
                        try {
                            long[] jArr = (long[]) NoShadeMotion.viewTimings.get(viewPropertyAnimator);
                            viewPropertyAnimator.setDuration(MotionTiming.scale(jArr[0], VisualPolicy.motionScale()));
                            viewPropertyAnimator.setStartDelay(MotionTiming.scale(jArr[1], VisualPolicy.motionScale()));
                        } finally {
                            NoShadeMotion.configuring.remove();
                        }
                    }
                }
            };
            String[] strArr5 = {"start", "startAnimation"};
            for (int i11 = 0; i11 < 2; i11++) {
                try {
                    hook(ViewPropertyAnimator.class.getDeclaredMethod(strArr5[i11], new Class[0]), xC_MethodHook3);
                } catch (Throwable th7) {
                }
            }
            String[] strArr6 = {"setDuration", "setStartDelay"};
            for (int i12 = 0; i12 < 2; i12++) {
                hook(ViewPropertyAnimator.class.getDeclaredMethod(strArr6[i12], Long.TYPE), new XC_MethodHook() { // from class: nihil.sunshine.parts.NoShadeMotion.6
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        long[] jArr;
                        if (VisualPolicy.motionScale() != 1.0f && NoShadeMotion.main() && !Boolean.TRUE.equals(NoShadeMotion.configuring.get()) && (jArr = (long[]) NoShadeMotion.viewTimings.get(methodHookParam.thisObject)) != null) {
                            jArr[!methodHookParam.method.getName().equals("setDuration") ? 1 : 0] = ((Long) methodHookParam.args[0]).longValue();
                            methodHookParam.args[0] = Long.valueOf(MotionTiming.scale(((Long) methodHookParam.args[0]).longValue(), VisualPolicy.motionScale()));
                        }
                    }
                });
            }
            ControlBus.feature("view_panel_motion", "ready");
        } catch (Throwable th8) {
            ControlBus.feature("view_panel_motion", "unavailable");
        }
        ControlBus.feature("panel_motion", hooks <= 0 ? "unavailable" : "ready");
        Log.i("SunshinePartsUI", "Media clocks preserved; scoped QS motion ready; no stack inspection");
    }

    private static XC_MethodHook scoped(final int i) {
        return new XC_MethodHook(10000) { // from class: nihil.sunshine.parts.NoShadeMotion.7
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                ((ArrayDeque) NoShadeMotion.previousScopes.get()).push(Integer.valueOf(NoShadeMotion.context()));
                if (NoShadeMotion.context() != 2) {
                    NoShadeMotion.scope.set(Integer.valueOf(i));
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                ArrayDeque arrayDeque = (ArrayDeque) NoShadeMotion.previousScopes.get();
                NoShadeMotion.scope.set(Integer.valueOf(arrayDeque.isEmpty() ? 0 : ((Integer) arrayDeque.pop()).intValue()));
            }
        };
    }
}
