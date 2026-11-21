package nihil.lain.sunshine;

import android.content.res.Configuration;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
final class ComposeHost {
    static final String CONTROLLER = "com.android.systemui.samsung.quicksetting.SecQSPanelCompose";
    static final String ROOT = "com.android.systemui.samsung.quicksetting.view.SecQSPanelComposeRoot";
    final Method attach;
    long calls;
    final Class<?> composeClass;
    long created;
    final Method dispose;
    boolean failed;
    long removed;
    long reused;
    final Class<?> rootClass;
    final Field viewField;
    final WeakHashMap<Object, Record> owners = new WeakHashMap<>();
    WeakReference<ViewGroup> current = new WeakReference<>(null);

    static final class Record {
        final ArrayList<WeakReference<View>> children = new ArrayList<>();
        Configuration config;
        final WeakReference<ViewGroup> root;

        Record(ViewGroup viewGroup) {
            this.root = new WeakReference<>(viewGroup);
            this.config = new Configuration(viewGroup.getResources().getConfiguration());
        }
    }

    static final class Attempt {
        final Set<View> before;
        final ViewGroup root;
        boolean skip;

        Attempt(ViewGroup viewGroup, Set<View> set) {
            this.root = viewGroup;
            this.before = set;
        }
    }

    ComposeHost(ClassLoader classLoader, boolean z) throws Exception {
        if (!z) {
            throw new IllegalArgumentException("unknown DEX");
        }
        Class<?> cls = Class.forName(ROOT, false, classLoader);
        this.rootClass = cls;
        Class<?> cls2 = Class.forName("androidx.compose.ui.platform.ComposeView", false, classLoader);
        this.composeClass = cls2;
        Field field = SunshineModule.field(Class.forName(CONTROLLER, false, classLoader), "view");
        this.viewField = field;
        if (field.getType() == cls && ViewGroup.class.isAssignableFrom(cls)) {
            Method method = SunshineModule.method(classLoader, CONTROLLER, "attachComposeView", new Class[0]);
            this.attach = method;
            if (method.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("attach return");
            }
            this.dispose = cls2.getMethod("disposeComposition", null);
            return;
        }
        throw new NoSuchFieldException("view contract");
    }

    void install() {
        RuntimeHooks.hookMethod(this.attach, new XC_MethodHook() { // from class: nihil.lain.sunshine.ComposeHost.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                try {
                    Attempt attemptBefore = ComposeHost.this.before(methodHookParam.thisObject);
                    methodHookParam.setObjectExtra("sunshine.composeHost", attemptBefore);
                    if (attemptBefore == null || !attemptBefore.skip) {
                        return;
                    }
                    methodHookParam.setResult((Object) null);
                } catch (Throwable th) {
                    ComposeHost.this.fail(th);
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (methodHookParam.hasThrowable()) {
                    return;
                }
                try {
                    ComposeHost.this.after(methodHookParam.thisObject, (Attempt) methodHookParam.getObjectExtra("sunshine.composeHost"));
                } catch (Throwable th) {
                    ComposeHost.this.fail(th);
                }
            }
        });
    }

    Attempt before(Object obj) throws Exception {
        Object obj2;
        if (this.failed || Looper.myLooper() != Looper.getMainLooper() || (obj2 = this.viewField.get(obj)) == null || obj2.getClass() != this.rootClass) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) obj2;
        this.current = new WeakReference<>(viewGroup);
        this.calls++;
        Record record = this.owners.get(obj);
        if (RuntimeOptions.enabled(4) && record != null && record.root.get() == viewGroup && record.config.equals(viewGroup.getResources().getConfiguration())) {
            Iterator<WeakReference<View>> it = record.children.iterator();
            View view = null;
            while (it.hasNext()) {
                View view2 = it.next().get();
                if (view2 != null && view2.getParent() == viewGroup && view2.getClass() == this.composeClass) {
                    view = view2;
                }
            }
            if (view != null) {
                prune(record, viewGroup, view);
                if (viewGroup.getVisibility() != 0) {
                    viewGroup.setVisibility(0);
                }
                Attempt attempt = new Attempt(viewGroup, null);
                attempt.skip = true;
                this.reused++;
                return attempt;
            }
        }
        Set setNewSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            setNewSetFromMap.add(viewGroup.getChildAt(i));
        }
        return new Attempt(viewGroup, setNewSetFromMap);
    }

    void after(Object obj, Attempt attempt) throws Exception {
        if (this.failed || attempt == null || attempt.skip) {
            return;
        }
        View view = null;
        int i = 0;
        for (int i2 = 0; i2 < attempt.root.getChildCount(); i2++) {
            View childAt = attempt.root.getChildAt(i2);
            if (!attempt.before.contains(childAt) && childAt.getClass() == this.composeClass) {
                i++;
                view = childAt;
            }
        }
        if (i != 1) {
            return;
        }
        Record record = this.owners.get(obj);
        if (record == null || record.root.get() != attempt.root) {
            record = new Record(attempt.root);
            this.owners.put(obj, record);
        }
        record.children.add(new WeakReference<>(view));
        this.created++;
        record.config = new Configuration(attempt.root.getResources().getConfiguration());
        if (RuntimeOptions.enabled(4)) {
            prune(record, attempt.root, view);
        }
    }

    void prune(Record record, ViewGroup viewGroup, View view) throws Exception {
        Iterator<WeakReference<View>> it = record.children.iterator();
        while (it.hasNext()) {
            View view2 = it.next().get();
            if (view2 != view) {
                it.remove();
                if (view2 != null && view2.getParent() == viewGroup && view2.getClass() == this.composeClass) {
                    viewGroup.removeView(view2);
                    this.dispose.invoke(view2, null);
                    this.removed++;
                }
            }
        }
    }

    void fail(Throwable th) {
        if (!this.failed) {
            SunshineModule.diagnostic("qs-compose-host original apos erro: " + th.getClass().getSimpleName());
        }
        this.failed = true;
    }

    static int countViews(View view) {
        if (view == null) {
            return 0;
        }
        ArrayDeque arrayDeque = new ArrayDeque();
        arrayDeque.add(view);
        int i = 0;
        while (!arrayDeque.isEmpty() && i < 15000) {
            View view2 = (View) arrayDeque.removeFirst();
            i++;
            if (view2 instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view2;
                for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
                    arrayDeque.addLast(viewGroup.getChildAt(i2));
                }
            }
        }
        return i;
    }

    String report() {
        int i;
        int iCountViews;
        int iCountViews2;
        ViewGroup viewGroup = this.current.get();
        if (viewGroup == null || Looper.myLooper() != Looper.getMainLooper()) {
            i = -1;
            iCountViews = -1;
            iCountViews2 = -1;
        } else {
            i = 0;
            for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
                if (viewGroup.getChildAt(i2).getClass() == this.composeClass) {
                    i++;
                }
            }
            iCountViews2 = countViews(viewGroup);
            iCountViews = countViews(viewGroup.getRootView());
        }
        return "composeHostChamadas=" + this.calls + " composeHostsCriados=" + this.created + " composeHostsReusados=" + this.reused + " composeHostsRemovidos=" + this.removed + " qsHosts=" + i + " qsViews=" + iCountViews2 + " shadeViews=" + iCountViews + " composeHostFalha=" + this.failed;
    }
}
