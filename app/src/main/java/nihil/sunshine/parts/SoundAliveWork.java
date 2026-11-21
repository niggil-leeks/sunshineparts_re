package nihil.sunshine.parts;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class SoundAliveWork {
    static final String AUTHORITY = "com.sec.android.app.soundalive.compatibility.SAContentProvider";
    private static final String ROOT = "com.android.systemui.audio.soundcraft.";
    private static Access access;
    private static volatile boolean enabled;
    private static Listener listener;
    private static Handler main;
    private static volatile boolean matched;
    private static SoundAliveQueue<Key, Snapshot> queue;
    private static volatile long routeEpoch;
    private static volatile long userEpoch;
    private static Handler worker;
    private static final SoundAliveStats stats = new SoundAliveStats();
    private static final ThreadLocal<Scope> scopes = new ThreadLocal<>();
    private static final ThreadLocal<Frame> frames = new ThreadLocal<>();
    private static final WeakHashMap<Object, Session> sessions = new WeakHashMap<>();

    static final class Snapshot {
        final String[] columns;
        final long readAt = SystemClock.uptimeMillis();
        final String[] rows;

        Snapshot(String[] strArr, String[] strArr2) {
            this.columns = (String[]) strArr.clone();
            this.rows = (String[]) strArr2.clone();
        }

        Cursor cursor() {
            MatrixCursor matrixCursor = new MatrixCursor(this.columns, this.rows.length);
            for (String str : this.rows) {
                Object[] objArr = new Object[this.columns.length];
                objArr[0] = str;
                matrixCursor.addRow(objArr);
            }
            return matrixCursor;
        }

        static Snapshot read(ContentResolver contentResolver, Uri uri, String[] strArr) throws Exception {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("Provider read on main");
            }
            Cursor cursorQuery = contentResolver.query(uri, strArr, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getColumnCount() >= 1 && cursorQuery.getColumnCount() <= 32) {
                        String[] columnNames = cursorQuery.getColumnNames();
                        ArrayList arrayList = new ArrayList();
                        while (cursorQuery.moveToNext()) {
                            if (arrayList.size() >= strArr.length) {
                                throw new IllegalStateException("Unexpected provider row count");
                            }
                            String string = cursorQuery.getString(0);
                            if (string == null || string.length() > 4096) {
                                throw new IllegalStateException("Invalid provider row");
                            }
                            Integer.parseInt(string.split(",", 2)[0]);
                            arrayList.add(string);
                        }
                        if (arrayList.size() != strArr.length) {
                            throw new IllegalStateException("Incomplete provider state");
                        }
                        Snapshot snapshot = new Snapshot(columnNames, (String[]) arrayList.toArray(new String[0]));
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return snapshot;
                    }
                } catch (Throwable th) {
                    if (cursorQuery == null) {
                        throw th;
                    }
                    try {
                        cursorQuery.close();
                        throw th;
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                        throw th;
                    }
                }
            }
            throw new IllegalStateException("Provider unavailable");
        }
    }

    static final class Key {
        final Context context;
        final int device;
        final Uri uri;
        final int user;
        final long route = SoundAliveWork.routeEpoch;
        final long users = SoundAliveWork.userEpoch;
        final long measurementEpoch = SoundAliveWork.stats.epoch();

        Key(Head head) {
            this.context = head.context;
            this.uri = head.uri;
            this.user = head.user;
            this.device = head.device;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            return this.context == key.context && this.uri.equals(key.uri) && this.user == key.user && this.device == key.device && this.route == key.route && this.users == key.users;
        }

        public int hashCode() {
            return (System.identityHashCode(this.context) * 31) + this.device + this.user;
        }
    }

    static final class Head {
        final boolean buds;
        final Context context;
        final int device;
        final Object settings;
        final Uri uri;
        final int user;
        final Object volume;
        final Object volumeManager;

        Head(Context context, Uri uri, Object obj, Object obj2, Object obj3, int i, int i2, boolean z) {
            this.context = context;
            this.uri = uri;
            this.volume = obj;
            this.settings = obj2;
            this.volumeManager = obj3;
            this.user = i;
            this.device = i2;
            this.buds = z;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Session {
        boolean applying;
        int origin;
        Head prepared;
        boolean refreshPosted;
        final WeakReference<Object> vm;
        final SoundAliveQueue.Entry<Key, Snapshot> entry = new SoundAliveQueue.Entry<>();
        WeakReference<Object> owner = new WeakReference<>(null);
        WeakReference<Object> registry = new WeakReference<>(null);

        Session(Object obj) {
            this.vm = new WeakReference<>(obj);
        }
    }

    private static final class Scope {
        final Object owner;
        final Scope previous;

        Scope(Object obj, Scope scope) {
            this.owner = obj;
            this.previous = scope;
        }
    }

    private static final class Frame {
        final boolean async;
        final long began;
        final long epoch;
        final Head head;
        final int kind;
        final Frame previous;
        final Session session;
        final Snapshot snapshot;
        boolean volumePending = true;
        boolean settingsPending = true;

        Frame(Frame frame, Session session, Head head, Snapshot snapshot, boolean z, long j, long j2) {
            this.previous = frame;
            this.session = session;
            this.head = head;
            this.snapshot = snapshot;
            this.async = z;
            this.began = j;
            this.epoch = j2;
            this.kind = (session == null || !session.applying) ? 0 : 3;
        }
    }

    private static final class Access {
        final Field buds;
        final Class<?> component;
        final Field componentRegistry;
        final Field currentOwner;
        final Field device;
        final Field fromCover;
        final Field fromNowBar;
        final Method lifeState;
        final Method liveSet;
        final Field manager;
        final Field managerContext;
        final Field managerUri;
        final Field ownerModel;
        final String[] projection;
        final Field provider;
        final Field providerVolume;
        final Field settings;
        final Method settingsUpdate;
        final Method update;
        final Field updateVolume;
        final Method userId;
        final Method viewModel;
        final Class<?> vm;
        final Object vmExt;
        final Method volumeGet;
        final Field volumeManager;

        Access(ClassLoader classLoader) throws Exception {
            this.vm = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.viewmodel.SoundCraftViewModel");
            Class clsType = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.interfaces.soundalive.SoundAliveManager");
            Class clsType2 = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.interfaces.volume.VolumeManager");
            Class clsType3 = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.model.ModelProvider");
            Class clsType4 = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.interfaces.settings.SoundCraftSettings");
            Class<?> clsType5 = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.model.common.VolumeModel");
            Class clsType6 = SoundAliveWork.type(classLoader, "androidx.lifecycle.MutableLiveData");
            Class clsType7 = SoundAliveWork.type(classLoader, "androidx.lifecycle.Lifecycle");
            this.manager = SoundAliveWork.field(this.vm, "soundAliveManager", clsType);
            this.managerContext = SoundAliveWork.field(clsType, "context", Context.class);
            this.managerUri = SoundAliveWork.field(clsType, "uri", Uri.class);
            this.volumeManager = SoundAliveWork.field(this.vm, "volumeManager", clsType2);
            this.provider = SoundAliveWork.field(this.vm, "modelProvider", clsType3);
            this.providerVolume = SoundAliveWork.field(clsType3, "volumeModel", clsType5);
            this.settings = SoundAliveWork.field(this.vm, "settings", clsType4);
            this.device = SoundAliveWork.field(clsType5, "device", Integer.TYPE);
            this.buds = SoundAliveWork.field(clsType4, "isBudsActive", Boolean.TYPE);
            this.updateVolume = SoundAliveWork.field(this.vm, "updateVolumeBar", clsType6);
            this.component = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.view.SoundCraftViewComponent");
            this.update = this.component.getDeclaredMethod("updateModel", Integer.TYPE);
            this.volumeGet = clsType2.getDeclaredMethod("getVolumeModel", new Class[0]);
            this.settingsUpdate = clsType4.getDeclaredMethod("update", new Class[0]);
            this.liveSet = clsType6.getDeclaredMethod("setValue", Object.class);
            this.lifeState = clsType7.getDeclaredMethod("getCurrentState", new Class[0]);
            this.userId = Context.class.getDeclaredMethod("getUserId", new Class[0]);
            if (this.update.getReturnType() == Void.TYPE && this.volumeGet.getReturnType() == clsType5 && this.settingsUpdate.getReturnType() == Void.TYPE) {
                this.componentRegistry = SoundAliveWork.field(this.component, "registry", SoundAliveWork.type(classLoader, "androidx.lifecycle.LifecycleRegistry"));
                this.ownerModel = SoundAliveWork.field(this.component, "modelProvider", clsType3);
                this.fromNowBar = SoundAliveWork.field(clsType3, "isFromNowBar", Boolean.TYPE);
                this.fromCover = SoundAliveWork.field(clsType3, "isFromCover", Boolean.TYPE);
                Class clsType8 = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.di.vm.component.SoundCraftLocalViewModelStoreOwner");
                Class clsType9 = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.di.vm.component.SoundCraftViewModelExt");
                this.currentOwner = SoundAliveWork.field(clsType8, "current", this.component);
                this.vmExt = clsType9.getDeclaredField("INSTANCE").get(null);
                this.viewModel = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.view.SoundCraftDetailPageView$$ExternalSyntheticOutline0").getDeclaredMethod("m", clsType9, this.component, this.component, Class.class);
                this.viewModel.setAccessible(true);
                Class clsType10 = SoundAliveWork.type(classLoader, "com.android.systemui.audio.soundcraft.interfaces.soundalive.SoundAliveEffectEnum");
                Method declaredMethod = clsType10.getDeclaredMethod("values", new Class[0]);
                Method declaredMethod2 = clsType10.getDeclaredMethod("getSettingName", new Class[0]);
                Object objInvoke = declaredMethod.invoke(null, new Object[0]);
                if (Array.getLength(objInvoke) != 6) {
                    throw new IllegalStateException("SoundAlive projection ABI");
                }
                this.projection = new String[6];
                for (int i = 0; i < 6; i++) {
                    this.projection[i] = (String) declaredMethod2.invoke(Array.get(objInvoke, i), new Object[0]);
                }
                Method[] methodArr = {this.update, this.volumeGet, this.settingsUpdate, this.liveSet, this.lifeState, this.userId};
                for (int i2 = 0; i2 < 6; i2++) {
                    methodArr[i2].setAccessible(true);
                }
                return;
            }
            throw new NoSuchMethodException("SoundCraft model ABI");
        }

        Object model(Object obj) throws Exception {
            return this.viewModel.invoke(null, this.vmExt, obj, obj, this.vm);
        }

        Head head(Object obj) throws Exception {
            this.currentOwner.set(null, obj);
            Session session = (Session) SoundAliveWork.sessions.get(obj);
            int i = session == null ? 0 : session.origin;
            Object obj2 = this.ownerModel.get(obj);
            this.fromNowBar.setBoolean(obj2, i == 1);
            this.fromCover.setBoolean(obj2, i == 2);
            Object objModel = model(obj);
            Object obj3 = this.volumeManager.get(objModel);
            Object objInvoke = this.volumeGet.invoke(obj3, new Object[0]);
            Object obj4 = this.provider.get(objModel);
            Object obj5 = this.settings.get(objModel);
            this.providerVolume.set(obj4, objInvoke);
            this.settingsUpdate.invoke(obj5, new Object[0]);
            Object obj6 = this.manager.get(objModel);
            Context context = (Context) this.managerContext.get(obj6);
            Uri uri = (Uri) this.managerUri.get(obj6);
            if (!SoundAliveWork.AUTHORITY.equals(uri.getAuthority())) {
                throw new IllegalStateException("Unexpected provider");
            }
            return new Head(context, uri, objInvoke, obj5, obj3, ((Integer) this.userId.invoke(context, new Object[0])).intValue(), this.device.getInt(objInvoke), this.buds.getBoolean(obj5));
        }

        Object registry(Object obj) throws Exception {
            return this.componentRegistry.get(obj);
        }

        boolean alive(Object obj, Object obj2) throws Exception {
            if (obj == null || obj2 == null || registry(obj) != obj2) {
                return false;
            }
            String strValueOf = String.valueOf(this.lifeState.invoke(obj2, new Object[0]));
            return (strValueOf.equals("DESTROYED") || strValueOf.equals("INITIALIZED")) ? false : true;
        }

        void volumeOnly(Object obj) throws Exception {
            this.liveSet.invoke(this.updateVolume.get(model(obj)), Boolean.TRUE);
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

    /* JADX INFO: Access modifiers changed from: private */
    public static Handler worker() {
        if (worker == null) {
            HandlerThread handlerThread = new HandlerThread("SunshineSoundAlive", 10);
            handlerThread.start();
            worker = new Handler(handlerThread.getLooper());
        }
        return worker;
    }

    private static boolean pinned(String str) {
        return NativeTargets.systemUi(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Session session(Object obj) {
        Session session = sessions.get(obj);
        if (session != null) {
            return session;
        }
        if (sessions.size() >= 8) {
            Iterator<Map.Entry<Object, Session>> it = sessions.entrySet().iterator();
            while (it.hasNext()) {
                Session value = it.next().getValue();
                if (!value.entry.active) {
                    queue.close(value.entry);
                    it.remove();
                    break;
                }
            }
        }
        if (sessions.size() >= 8) {
            return session;
        }
        Session session2 = new Session(obj);
        sessions.put(obj, session2);
        return session2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean active(Session session) {
        try {
            return session.entry.active && access.alive(session.owner.get(), session.registry.get());
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void close(Object obj) {
        for (Session session : new ArrayList(sessions.values())) {
            if (session.owner.get() == obj) {
                queue.close(session.entry);
                session.prepared = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void finishScope(Scope scope, boolean z) {
        if (scope == null) {
            return;
        }
        for (Session session : new ArrayList(sessions.values())) {
            if (session.owner.get() == scope.owner) {
                try {
                    Object objRegistry = access.registry(scope.owner);
                    if (z || !access.alive(scope.owner, objRegistry)) {
                        queue.close(session.entry);
                    } else {
                        session.registry = new WeakReference<>(objRegistry);
                    }
                } catch (Exception e) {
                    queue.close(session.entry);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void refresh(Session session) {
        Object obj = session.vm.get();
        if (obj != null && enabled && active(session)) {
            try {
                access.update.invoke(obj, Integer.valueOf(session.origin));
            } catch (Exception e) {
                stats.failure();
            }
        }
    }

    static /* synthetic */ void lambda$schedule$0(Session session) {
        session.refreshPosted = false;
        refresh(session);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void schedule(final Session session) {
        if (session.refreshPosted) {
            return;
        }
        session.refreshPosted = true;
        main.post(new Runnable() { // from class: nihil.sunshine.parts.SoundAliveWork$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SoundAliveWork.lambda$schedule$0(session);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void invalidate(boolean z, boolean z2, boolean z3) {
        if (z2) {
            routeEpoch++;
        }
        if (z3) {
            userEpoch++;
        }
        for (Session session : new ArrayList(sessions.values())) {
            queue.invalidate(session.entry, z);
            if (enabled && active(session)) {
                schedule(session);
            }
        }
    }

    private static void initializeQueue() {
        queue = new SoundAliveQueue<>(new SoundAliveQueue.Backend<Key, Snapshot>() { // from class: nihil.sunshine.parts.SoundAliveWork.1
            @Override // nihil.sunshine.parts.SoundAliveQueue.Backend
            public Snapshot read(Key key) throws Exception {
                if (!SoundAliveWork.enabled || key.route != SoundAliveWork.routeEpoch || key.users != SoundAliveWork.userEpoch || key.measurementEpoch != SoundAliveWork.stats.epoch()) {
                    return null;
                }
                SoundAliveWork.ensureListener(key.context, key.uri);
                long j = key.measurementEpoch;
                if (!SoundAliveWork.enabled || j != SoundAliveWork.stats.epoch()) {
                    return null;
                }
                long jBegin = SoundAliveWork.stats.begin(2, j);
                SoundAliveWork.stats.read(j);
                try {
                    try {
                        Snapshot snapshot = Snapshot.read(key.context.getContentResolver(), key.uri, (String[]) SoundAliveWork.access.projection.clone());
                        SoundAliveWork.stats.end(2, jBegin, j);
                        return snapshot;
                    } catch (Exception e) {
                        SoundAliveWork.stats.failure(j);
                        throw e;
                    }
                } catch (Throwable th) {
                    SoundAliveWork.stats.end(2, jBegin, j);
                    throw th;
                }
            }

            @Override // nihil.sunshine.parts.SoundAliveQueue.Backend
            public boolean current(SoundAliveQueue.Entry<Key, Snapshot> entry, Key key) {
                Object obj;
                Session sessionFind = SoundAliveWork.find(entry);
                if (sessionFind == null || !SoundAliveWork.enabled || !SoundAliveWork.active(sessionFind) || key.route != SoundAliveWork.routeEpoch || key.users != SoundAliveWork.userEpoch || (obj = sessionFind.vm.get()) == null) {
                    return false;
                }
                Frame frame = (Frame) SoundAliveWork.frames.get();
                SoundAliveWork.frames.remove();
                try {
                    Head head = SoundAliveWork.access.head(obj);
                    if (!head.buds && key.equals(new Key(head))) {
                        sessionFind.prepared = head;
                    }
                    SoundAliveWork.schedule(sessionFind);
                    return false;
                } catch (Exception e) {
                    SoundAliveWork.stats.failure();
                    return false;
                } finally {
                    if (frame != null) {
                        SoundAliveWork.frames.set(frame);
                    }
                }
            }

            @Override // nihil.sunshine.parts.SoundAliveQueue.Backend
            public void publish(SoundAliveQueue.Entry<Key, Snapshot> entry) {
                Session sessionFind = SoundAliveWork.find(entry);
                if (sessionFind != null) {
                    sessionFind.applying = true;
                    try {
                        SoundAliveWork.refresh(sessionFind);
                    } finally {
                        sessionFind.applying = false;
                        sessionFind.prepared = null;
                    }
                }
            }
        }, new SoundAliveQueue.Dispatch() { // from class: nihil.sunshine.parts.SoundAliveWork.2
            @Override // nihil.sunshine.parts.SoundAliveQueue.Dispatch
            public void main(Runnable runnable) {
                SoundAliveWork.main.post(runnable);
            }

            @Override // nihil.sunshine.parts.SoundAliveQueue.Dispatch
            public long now() {
                return SystemClock.uptimeMillis();
            }

            @Override // nihil.sunshine.parts.SoundAliveQueue.Dispatch
            public void worker(Runnable runnable) {
                SoundAliveWork.worker().post(runnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Session find(SoundAliveQueue.Entry<Key, Snapshot> entry) {
        for (Session session : sessions.values()) {
            if (session.entry == entry) {
                return session;
            }
        }
        return null;
    }

    private static XC_MethodHook scopeHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.SoundAliveWork.3
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    return;
                }
                try {
                    Scope scope = new Scope(methodHookParam.thisObject, (Scope) SoundAliveWork.scopes.get());
                    SoundAliveWork.scopes.set(scope);
                    methodHookParam.setObjectExtra("soundalive.scope", scope);
                } catch (Exception e) {
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra("soundalive.scope");
                if (objectExtra instanceof Scope) {
                    Scope scope = (Scope) objectExtra;
                    SoundAliveWork.finishScope(scope, methodHookParam.hasThrowable());
                    if (SoundAliveWork.enabled && !methodHookParam.hasThrowable()) {
                        for (Session session : new ArrayList(SoundAliveWork.sessions.values())) {
                            if (session.owner.get() == scope.owner && session.entry.snapshot == null && SoundAliveWork.active(session)) {
                                SoundAliveWork.schedule(session);
                            }
                        }
                    }
                    if (scope.previous == null) {
                        SoundAliveWork.scopes.remove();
                    } else {
                        SoundAliveWork.scopes.set(scope.previous);
                    }
                }
            }
        };
    }

    private static XC_MethodHook updateHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.SoundAliveWork.4
            /* JADX WARN: Code duplicated, block: B:103:0x00ef A[EXC_TOP_SPLITTER, SYNTHETIC] */
            /* JADX WARN: Code duplicated, block: B:109:0x00a8 A[EXC_TOP_SPLITTER, SYNTHETIC] */
            /* JADX WARN: Code duplicated, block: B:33:0x007e A[Catch: all -> 0x0071, Exception -> 0x0076, TRY_LEAVE, TryCatch #12 {Exception -> 0x0076, all -> 0x0071, blocks: (B:21:0x005e, B:23:0x0066, B:33:0x007e), top: B:105:0x005e }] */
            /* JADX WARN: Code duplicated, block: B:36:0x009b A[Catch: all -> 0x016b, Exception -> 0x0170, TRY_LEAVE, TryCatch #15 {Exception -> 0x0170, all -> 0x016b, blocks: (B:15:0x0049, B:18:0x004f, B:34:0x0095, B:36:0x009b), top: B:99:0x0049 }] */
            /* JADX WARN: Code duplicated, block: B:47:0x00c5 A[Catch: all -> 0x00b3, Exception -> 0x00ba, TRY_ENTER, TRY_LEAVE, TryCatch #10 {Exception -> 0x00ba, all -> 0x00b3, blocks: (B:39:0x00a8, B:47:0x00c5), top: B:109:0x00a8 }] */
            /* JADX WARN: Code duplicated, block: B:49:0x00d8 A[Catch: all -> 0x0142, Exception -> 0x0149, TRY_ENTER, TRY_LEAVE, TryCatch #14 {Exception -> 0x0149, all -> 0x0142, blocks: (B:45:0x00c1, B:49:0x00d8), top: B:101:0x00c1 }] */
            /* JADX WARN: Code duplicated, block: B:55:0x00fe A[Catch: all -> 0x0135, Exception -> 0x013c, TryCatch #9 {Exception -> 0x013c, all -> 0x0135, blocks: (B:53:0x00f5, B:55:0x00fe, B:61:0x010d, B:63:0x0111, B:65:0x011c), top: B:111:0x00f5 }] */
            /* JADX WARN: Code duplicated, block: B:60:0x010b  */
            /* JADX WARN: Code duplicated, block: B:63:0x0111 A[Catch: all -> 0x0135, Exception -> 0x013c, TryCatch #9 {Exception -> 0x013c, all -> 0x0135, blocks: (B:53:0x00f5, B:55:0x00fe, B:61:0x010d, B:63:0x0111, B:65:0x011c), top: B:111:0x00f5 }] */
            /* JADX WARN: Code duplicated, block: B:65:0x011c A[Catch: all -> 0x0135, Exception -> 0x013c, TRY_LEAVE, TryCatch #9 {Exception -> 0x013c, all -> 0x0135, blocks: (B:53:0x00f5, B:55:0x00fe, B:61:0x010d, B:63:0x0111, B:65:0x011c), top: B:111:0x00f5 }] */
            /* JADX WARN: Code duplicated, block: B:79:0x0158  */
            /* JADX WARN: Code duplicated, block: B:90:0x0181 A[ADDED_TO_REGION] */
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                Snapshot snapshot;
                Frame frame;
                boolean z;
                Head head;
                Snapshot snapshot2;
                boolean z2;
                Head head2;
                Snapshot snapshot3;
                Head head3;
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    return;
                }
                Frame frame2 = (Frame) SoundAliveWork.frames.get();
                Session session = SoundAliveWork.session(methodHookParam.thisObject);
                boolean z3 = false;
                if (session != null) {
                    session.origin = ((Integer) methodHookParam.args[0]).intValue();
                }
                long jBegin = SoundAliveWork.stats.begin((session == null || !session.applying) ? 0 : 3);
                long jEpoch = SoundAliveWork.stats.epoch();
                Head head4 = null;
                try {
                    Object obj = methodHookParam.thisObject;
                    if (session == null || obj == null) {
                        z = false;
                        head = null;
                        snapshot2 = null;
                    } else {
                        Object objRegistry = SoundAliveWork.access.registry(obj);
                        if (session.entry.active) {
                            try {
                                if (session.owner.get() == obj && session.registry.get() == objRegistry) {
                                    z2 = false;
                                }
                                if (z2) {
                                    SoundAliveWork.queue.close(session.entry);
                                    session.owner = new WeakReference<>(obj);
                                    session.registry = new WeakReference<>(objRegistry);
                                }
                                if (SoundAliveWork.enabled) {
                                    SoundAliveWork.frames.remove();
                                    head2 = session.prepared;
                                    try {
                                        session.prepared = null;
                                        if (head2 == null) {
                                            try {
                                                head2 = SoundAliveWork.access.head(methodHookParam.thisObject);
                                            } catch (Exception e) {
                                                Head head5 = head2;
                                                snapshot = null;
                                                head4 = head5;
                                                z3 = false;
                                                try {
                                                    SoundAliveWork.stats.failure();
                                                    if (SoundAliveWork.enabled) {
                                                        SoundAliveWork.queue.close(session.entry);
                                                        methodHookParam.setResult((Object) null);
                                                    }
                                                    frame = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                                    SoundAliveWork.frames.set(frame);
                                                    methodHookParam.setObjectExtra("soundalive.frame", frame);
                                                } catch (Throwable th) {
                                                    th = th;
                                                    Frame frame3 = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                                    SoundAliveWork.frames.set(frame3);
                                                    methodHookParam.setObjectExtra("soundalive.frame", frame3);
                                                    throw th;
                                                }
                                            } catch (Throwable th2) {
                                                th = th2;
                                                Head head6 = head2;
                                                snapshot = null;
                                                head4 = head6;
                                                z3 = false;
                                                Frame frame4 = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                                SoundAliveWork.frames.set(frame4);
                                                methodHookParam.setObjectExtra("soundalive.frame", frame4);
                                                throw th;
                                            }
                                        }
                                        try {
                                            if (head2.buds) {
                                                SoundAliveWork.queue.close(session.entry);
                                                session.entry.snapshot = null;
                                                Head head7 = head2;
                                                snapshot2 = null;
                                                head = head7;
                                                z = false;
                                            } else {
                                                SoundAliveWork.queue.open(session.entry, new Key(head2));
                                                snapshot3 = session.entry.snapshot;
                                                if (snapshot3 != null) {
                                                    try {
                                                        head3 = head2;
                                                        try {
                                                            if (SystemClock.uptimeMillis() - snapshot3.readAt >= 5000) {
                                                                session.entry.dirty = true;
                                                            }
                                                        } catch (Exception e2) {
                                                            snapshot = snapshot3;
                                                            z3 = true;
                                                            head4 = head3;
                                                            SoundAliveWork.stats.failure();
                                                            if (SoundAliveWork.enabled) {
                                                                SoundAliveWork.queue.close(session.entry);
                                                                methodHookParam.setResult((Object) null);
                                                            }
                                                            frame = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                            snapshot = snapshot3;
                                                            z3 = true;
                                                            head4 = head3;
                                                            Frame frame5 = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                                            SoundAliveWork.frames.set(frame5);
                                                            methodHookParam.setObjectExtra("soundalive.frame", frame5);
                                                            throw th;
                                                        }
                                                    } catch (Exception e3) {
                                                        head3 = head2;
                                                        snapshot = snapshot3;
                                                        z3 = true;
                                                        head4 = head3;
                                                        SoundAliveWork.stats.failure();
                                                        if (SoundAliveWork.enabled) {
                                                            SoundAliveWork.queue.close(session.entry);
                                                            methodHookParam.setResult((Object) null);
                                                        }
                                                        frame = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                        head3 = head2;
                                                        snapshot = snapshot3;
                                                        z3 = true;
                                                        head4 = head3;
                                                        Frame frame6 = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                                        SoundAliveWork.frames.set(frame6);
                                                        methodHookParam.setObjectExtra("soundalive.frame", frame6);
                                                        throw th;
                                                    }
                                                } else {
                                                    head3 = head2;
                                                }
                                                if (!session.applying) {
                                                    SoundAliveWork.queue.request(session.entry);
                                                }
                                                if (snapshot3 == null) {
                                                    SoundAliveWork.access.volumeOnly(methodHookParam.thisObject);
                                                    SoundAliveWork.stats.defer();
                                                    methodHookParam.setResult((Object) null);
                                                }
                                                snapshot2 = snapshot3;
                                                z = true;
                                                head = head3;
                                            }
                                        } catch (Exception e4) {
                                            head4 = head2;
                                            snapshot = null;
                                            SoundAliveWork.stats.failure();
                                            if (SoundAliveWork.enabled && session != null) {
                                                SoundAliveWork.queue.close(session.entry);
                                                methodHookParam.setResult((Object) null);
                                            }
                                            frame = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                            SoundAliveWork.frames.set(frame);
                                            methodHookParam.setObjectExtra("soundalive.frame", frame);
                                        } catch (Throwable th5) {
                                            th = th5;
                                            head4 = head2;
                                            snapshot = null;
                                            Frame frame7 = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                            SoundAliveWork.frames.set(frame7);
                                            methodHookParam.setObjectExtra("soundalive.frame", frame7);
                                            throw th;
                                        }
                                    } catch (Exception e5) {
                                        head4 = head2;
                                    } catch (Throwable th6) {
                                        th = th6;
                                        head4 = head2;
                                    }
                                } else {
                                    z = false;
                                    head = null;
                                    snapshot2 = null;
                                }
                            } catch (Exception e6) {
                                z3 = false;
                                snapshot = null;
                                SoundAliveWork.stats.failure();
                                if (SoundAliveWork.enabled) {
                                    SoundAliveWork.queue.close(session.entry);
                                    methodHookParam.setResult((Object) null);
                                }
                                frame = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                SoundAliveWork.frames.set(frame);
                                methodHookParam.setObjectExtra("soundalive.frame", frame);
                            } catch (Throwable th7) {
                                th = th7;
                                z3 = false;
                                snapshot = null;
                                Frame frame8 = new Frame(frame2, session, head4, snapshot, z3, jBegin, jEpoch);
                                SoundAliveWork.frames.set(frame8);
                                methodHookParam.setObjectExtra("soundalive.frame", frame8);
                                throw th;
                            }
                        }
                        z2 = true;
                        if (z2) {
                            SoundAliveWork.queue.close(session.entry);
                            session.owner = new WeakReference<>(obj);
                            session.registry = new WeakReference<>(objRegistry);
                        }
                        if (SoundAliveWork.enabled) {
                            SoundAliveWork.frames.remove();
                            head2 = session.prepared;
                            session.prepared = null;
                            if (head2 == null) {
                                head2 = SoundAliveWork.access.head(methodHookParam.thisObject);
                            }
                            if (head2.buds) {
                                SoundAliveWork.queue.close(session.entry);
                                session.entry.snapshot = null;
                                Head head8 = head2;
                                snapshot2 = null;
                                head = head8;
                                z = false;
                            } else {
                                SoundAliveWork.queue.open(session.entry, new Key(head2));
                                snapshot3 = session.entry.snapshot;
                                if (snapshot3 != null) {
                                    head3 = head2;
                                    if (SystemClock.uptimeMillis() - snapshot3.readAt >= 5000) {
                                        session.entry.dirty = true;
                                    }
                                } else {
                                    head3 = head2;
                                }
                                if (!session.applying) {
                                    SoundAliveWork.queue.request(session.entry);
                                }
                                if (snapshot3 == null) {
                                    SoundAliveWork.access.volumeOnly(methodHookParam.thisObject);
                                    SoundAliveWork.stats.defer();
                                    methodHookParam.setResult((Object) null);
                                }
                                snapshot2 = snapshot3;
                                z = true;
                                head = head3;
                            }
                        } else {
                            z = false;
                            head = null;
                            snapshot2 = null;
                        }
                    }
                    frame = new Frame(frame2, session, head, snapshot2, z, jBegin, jEpoch);
                } catch (Exception e7) {
                    head4 = null;
                } catch (Throwable th8) {
                    th = th8;
                    head4 = null;
                }
                SoundAliveWork.frames.set(frame);
                methodHookParam.setObjectExtra("soundalive.frame", frame);
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra("soundalive.frame");
                if (objectExtra instanceof Frame) {
                    Frame frame = (Frame) objectExtra;
                    SoundAliveWork.stats.end(frame.kind, frame.began, frame.epoch);
                    if (frame.previous == null) {
                        SoundAliveWork.frames.remove();
                    } else {
                        SoundAliveWork.frames.set(frame.previous);
                    }
                }
            }
        };
    }

    private static XC_MethodHook queryHook() {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.SoundAliveWork.5
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Frame frame = (Frame) SoundAliveWork.frames.get();
                if (frame == null || !(methodHookParam.args[0] instanceof Uri) || !(methodHookParam.args[1] instanceof String[]) || !SoundAliveWork.AUTHORITY.equals(((Uri) methodHookParam.args[0]).getAuthority()) || !Arrays.equals(SoundAliveWork.access.projection, (String[]) methodHookParam.args[1]) || methodHookParam.args[2] != null || methodHookParam.args[3] != null || methodHookParam.args[4] != null) {
                    return;
                }
                if (frame.head == null || methodHookParam.thisObject == frame.head.context.getContentResolver()) {
                    if (!frame.async || frame.snapshot == null) {
                        methodHookParam.setObjectExtra("soundalive.query", new long[]{SoundAliveWork.stats.begin(1), SoundAliveWork.stats.epoch()});
                    } else {
                        SoundAliveWork.stats.cache();
                        methodHookParam.setResult(frame.snapshot.cursor());
                    }
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra("soundalive.query");
                if (objectExtra instanceof long[]) {
                    long[] jArr = (long[]) objectExtra;
                    SoundAliveWork.stats.end(1, jArr[0], jArr[1]);
                }
            }
        };
    }

    static void configure() {
        if (main == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(new Runnable() { // from class: nihil.sunshine.parts.SoundAliveWork$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SoundAliveWork.configure();
                }
            });
            return;
        }
        boolean z = matched && Config.soundAliveAsync && !RuntimeProfile.recovery();
        if (z == enabled) {
            return;
        }
        enabled = z;
        stats.mode(z);
        for (Session session : new ArrayList(sessions.values())) {
            queue.close(session.entry);
            session.entry.snapshot = null;
            session.prepared = null;
        }
        if (z || worker == null) {
            return;
        }
        worker.post(new Runnable() { // from class: nihil.sunshine.parts.SoundAliveWork$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                SoundAliveWork.lambda$configure$1();
            }
        });
    }

    static /* synthetic */ void lambda$configure$1() {
        if (listener != null) {
            listener.close();
            listener = null;
        }
    }

    static JSONObject status() throws JSONException {
        return stats.snapshot(matched);
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Listener {
        final AudioManager audio;
        final Context context;
        boolean registered;
        final ContentResolver resolver;
        final ContentObserver observer = new ContentObserver(SoundAliveWork.main) { // from class: nihil.sunshine.parts.SoundAliveWork.Listener.1
            @Override // android.database.ContentObserver
            public void onChange(boolean z) {
                if (SoundAliveWork.enabled) {
                    SoundAliveWork.invalidate(false, false, false);
                }
            }
        };
        final BroadcastReceiver receiver = new BroadcastReceiver() { // from class: nihil.sunshine.parts.SoundAliveWork.Listener.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (SoundAliveWork.enabled) {
                    boolean zEquals = "android.intent.action.USER_SWITCHED".equals(intent.getAction());
                    SoundAliveWork.invalidate(true, !zEquals, zEquals);
                }
            }
        };
        final AudioDeviceCallback devices = new AudioDeviceCallback() { // from class: nihil.sunshine.parts.SoundAliveWork.Listener.3
            @Override // android.media.AudioDeviceCallback
            public void onAudioDevicesAdded(AudioDeviceInfo[] audioDeviceInfoArr) {
                if (SoundAliveWork.enabled) {
                    SoundAliveWork.invalidate(true, true, false);
                }
            }

            @Override // android.media.AudioDeviceCallback
            public void onAudioDevicesRemoved(AudioDeviceInfo[] audioDeviceInfoArr) {
                if (SoundAliveWork.enabled) {
                    SoundAliveWork.invalidate(true, true, false);
                }
            }
        };

        Listener(Context context, Uri uri) {
            this.context = context;
            this.resolver = context.getContentResolver();
            this.audio = (AudioManager) context.getSystemService(AudioManager.class);
            try {
                this.resolver.registerContentObserver(uri, true, this.observer);
            } catch (Exception e) {
                SoundAliveWork.stats.failure();
            }
            try {
                IntentFilter intentFilter = new IntentFilter();
                String[] strArr = {"android.intent.action.USER_SWITCHED", "android.intent.action.HEADSET_PLUG", "android.media.STREAM_DEVICES_CHANGED_ACTION", "android.media.ACTION_SPEAKERPHONE_STATE_CHANGED", "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED", "android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED", "android.bluetooth.action.LE_AUDIO_ACTIVE_DEVICE_CHANGED"};
                for (int i = 0; i < 7; i++) {
                    intentFilter.addAction(strArr[i]);
                }
                context.registerReceiver(this.receiver, intentFilter, null, SoundAliveWork.main, 2);
                this.registered = true;
            } catch (Exception e2) {
                SoundAliveWork.stats.failure();
            }
            if (this.audio != null) {
                try {
                    this.audio.registerAudioDeviceCallback(this.devices, SoundAliveWork.main);
                } catch (Exception e3) {
                    SoundAliveWork.stats.failure();
                }
            }
        }

        void close() {
            try {
                this.resolver.unregisterContentObserver(this.observer);
            } catch (Exception e) {
            }
            if (this.registered) {
                try {
                    this.context.unregisterReceiver(this.receiver);
                } catch (Exception e2) {
                }
            }
            if (this.audio != null) {
                try {
                    this.audio.unregisterAudioDeviceCallback(this.devices);
                } catch (Exception e3) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void ensureListener(Context context, Uri uri) {
        if (listener != null && listener.context != context) {
            listener.close();
            listener = null;
        }
        if (listener == null && enabled) {
            listener = new Listener(context, uri);
        }
    }

    static void install(ClassLoader classLoader, String str) {
        ArrayList arrayList = new ArrayList();
        try {
            if (pinned(str)) {
                access = new Access(classLoader);
                main = new Handler(Looper.getMainLooper());
                initializeQueue();
                ArrayList arrayList2 = new ArrayList();
                ArrayList arrayList3 = new ArrayList();
                arrayList2.add(access.update);
                arrayList3.add(updateHook());
                arrayList2.add(ContentResolver.class.getDeclaredMethod("query", Uri.class, String[].class, String.class, String[].class, String.class));
                arrayList3.add(queryHook());
                arrayList2.add(access.volumeGet);
                arrayList3.add(new XC_MethodHook() { // from class: nihil.sunshine.parts.SoundAliveWork.6
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Frame frame = (Frame) SoundAliveWork.frames.get();
                        if (frame == null || frame.head == null || !frame.volumePending || methodHookParam.thisObject != frame.head.volumeManager) {
                            return;
                        }
                        frame.volumePending = false;
                        methodHookParam.setResult(frame.head.volume);
                    }
                });
                arrayList2.add(access.settingsUpdate);
                arrayList3.add(new XC_MethodHook() { // from class: nihil.sunshine.parts.SoundAliveWork.7
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Frame frame = (Frame) SoundAliveWork.frames.get();
                        if (frame == null || frame.head == null || !frame.settingsPending || methodHookParam.thisObject != frame.head.settings) {
                            return;
                        }
                        frame.settingsPending = false;
                        methodHookParam.setResult((Object) null);
                    }
                });
                arrayList2.add(access.component.getDeclaredMethod("onCreate", Context.class, ViewGroup.class));
                arrayList3.add(scopeHook());
                arrayList2.add(access.component.getDeclaredMethod("onDestroy", new Class[0]));
                arrayList3.add(new XC_MethodHook() { // from class: nihil.sunshine.parts.SoundAliveWork.8
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        SoundAliveWork.close(methodHookParam.thisObject);
                    }
                });
                arrayList2.add(access.manager.getType().getDeclaredMethod("setState", Integer.TYPE, String.class));
                arrayList3.add(new AnonymousClass9());
                for (int i = 0; i < arrayList2.size(); i++) {
                    ((Method) arrayList2.get(i)).setAccessible(true);
                    XC_MethodHook.Unhook unhookHookMethod = RuntimeHooks.hookMethod((Member) arrayList2.get(i), (XC_MethodHook) arrayList3.get(i));
                    if (unhookHookMethod == null) {
                        throw new IllegalStateException("SoundCraft hook unavailable");
                    }
                    arrayList.add(unhookHookMethod);
                }
                matched = true;
                configure();
            }
        } catch (Throwable th) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                RuntimeHooks.unhook((XC_MethodHook.Unhook) it.next());
            }
            matched = false;
            enabled = false;
        }
    }

    /* JADX INFO: renamed from: nihil.sunshine.parts.SoundAliveWork$9, reason: invalid class name */
    class AnonymousClass9 extends XC_MethodHook {
        AnonymousClass9() {
        }

        protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            if (!SoundAliveWork.enabled || methodHookParam.hasThrowable()) {
                return;
            }
            SoundAliveWork.main.post(new Runnable() { // from class: nihil.sunshine.parts.SoundAliveWork$9$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SoundAliveWork.invalidate(true, false, false);
                }
            });
        }
    }

    private SoundAliveWork() {
    }
}
