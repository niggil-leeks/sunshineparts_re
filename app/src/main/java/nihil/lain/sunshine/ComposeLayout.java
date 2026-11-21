package nihil.lain.sunshine;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.WeakHashMap;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
final class ComposeLayout {
    static final String LISTENER = "com.android.systemui.shade.NotificationsQSContainerController$onInit$2";
    static final String OWNER = "com.android.systemui.shade.NotificationsQSContainerController";
    final Field ambient;
    final Field ambientHeight;
    long blockedCalls;
    final Field blurTop;
    final Field compose;
    final Field[] dimensions;
    boolean enabled;
    final Field extContext;
    final Field extLazy;
    final Field extModel;
    boolean failed;
    final Field header;
    final Field headerController;
    final Field helperContext;
    final Field helperLazy;
    final Method isPopOver;
    final Field large;
    final Method lazyGet;
    final Method listener;
    long listenerCalls;
    final Field listenerOwner;
    long maxUpdateNs;
    final Field[] paddingFields;
    final Field popManager;
    final Field root;
    long skipped;
    long slowUpdates;
    long supportedCalls;
    final Method update;
    long updates;
    String mode = "unknown";
    final WeakHashMap<Object, Stamp> stamps = new WeakHashMap<>();

    ComposeLayout(ClassLoader classLoader, boolean z) throws Exception {
        if (!z) {
            throw new IllegalArgumentException("Unverified QS DEX");
        }
        Method method = SunshineModule.method(classLoader, OWNER, "updateConstraints$1", new Class[0]);
        this.update = method;
        Method method2 = SunshineModule.method(classLoader, LISTENER, "onLayoutChange", View.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
        this.listener = method2;
        this.listenerOwner = SunshineModule.field(method2.getDeclaringClass(), "this$0");
        Class<?> declaringClass = method.getDeclaringClass();
        this.root = SunshineModule.field(Class.forName("com.android.systemui.util.ViewController", false, classLoader), "mView");
        Field field = SunshineModule.field(declaringClass, "shadeHeaderController");
        this.headerController = field;
        this.header = SunshineModule.field(field.getType(), "header");
        this.compose = SunshineModule.field(Class.forName("com.android.systemui.QpRune", false, classLoader), "QUICK_PANEL_COMPOSE");
        this.large = SunshineModule.field(declaringClass, "largeScreenShadeHeaderActive");
        String[] strArr = {"largeScreenShadeHeaderHeight", "shadeHeaderHeight", "panelMarginHorizontal"};
        this.dimensions = new Field[3];
        for (int i = 0; i < 3; i++) {
            this.dimensions[i] = SunshineModule.field(declaringClass, strArr[i]);
            if (this.dimensions[i].getType() != Integer.TYPE) {
                throw new NoSuchFieldException(strArr[i]);
            }
        }
        this.helperLazy = SunshineModule.field(declaringClass, "largeScreenHeaderHelperLazy");
        this.lazyGet = Class.forName("dagger.Lazy", false, classLoader).getMethod("get", null);
        this.helperContext = SunshineModule.field(Class.forName("com.android.systemui.shade.LargeScreenHeaderHelper", false, classLoader), "context");
        this.extLazy = SunshineModule.field(this.headerController.getType(), "samsungExt");
        Class<?> cls = Class.forName("com.android.systemui.shade.SamsungShadeHeaderControllerExt", false, classLoader);
        this.extContext = SunshineModule.field(cls, "context");
        this.extModel = SunshineModule.field(cls, "model");
        this.isPopOver = SunshineModule.method(classLoader, "com.android.systemui.util.DeviceState", "isShowingPopOverStatusBar", Context.class);
        Field field2 = SunshineModule.field(declaringClass, "panelPopOverManager");
        this.popManager = field2;
        this.blurTop = SunshineModule.field(field2.getType(), "blurViewTopMargin");
        Field field3 = SunshineModule.field(declaringClass, "ambientState");
        this.ambient = field3;
        this.ambientHeight = SunshineModule.field(field3.getType(), "mLargeScreenShadeHeaderHeight");
        String[] strArr2 = {"leftPadding", "topPadding", "rightPadding", "bottomPadding", "cameraTopPadding", "cameraBottomPadding"};
        this.paddingFields = new Field[6];
        for (int i2 = 0; i2 < 6; i2++) {
            this.paddingFields[i2] = SunshineModule.field(this.extModel.getType(), strArr2[i2]);
            if (this.paddingFields[i2].getType() != Integer.TYPE) {
                throw new NoSuchFieldException(strArr2[i2]);
            }
        }
        if (this.isPopOver.getReturnType() != Boolean.TYPE || this.blurTop.getType() != Integer.TYPE || this.ambientHeight.getType() != Integer.TYPE) {
            throw new NoSuchFieldException("large header contract");
        }
        if (this.update.getReturnType() != Void.TYPE || this.listener.getReturnType() != Void.TYPE || this.listenerOwner.getType() != declaringClass || this.compose.getType() != Boolean.TYPE || this.large.getType() != Boolean.TYPE || !View.class.isAssignableFrom(this.header.getType()) || !View.class.isAssignableFrom(this.root.getType())) {
            throw new NoSuchFieldException("compose layout contract");
        }
    }

    void install() {
        RuntimeHooks.hookMethod(this.update, new XC_MethodHook() { // from class: nihil.lain.sunshine.ComposeLayout.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (ComposeLayout.this.enabled && !ComposeLayout.this.failed && ComposeLayout.this.main()) {
                    ComposeLayout.this.stamps.remove(methodHookParam.thisObject);
                    methodHookParam.setObjectExtra("sunshine.constraints.start", Long.valueOf(System.nanoTime()));
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra("sunshine.constraints.start");
                if (objectExtra instanceof Long) {
                    long jNanoTime = System.nanoTime() - ((Long) objectExtra).longValue();
                    ComposeLayout.this.updates++;
                    ComposeLayout composeLayout = ComposeLayout.this;
                    composeLayout.maxUpdateNs = Math.max(composeLayout.maxUpdateNs, jNanoTime);
                    if (jNanoTime >= 8000000) {
                        ComposeLayout.this.slowUpdates++;
                    }
                    if (methodHookParam.hasThrowable() || !ComposeLayout.this.canRun()) {
                        return;
                    }
                    try {
                        if (ComposeLayout.this.supported(methodHookParam.thisObject)) {
                            ComposeLayout.this.stamps.put(methodHookParam.thisObject, ComposeLayout.this.new Stamp(methodHookParam.thisObject));
                        }
                    } catch (Throwable th) {
                        ComposeLayout.this.fail(th);
                    }
                }
            }
        });
        RuntimeHooks.hookMethod(this.listener, new XC_MethodHook() { // from class: nihil.lain.sunshine.ComposeLayout.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (ComposeLayout.this.enabled && !ComposeLayout.this.failed && ComposeLayout.this.main()) {
                    ComposeLayout.this.listenerCalls++;
                    if (RuntimeOptions.enabled(3)) {
                        try {
                            Object obj = ComposeLayout.this.listenerOwner.get(methodHookParam.thisObject);
                            if (!ComposeLayout.this.supported(obj)) {
                                ComposeLayout.this.blockedCalls++;
                                return;
                            }
                            ComposeLayout.this.supportedCalls++;
                            Stamp stamp = ComposeLayout.this.stamps.get(obj);
                            if (stamp == null || !stamp.matches(obj, (View) methodHookParam.args[0])) {
                                return;
                            }
                            ComposeLayout.this.skipped++;
                            methodHookParam.setResult((Object) null);
                            if (ComposeLayout.this.skipped == 1 || ComposeLayout.this.skipped % 32 == 0) {
                                SunshineModule.diagnostic("qs-compose-layout: " + ComposeLayout.this.report());
                            }
                        } catch (Throwable th) {
                            ComposeLayout.this.fail(th);
                        }
                    }
                }
            }
        });
        this.enabled = true;
    }

    boolean main() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    boolean canRun() {
        return this.enabled && !this.failed && main() && RuntimeOptions.enabled(3);
    }

    boolean supported(Object obj) throws Exception {
        if (!this.compose.getBoolean(null) || !(this.root.get(obj) instanceof ViewGroup)) {
            this.mode = "legacy";
            return false;
        }
        if (this.large.getBoolean(obj)) {
            Object objInvoke = this.lazyGet.invoke(this.helperLazy.get(obj), null);
            Object objExtension = extension(obj);
            if (Boolean.TRUE.equals(this.isPopOver.invoke(null, this.helperContext.get(objInvoke))) || Boolean.TRUE.equals(this.isPopOver.invoke(null, this.extContext.get(objExtension)))) {
                this.mode = "popover";
                return false;
            }
            this.mode = "large";
            return true;
        }
        this.mode = "compact";
        return true;
    }

    Object extension(Object obj) throws Exception {
        return this.lazyGet.invoke(this.extLazy.get(this.headerController.get(obj)), null);
    }

    void fail(Throwable th) {
        if (this.failed) {
            return;
        }
        this.failed = true;
        this.stamps.clear();
        SunshineModule.diagnostic("qs-compose-layout desativado: " + th.getClass().getSimpleName());
    }

    String report() {
        return "composeLayoutChamadas=" + this.listenerCalls + " composeLayoutEvitados=" + this.skipped + " composeLayoutElegiveis=" + this.supportedCalls + " composeLayoutBarrados=" + this.blockedCalls + " composeLayoutModo=" + this.mode + " constraintsAplicadas=" + this.updates + " constraintsLentas=" + this.slowUpdates + " constraintsMaxMs=" + (this.maxUpdateNs / 1000000.0d) + " composeLayoutFalha=" + this.failed;
    }

    final class Stamp {
        final Child[] children;
        final Configuration configuration;
        final WeakReference<ViewGroup> container;
        final int headerHeight;
        final WeakReference<View> headerView;
        final int headerWidth;
        final int[] inputs;
        final Object insets;
        final LargeStamp largeStamp;
        final int rootHeight;
        final int rootWidth;

        Stamp(Object obj) throws Exception {
            ViewGroup viewGroup = (ViewGroup) ComposeLayout.this.root.get(obj);
            View view = (View) ComposeLayout.this.header.get(ComposeLayout.this.headerController.get(obj));
            this.container = new WeakReference<>(viewGroup);
            this.headerView = new WeakReference<>(view);
            this.configuration = new Configuration(viewGroup.getResources().getConfiguration());
            this.insets = viewGroup.getRootWindowInsets();
            this.rootWidth = viewGroup.getWidth();
            this.rootHeight = viewGroup.getHeight();
            this.headerWidth = view.getMeasuredWidth();
            this.headerHeight = view.getMeasuredHeight();
            this.largeStamp = ComposeLayout.this.large.getBoolean(obj) ? ComposeLayout.this.new LargeStamp(obj, view) : null;
            this.inputs = new int[ComposeLayout.this.dimensions.length];
            int i = 0;
            int i2 = 0;
            while (true) {
                int[] iArr = this.inputs;
                if (i2 >= iArr.length) {
                    break;
                }
                iArr[i2] = ComposeLayout.this.dimensions[i2].getInt(obj);
                i2++;
            }
            this.children = new Child[viewGroup.getChildCount()];
            while (true) {
                Child[] childArr = this.children;
                if (i >= childArr.length) {
                    return;
                }
                childArr[i] = new Child(viewGroup.getChildAt(i));
                i++;
            }
        }

        boolean matches(Object obj, View view) throws Exception {
            ViewGroup viewGroup = (ViewGroup) ComposeLayout.this.root.get(obj);
            View view2 = (View) ComposeLayout.this.header.get(ComposeLayout.this.headerController.get(obj));
            if (viewGroup == this.container.get() && view2 == this.headerView.get() && view == view2 && viewGroup.getWidth() == this.rootWidth) {
                int height = viewGroup.getHeight();
                int i = this.rootHeight;
                if (height == i && this.rootWidth > 0 && i > 0 && this.headerHeight > 0 && view2.getMeasuredWidth() == this.headerWidth && view2.getMeasuredHeight() == this.headerHeight && Objects.equals(viewGroup.getRootWindowInsets(), this.insets) && this.configuration.equals(viewGroup.getResources().getConfiguration()) && viewGroup.getChildCount() == this.children.length) {
                    boolean z = ComposeLayout.this.large.getBoolean(obj);
                    LargeStamp largeStamp = this.largeStamp;
                    if (z != (largeStamp != null)) {
                        return false;
                    }
                    if (largeStamp != null && !largeStamp.matches(obj, view2)) {
                        return false;
                    }
                    for (int i2 = 0; i2 < this.inputs.length; i2++) {
                        if (ComposeLayout.this.dimensions[i2].getInt(obj) != this.inputs[i2]) {
                            return false;
                        }
                    }
                    int i3 = 0;
                    while (true) {
                        Child[] childArr = this.children;
                        if (i3 >= childArr.length) {
                            return true;
                        }
                        if (!childArr[i3].matches(viewGroup.getChildAt(i3))) {
                            return false;
                        }
                        i3++;
                    }
                }
            }
            return false;
        }
    }

    final class LargeStamp {
        final int ambientValue;
        final int blur;
        final int bottom;
        final WeakReference<Object> extRef;
        final int left;
        final WeakReference<Object> modelRef;
        final int[] pads;
        final int right;
        final int top;

        LargeStamp(Object obj, View view) throws Exception {
            Object objExtension = ComposeLayout.this.extension(obj);
            Object obj2 = ComposeLayout.this.extModel.get(objExtension);
            this.extRef = new WeakReference<>(objExtension);
            this.modelRef = new WeakReference<>(obj2);
            this.pads = new int[ComposeLayout.this.paddingFields.length];
            int i = 0;
            while (true) {
                int[] iArr = this.pads;
                if (i >= iArr.length) {
                    this.left = view.getPaddingLeft();
                    this.top = view.getPaddingTop();
                    this.right = view.getPaddingRight();
                    this.bottom = view.getPaddingBottom();
                    this.blur = ComposeLayout.this.blurTop.getInt(ComposeLayout.this.popManager.get(obj));
                    this.ambientValue = ComposeLayout.this.ambientHeight.getInt(ComposeLayout.this.ambient.get(obj));
                    return;
                }
                iArr[i] = ComposeLayout.this.paddingFields[i].getInt(obj2);
                i++;
            }
        }

        boolean matches(Object obj, View view) throws Exception {
            Object objExtension = ComposeLayout.this.extension(obj);
            Object obj2 = ComposeLayout.this.extModel.get(objExtension);
            if (objExtension != this.extRef.get() || obj2 != this.modelRef.get() || view.getPaddingLeft() != this.left || view.getPaddingTop() != this.top || view.getPaddingRight() != this.right || view.getPaddingBottom() != this.bottom || ComposeLayout.this.blurTop.getInt(ComposeLayout.this.popManager.get(obj)) != this.blur || ComposeLayout.this.ambientHeight.getInt(ComposeLayout.this.ambient.get(obj)) != this.ambientValue) {
                return false;
            }
            for (int i = 0; i < this.pads.length; i++) {
                if (ComposeLayout.this.paddingFields[i].getInt(obj2) != this.pads[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    static final class Child {
        final int bottom;
        final int end;
        final int endLink;
        final Field endToEnd;
        final int height;
        final int id;
        final int left;
        final WeakReference<ViewGroup.LayoutParams> params;
        final int right;
        final int start;
        final int top;
        final WeakReference<View> view;
        final int visibility;
        final int width;

        Child(View view) throws Exception {
            Field field;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams == null) {
                throw new IllegalArgumentException("null layout params");
            }
            this.view = new WeakReference<>(view);
            this.params = new WeakReference<>(layoutParams);
            Class<?> superclass = layoutParams.getClass();
            while (true) {
                if (superclass == null) {
                    field = null;
                    break;
                } else if (!"androidx.constraintlayout.widget.ConstraintLayout$LayoutParams".equals(superclass.getName())) {
                    superclass = superclass.getSuperclass();
                } else {
                    field = SunshineModule.field(superclass, "endToEnd");
                    break;
                }
            }
            this.endToEnd = field;
            this.endLink = field == null ? 0 : field.getInt(layoutParams);
            this.id = view.getId();
            this.visibility = view.getVisibility();
            this.width = layoutParams.width;
            this.height = layoutParams.height;
            ViewGroup.MarginLayoutParams marginLayoutParams = layoutParams instanceof ViewGroup.MarginLayoutParams ? (ViewGroup.MarginLayoutParams) layoutParams : null;
            this.top = marginLayoutParams == null ? 0 : marginLayoutParams.topMargin;
            this.bottom = marginLayoutParams == null ? 0 : marginLayoutParams.bottomMargin;
            this.left = marginLayoutParams == null ? 0 : marginLayoutParams.leftMargin;
            this.right = marginLayoutParams == null ? 0 : marginLayoutParams.rightMargin;
            this.start = marginLayoutParams == null ? 0 : marginLayoutParams.getMarginStart();
            this.end = marginLayoutParams != null ? marginLayoutParams.getMarginEnd() : 0;
        }

        boolean matches(View view) throws Exception {
            Field field;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (view != this.view.get() || layoutParams == null || layoutParams != this.params.get() || this.id != view.getId() || this.visibility != view.getVisibility() || this.width != layoutParams.width || this.height != layoutParams.height || ((field = this.endToEnd) != null && field.getInt(layoutParams) != this.endLink)) {
                return false;
            }
            if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
                return true;
            }
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            return this.top == marginLayoutParams.topMargin && this.bottom == marginLayoutParams.bottomMargin && this.left == marginLayoutParams.leftMargin && this.right == marginLayoutParams.rightMargin && this.start == marginLayoutParams.getMarginStart() && this.end == marginLayoutParams.getMarginEnd();
        }
    }
}
