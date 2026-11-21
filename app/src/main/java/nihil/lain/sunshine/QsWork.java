package nihil.lain.sunshine;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
final class QsWork {
    static final String ANIMATOR = "com.android.systemui.qs.animator.LegacyQsExpandAnimator";
    static final String JOB = "com.android.systemui.qs.animator.LegacyQsExpandAnimator$$ExternalSyntheticLambda1";
    private volatile boolean enabled;
    private boolean failed;
    private final Field owner;
    private final Field panel;
    private final Method rebuild;
    private final Method run;
    private long skipped;
    private final Field updateJob;
    private final Cost rebuildCost = new Cost("qs-rebuild");
    private final Cost captureCost = new Cost("qs-capture");

    static /* synthetic */ long access$508(QsWork qsWork) {
        long j = qsWork.skipped;
        qsWork.skipped = 1 + j;
        return j;
    }

    QsWork(ClassLoader classLoader, boolean z) throws Exception {
        if (!z) {
            throw new IllegalArgumentException("Unverified QS DEX");
        }
        Class<?> cls = Class.forName(ANIMATOR, false, classLoader);
        Class<?> cls2 = Class.forName(JOB, false, classLoader);
        Field field = SunshineModule.field(cls2, "f$0");
        this.owner = field;
        Field field2 = SunshineModule.field(cls, "mQsPanel");
        this.panel = field2;
        Field field3 = SunshineModule.field(cls, "mUpdateAnimators");
        this.updateJob = field3;
        if (field.getType() == cls && field3.getType() == cls2 && Runnable.class.isAssignableFrom(cls2) && View.class.isAssignableFrom(field2.getType())) {
            Method method = SunshineModule.method(classLoader, JOB, "run", new Class[0]);
            this.run = method;
            Method method2 = SunshineModule.method(classLoader, ANIMATOR, "updateAnimators", new Class[0]);
            this.rebuild = method2;
            if (method.getReturnType() != Void.TYPE || method2.getReturnType() != Void.TYPE) {
                throw new NoSuchMethodException("QS return types");
            }
            return;
        }
        throw new NoSuchFieldException("QS job contract");
    }

    void install() {
        RuntimeHooks.hookMethod(this.run, new XC_MethodHook() { // from class: nihil.lain.sunshine.QsWork.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (QsWork.this.enabled && !QsWork.this.failed && Looper.myLooper() == Looper.getMainLooper()) {
                    try {
                        Object obj = QsWork.this.owner.get(methodHookParam.thisObject);
                        if (obj != null && QsWork.this.updateJob.get(obj) == methodHookParam.thisObject) {
                            View view = (View) QsWork.this.panel.get(obj);
                            Handler handler = view == null ? null : view.getHandler();
                            if (handler == null || !handler.hasCallbacks((Runnable) methodHookParam.thisObject)) {
                                return;
                            }
                            QsWork.access$508(QsWork.this);
                            methodHookParam.setResult((Object) null);
                        }
                    } catch (Throwable th) {
                        QsWork.this.failed = true;
                        SunshineModule.diagnostic("qs-job desativado: " + th.getClass().getSimpleName());
                    }
                }
            }
        });
        RuntimeHooks.hookMethod(this.rebuild, timer(this.rebuildCost, null));
        this.enabled = true;
    }

    void installCaptureTimer(ClassLoader classLoader) throws Exception {
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
        Method method = SunshineModule.method(classLoader, "com.android.systemui.blur.domain.interactor.SecCapturedBlurBitmapGenerator", "getBlurredBitmapWithEffect", cls);
        if (!method.getReturnType().getName().equals("android.graphics.drawable.BitmapDrawable")) {
            throw new NoSuchMethodException("capture return type");
        }
        RuntimeHooks.hookMethod(method, timer(this.captureCost, obj));
    }

    private XC_MethodHook timer(final Cost cost, final Object obj) {
        return new XC_MethodHook() { // from class: nihil.lain.sunshine.QsWork.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (QsWork.this.enabled) {
                    if (obj == null || methodHookParam.args[0] == obj) {
                        try {
                            methodHookParam.setObjectExtra("sunshine.qs.cost", new Stamp());
                        } catch (Throwable unused) {
                        }
                    }
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra("sunshine.qs.cost");
                if (objectExtra instanceof Stamp) {
                    try {
                        cost.record((Stamp) objectExtra, methodHookParam.hasThrowable());
                    } catch (Throwable unused) {
                    }
                }
            }
        };
    }

    static final class Stamp {
        final boolean main;
        final long wall = SystemClock.elapsedRealtimeNanos();
        final long cpu = SystemClock.currentThreadTimeMillis();

        Stamp() {
            this.main = Looper.myLooper() == Looper.getMainLooper();
        }
    }

    static final class Cost {
        long count;
        long lastLog;
        long maxNs;
        final String name;
        long totalNs;

        Cost(String str) {
            this.name = str;
        }

        synchronized void record(Stamp stamp, boolean z) {
            long jMax = Math.max(0L, SystemClock.elapsedRealtimeNanos() - stamp.wall);
            long jMax2 = Math.max(0L, SystemClock.currentThreadTimeMillis() - stamp.cpu);
            this.count++;
            this.totalNs += jMax;
            this.maxNs = Math.max(this.maxNs, jMax);
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (this.count <= 3 || (jMax >= 8000000 && jUptimeMillis - this.lastLog >= 1500)) {
                this.lastLog = jUptimeMillis;
                SunshineModule.diagnostic(this.name + " wallMs=" + (jMax / 1000000.0d) + " cpuMs=" + jMax2 + " main=" + stamp.main + " erro=" + z + " chamadas=" + this.count);
            }
        }

        synchronized String report() {
            StringBuilder sb;
            sb = new StringBuilder();
            sb.append(this.name);
            sb.append(" chamadas=");
            sb.append(this.count);
            sb.append(" maxMs=");
            sb.append(this.maxNs / 1000000.0d);
            sb.append(" mediaMs=");
            long j = this.count;
            sb.append(j == 0 ? 0.0d : (this.totalNs / j) / 1000000.0d);
            return sb.toString();
        }
    }

    String report() {
        return "qsJobsAgrupados=" + this.skipped + " qsJobFalha=" + this.failed + "; " + this.rebuildCost.report() + "; " + this.captureCost.report();
    }
}
