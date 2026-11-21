package nihil.lain.sunshine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes2.dex */
final class ShadeRate {
    final Method apply;
    final Field bouncer;
    long composeVotes;
    Session current;
    final Field deferField;
    final Field dozing;
    final Field dreaming;
    final Field expanded;
    boolean failed;
    final Field keyguard;
    long leases;
    long lockscreenLeases;
    final Field lpField;
    final Field maxRateField;
    final Field minRateField;
    long policyReads;
    long restores;
    final Field rootField;
    final WeakHashMap<Object, Session> sessions = new WeakHashMap<>();
    final Field stateField;
    final Field visible;

    ShadeRate(Class<?> cls) throws Exception {
        this.rootField = SunshineModule.field(cls, "mWindowRootView");
        Field field = SunshineModule.field(cls, "mCurrentState");
        this.stateField = field;
        this.lpField = SunshineModule.field(cls, "mLpChanged");
        this.deferField = SunshineModule.field(cls, "mDeferWindowLayoutParams");
        this.minRateField = SunshineModule.field(WindowManager.LayoutParams.class, "preferredMinDisplayRefreshRate");
        this.maxRateField = SunshineModule.field(WindowManager.LayoutParams.class, "preferredMaxDisplayRefreshRate");
        Class<?> type = field.getType();
        this.visible = SunshineModule.field(type, "panelVisible");
        this.expanded = SunshineModule.field(type, "shadeOrQsExpanded");
        this.dozing = SunshineModule.field(type, "dozing");
        this.keyguard = SunshineModule.field(type, "keyguardShowing");
        this.bouncer = SunshineModule.field(type, "bouncerShowing");
        this.dreaming = SunshineModule.field(type, "dreaming");
        Method declaredMethod = cls.getDeclaredMethod("applyWindowLayoutParams", null);
        this.apply = declaredMethod;
        declaredMethod.setAccessible(true);
    }

    final class Session implements Runnable, ViewTreeObserver.OnDrawListener {
        boolean applying;
        float downX;
        float downY;
        long drawCount;
        long intervalCount;
        long intervalSumNs;
        long lastDrawNs;
        float lastTarget;
        boolean listenerAttached;
        boolean lockscreenDrag;
        long maxIntervalNs;
        final WeakReference<Object> owner;
        boolean panelWasVisible;
        float peak;
        long policyAt;
        boolean powerSave;
        boolean queued;
        final WeakReference<View> root;
        long slowIntervals;
        boolean standardMode;
        float target;
        final int touchSlop;
        boolean trackingTouch;
        long until;
        boolean voted;
        final Handler handler = new Handler(Looper.getMainLooper());
        final WeakHashMap<View, Float> votes = new WeakHashMap<>();

        Session(Object obj, View view) {
            this.owner = new WeakReference<>(obj);
            this.root = new WeakReference<>(view);
            this.touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        }

        void renew() {
            if (ShadeRate.this.failed || !RuntimeOptions.enabled(5)) {
                return;
            }
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (this.until <= jUptimeMillis) {
                this.lastDrawNs = 0L;
                this.slowIntervals = 0L;
                this.maxIntervalNs = 0L;
                this.intervalSumNs = 0L;
                this.intervalCount = 0L;
                this.drawCount = 0L;
                ShadeRate.this.refreshPolicy(this, false);
                if (this.target <= 0.0f) {
                    return;
                }
                ShadeRate.this.leases++;
                if (this.lockscreenDrag) {
                    ShadeRate.this.lockscreenLeases++;
                }
            }
            this.until = jUptimeMillis + 900;
            if (this.queued) {
                return;
            }
            this.queued = true;
            this.handler.postDelayed(this, 900L);
        }

        @Override // java.lang.Runnable
        public void run() {
            this.queued = false;
            long jUptimeMillis = this.until - SystemClock.uptimeMillis();
            if (!ShadeRate.this.failed && jUptimeMillis > 0) {
                this.queued = true;
                this.handler.postDelayed(this, jUptimeMillis);
            } else {
                ShadeRate.this.release(this);
            }
        }

        @Override // android.view.ViewTreeObserver.OnDrawListener
        public void onDraw() {
            if (ShadeRate.this.failed || !RuntimeOptions.enabled(5) || this.target <= 0.0f || this.until <= SystemClock.uptimeMillis()) {
                return;
            }
            long jNanoTime = System.nanoTime();
            long j = this.lastDrawNs;
            if (j != 0) {
                long j2 = jNanoTime - j;
                this.intervalSumNs += j2;
                this.intervalCount++;
                this.maxIntervalNs = Math.max(this.maxIntervalNs, j2);
                if (j2 > ((long) (1.5E9d / ((double) this.target)))) {
                    this.slowIntervals++;
                }
            }
            this.lastDrawNs = jNanoTime;
            this.drawCount++;
        }
    }

    static final class Snapshot {
        final WindowManager.LayoutParams lp;
        final float max;
        final Field maxField;
        final float min;
        final Field minField;
        final float preferred;

        Snapshot(WindowManager.LayoutParams layoutParams, Field field, Field field2) throws IllegalAccessException {
            this.lp = layoutParams;
            this.minField = field;
            this.maxField = field2;
            this.min = field.getFloat(layoutParams);
            this.max = field2.getFloat(layoutParams);
            this.preferred = layoutParams.preferredRefreshRate;
        }

        void restore() throws IllegalAccessException {
            this.minField.setFloat(this.lp, this.min);
            this.maxField.setFloat(this.lp, this.max);
            this.lp.preferredRefreshRate = this.preferred;
        }
    }

    Object beforeApply(Object obj) throws Exception {
        View view;
        if (!this.failed && RuntimeOptions.enabled(5) && Looper.myLooper() == Looper.getMainLooper() && (view = (View) this.rootField.get(obj)) != null && view.isAttachedToWindow()) {
            Session session = this.sessions.get(obj);
            if (session == null || session.root.get() != view) {
                session = new Session(obj, view);
                this.sessions.put(obj, session);
                view.getViewTreeObserver().addOnDrawListener(session);
                session.listenerAttached = true;
            }
            this.current = session;
            Object obj2 = this.stateField.get(obj);
            boolean z = this.visible.getBoolean(obj2) || this.expanded.getBoolean(obj2);
            refreshPolicy(session, false);
            Display display = view.getDisplay();
            boolean z2 = this.keyguard.getBoolean(obj2);
            boolean zShadeAllowed = Policy.shadeAllowed(z || session.lockscreenDrag, this.dozing.getBoolean(obj2), z2 && !session.lockscreenDrag, this.bouncer.getBoolean(obj2), this.dreaming.getBoolean(obj2), display != null && display.getState() == 2, session.powerSave);
            if (!session.applying && zShadeAllowed && !z2 && !session.panelWasVisible) {
                session.renew();
            }
            if (!zShadeAllowed) {
                session.lockscreenDrag = false;
                session.trackingTouch = false;
            }
            session.panelWasVisible = z;
            if (session.applying) {
                return null;
            }
            if (!zShadeAllowed || SystemClock.uptimeMillis() >= session.until || session.target <= 0.0f) {
                if (session.voted) {
                    restoreViews(session);
                    session.voted = false;
                }
            } else {
                if (this.deferField.getInt(obj) != 0) {
                    return null;
                }
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) this.lpField.get(obj);
                Snapshot snapshot = new Snapshot(layoutParams, this.minRateField, this.maxRateField);
                try {
                    this.minRateField.setFloat(layoutParams, session.target);
                    this.maxRateField.setFloat(layoutParams, session.target);
                    layoutParams.preferredRefreshRate = session.target;
                    session.lastTarget = session.target;
                    session.voted = true;
                    return snapshot;
                } catch (Exception e) {
                    snapshot.restore();
                    throw e;
                }
            }
        }
        return null;
    }

    void afterApply(Object obj) throws IllegalAccessException {
        if (obj instanceof Snapshot) {
            ((Snapshot) obj).restore();
        }
    }

    void refreshPolicy(Session session, boolean z) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (z || jUptimeMillis - session.policyAt >= 1000) {
            session.policyAt = jUptimeMillis;
            this.policyReads++;
            View view = session.root.get();
            if (view == null) {
                session.target = 0.0f;
                return;
            }
            Context context = view.getContext();
            PowerManager powerManager = (PowerManager) context.getSystemService("power");
            session.powerSave = powerManager != null && powerManager.isPowerSaveMode();
            session.peak = Settings.System.getFloat(context.getContentResolver(), "peak_refresh_rate", 0.0f);
            session.standardMode = Settings.System.getInt(context.getContentResolver(), "refresh_rate_mode", -1) == 0;
            Display display = view.getDisplay();
            if (display == null) {
                session.target = 0.0f;
                return;
            }
            Display.Mode mode = display.getMode();
            Display.Mode[] supportedModes = display.getSupportedModes();
            float[] fArr = new float[supportedModes.length];
            for (int i = 0; i < supportedModes.length; i++) {
                if (supportedModes[i].getPhysicalWidth() == mode.getPhysicalWidth() && supportedModes[i].getPhysicalHeight() == mode.getPhysicalHeight()) {
                    fArr[i] = supportedModes[i].getRefreshRate();
                }
            }
            session.target = session.powerSave ? 0.0f : Policy.selectRate(fArr, session.peak, session.standardMode);
        }
    }

    void touch(View view, MotionEvent motionEvent) throws Exception {
        Session session;
        Object obj;
        if (this.failed || !RuntimeOptions.enabled(5) || Looper.myLooper() != Looper.getMainLooper() || (session = this.current) == null || session.root.get() != view || (obj = session.owner.get()) == null) {
            return;
        }
        Object obj2 = this.stateField.get(obj);
        Display display = view.getDisplay();
        if (this.dozing.getBoolean(obj2) || this.bouncer.getBoolean(obj2) || this.dreaming.getBoolean(obj2) || display == null || display.getState() != 2) {
            session.lockscreenDrag = false;
            session.trackingTouch = false;
            return;
        }
        refreshPolicy(session, false);
        if (session.powerSave) {
            return;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            session.downX = motionEvent.getX();
            session.downY = motionEvent.getY();
            session.trackingTouch = true;
            session.lockscreenDrag = false;
        } else if (actionMasked == 2 && session.trackingTouch && Policy.downwardDrag(motionEvent.getX() - session.downX, motionEvent.getY() - session.downY, session.touchSlop)) {
            session.lockscreenDrag = true;
        }
        if ((!this.keyguard.getBoolean(obj2) || session.lockscreenDrag) && (actionMasked == 0 || actionMasked == 2 || actionMasked == 1 || actionMasked == 3)) {
            long j = session.until;
            session.renew();
            if (j <= SystemClock.uptimeMillis() && session.until > SystemClock.uptimeMillis()) {
                this.apply.invoke(obj, null);
            }
        }
        if (actionMasked == 1 || actionMasked == 3) {
            session.trackingTouch = false;
        }
    }

    float composeRate(View view, float f) throws Exception {
        Session session = this.current;
        if (this.failed || !RuntimeOptions.enabled(5) || session == null || !session.voted || session.until <= SystemClock.uptimeMillis() || session.target <= 0.0f || view.getRootView() != session.root.get()) {
            return f;
        }
        session.votes.put(view, Float.valueOf(f));
        this.composeVotes++;
        return Math.max(session.target, f);
    }

    void restoreViews(Session session) {
        for (Map.Entry<View, Float> entry : session.votes.entrySet()) {
            View key = entry.getKey();
            if (key != null) {
                try {
                    key.setRequestedFrameRate(entry.getValue().floatValue());
                } catch (Throwable unused) {
                }
            }
        }
        session.votes.clear();
    }

    void release(Session session) {
        View view = session.root.get();
        Display display = view == null ? null : view.getDisplay();
        float refreshRate = display == null ? 0.0f : display.getRefreshRate();
        session.until = 0L;
        session.lockscreenDrag = false;
        session.trackingTouch = false;
        restoreViews(session);
        Object obj = session.owner.get();
        boolean z = session.voted;
        session.voted = false;
        if (obj != null && z) {
            try {
                session.applying = true;
                this.apply.invoke(obj, null);
                this.restores++;
            } catch (Throwable unused) {
            }
            session.applying = false;
        }
        if (session.drawCount > 0) {
            StringBuilder sb = new StringBuilder("gesto: pedido=");
            sb.append(session.lastTarget);
            sb.append("Hz observado=");
            sb.append(refreshRate);
            sb.append("Hz draws=");
            sb.append(session.drawCount);
            sb.append(" intervaloMedioMs=");
            sb.append(session.intervalCount == 0 ? 0.0d : (session.intervalSumNs / session.intervalCount) / 1000000.0d);
            sb.append(" maiorIntervaloMs=");
            sb.append(session.maxIntervalNs / 1000000.0d);
            sb.append(" intervalosLongos=");
            sb.append(session.slowIntervals);
            sb.append(" avisosEvitados=");
            sb.append(SunshineModule.warningCount.get());
            sb.append(" logsBlurEvitados=");
            sb.append(SunshineModule.alphaLogCount.get());
            SunshineModule.diagnostic(sb.toString());
            session.drawCount = 0L;
        }
    }

    void suspend() {
        for (Session session : this.sessions.values()) {
            session.handler.removeCallbacks(session);
            session.queued = false;
            release(session);
        }
    }

    void fail(Throwable th) {
        if (this.failed) {
            return;
        }
        this.failed = true;
        suspend();
        SunshineModule.log("refresh desativado apos erro; comportamento original mantido: " + th.getClass().getSimpleName());
    }

    String report() {
        Session session = this.current;
        StringBuilder sb = new StringBuilder("v1.7.0 ");
        sb.append(SunshineModule.hooks);
        sb.append("; avisosEvitados=");
        sb.append(SunshineModule.warningCount.get());
        sb.append("; logsBlurEvitados=");
        sb.append(SunshineModule.alphaLogCount.get());
        sb.append("; gestos=");
        sb.append(this.leases);
        sb.append("; gestosBloqueio=");
        sb.append(this.lockscreenLeases);
        sb.append("; leiturasPolitica=");
        sb.append(this.policyReads);
        sb.append("; votosCompose=");
        sb.append(this.composeVotes);
        sb.append("; restauracoes=");
        sb.append(this.restores);
        sb.append("; falhaRefresh=");
        sb.append(this.failed);
        sb.append("; pedidoHz=");
        sb.append((session == null || !session.voted) ? 0.0f : session.lastTarget);
        return sb.toString();
    }
}
