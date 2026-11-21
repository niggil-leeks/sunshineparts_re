package nihil.sunshine.parts;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
final class ScreenshotWork {
    private static final String ROOT = "com.android.systemui.screenshot.";
    private static boolean installed;
    private static Handler main;
    private static final Semaphore budget = new Semaphore(2);
    private static final ThreadLocal<Frame> current = new ThreadLocal<>();
    private static final ExecutorService worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue(1), new ThreadFactory() { // from class: nihil.sunshine.parts.ScreenshotWork$$ExternalSyntheticLambda0
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return ScreenshotWork.lambda$static$1(runnable);
        }
    });

    static /* synthetic */ void lambda$static$0(Runnable runnable) {
        Process.setThreadPriority(10);
        runnable.run();
    }

    static /* synthetic */ Thread lambda$static$1(final Runnable runnable) {
        Thread thread = new Thread(new Runnable() { // from class: nihil.sunshine.parts.ScreenshotWork$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ScreenshotWork.lambda$static$0(runnable);
            }
        }, "PartsShotThumbnail");
        thread.setDaemon(true);
        return thread;
    }

    static final class Frame {
        final Object callback;
        byte[] jpeg;
        final Object owner;
        Bitmap scaled;
        final Bitmap source;
        final long transaction;

        Frame(Object obj, Object obj2, Bitmap bitmap, long j) {
            this.callback = obj;
            this.owner = obj2;
            this.source = bitmap;
            this.transaction = j;
        }

        void prepare() throws Exception {
            if (this.source.isRecycled()) {
                throw new IOException("Obsolete screenshot");
            }
            this.scaled = Bitmap.createScaledBitmap(this.source, (int) (this.source.getWidth() * 0.25f), (int) (this.source.getHeight() * 0.25f), true);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                if (!this.scaled.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream) || byteArrayOutputStream.size() > 1048576) {
                    throw new IOException("Thumbnail unavailable");
                }
                this.jpeg = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
            } catch (Throwable th) {
                try {
                    byteArrayOutputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }

        void release() {
            if (this.scaled != null && this.scaled != this.source && !this.scaled.isRecycled()) {
                this.scaled.recycle();
            }
            this.scaled = null;
            this.jpeg = null;
        }
    }

    static void install(final ClassLoader classLoader, String str) {
        if (NativeTargets.systemUi(str)) {
            XposedHelpers.findAndHookMethod(Application.class, "attach", new Object[]{Context.class, new XC_MethodHook() { // from class: nihil.sunshine.parts.ScreenshotWork.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (!methodHookParam.hasThrowable() && !ScreenshotWork.installed) {
                        boolean unused = ScreenshotWork.installed = true;
                        LicenseRuntime.attach((Context) methodHookParam.args[0], "screenshot");
                        ScreenshotWork.bind(classLoader);
                    }
                }
            }});
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void bind(ClassLoader classLoader) {
        ArrayList arrayList = new ArrayList();
        try {
            Class<?> cls = Class.forName("com.android.systemui.screenshot.ScreenshotController", false, classLoader);
            Class<?> cls2 = Class.forName("com.android.systemui.screenshot.ScreenshotController$startCaptureAppRemoteServiceConnection$1", false, classLoader);
            Field field = field(cls2, "this$0");
            Field field2 = field(cls, "screenBitmap");
            Field field3 = field(cls, "screenshotTransactionId");
            Field field4 = field(cls, "isSavingFailed");
            Method declaredMethod = cls2.getDeclaredMethod("onConnectionResult", Boolean.TYPE);
            declaredMethod.setAccessible(true);
            if (field.getType() != cls || field2.getType() != Bitmap.class || field3.getType() != Long.TYPE || field4.getType() != Boolean.TYPE || declaredMethod.getReturnType() != Void.TYPE) {
                throw new NoSuchFieldException("Screenshot ABI");
            }
            QsHotPaths.deoptimize(declaredMethod);
            main = new Handler(Looper.getMainLooper());
            arrayList.add(RuntimeHooks.hookMethod(Bitmap.class.getDeclaredMethod("createScaledBitmap", Bitmap.class, Integer.TYPE, Integer.TYPE, Boolean.TYPE), new XC_MethodHook() { // from class: nihil.sunshine.parts.ScreenshotWork.2
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Frame frame = (Frame) ScreenshotWork.current.get();
                    if (frame == null || frame.scaled == null || methodHookParam.args[0] != frame.source || ((Integer) methodHookParam.args[1]).intValue() != frame.scaled.getWidth() || ((Integer) methodHookParam.args[2]).intValue() != frame.scaled.getHeight() || !Boolean.TRUE.equals(methodHookParam.args[3])) {
                        return;
                    }
                    methodHookParam.setResult(frame.scaled);
                }
            }));
            arrayList.add(RuntimeHooks.hookMethod(Bitmap.class.getDeclaredMethod("compress", Bitmap.CompressFormat.class, Integer.TYPE, OutputStream.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.ScreenshotWork.3
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Frame frame = (Frame) ScreenshotWork.current.get();
                    if (frame == null || frame.jpeg == null || methodHookParam.thisObject != frame.scaled || methodHookParam.args[0] != Bitmap.CompressFormat.JPEG || ((Integer) methodHookParam.args[1]).intValue() != 50 || !(methodHookParam.args[2] instanceof FileOutputStream)) {
                        return;
                    }
                    try {
                        ((OutputStream) methodHookParam.args[2]).write(frame.jpeg);
                        methodHookParam.setResult(Boolean.TRUE);
                    } catch (IOException e) {
                        methodHookParam.setResult(Boolean.FALSE);
                    }
                }
            }));
            arrayList.add(RuntimeHooks.hookMethod(declaredMethod, new AnonymousClass4(field, field2, field4, field3, declaredMethod)));
            if (arrayList.contains(null)) {
                throw new IllegalStateException("Screenshot hook rejected");
            }
        } catch (Throwable th) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                RuntimeHooks.unhook((XC_MethodHook.Unhook) it.next());
            }
        }
    }

    /* JADX INFO: renamed from: nihil.sunshine.parts.ScreenshotWork$4, reason: invalid class name */
    class AnonymousClass4 extends XC_MethodHook {
        final /* synthetic */ Field val$bitmap;
        final /* synthetic */ Field val$controller;
        final /* synthetic */ Field val$failed;
        final /* synthetic */ Method val$result;
        final /* synthetic */ Field val$transaction;

        AnonymousClass4(Field field, Field field2, Field field3, Field field4, Method method) {
            this.val$controller = field;
            this.val$bitmap = field2;
            this.val$failed = field3;
            this.val$transaction = field4;
            this.val$result = method;
        }

        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                boolean z = false;
                if (!Boolean.TRUE.equals(methodHookParam.args[0]) || !ScreenshotWork.budget.tryAcquire()) {
                    return;
                }
                try {
                    Object obj = this.val$controller.get(methodHookParam.thisObject);
                    Bitmap bitmap = (Bitmap) this.val$bitmap.get(obj);
                    if (!this.val$failed.getBoolean(obj) && bitmap != null && !bitmap.isRecycled() && bitmap.getWidth() >= 4 && bitmap.getHeight() >= 4) {
                        final Frame frame = new Frame(methodHookParam.thisObject, obj, bitmap, this.val$transaction.getLong(obj));
                        ExecutorService executorService = ScreenshotWork.worker;
                        final Field field = this.val$transaction;
                        final Field field2 = this.val$bitmap;
                        final Method method = this.val$result;
                        executorService.execute(new Runnable() { // from class: nihil.sunshine.parts.ScreenshotWork$4$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                ScreenshotWork.AnonymousClass4.lambda$beforeHookedMethod$1(frame, field, field2, method);
                            }
                        });
                        z = true;
                        methodHookParam.setResult((Object) null);
                        return;
                    }
                    ScreenshotWork.budget.release();
                } catch (RejectedExecutionException e) {
                    if (!z) {
                        ScreenshotWork.budget.release();
                    }
                } catch (Throwable th) {
                    if (z) {
                        throw th;
                    }
                    ScreenshotWork.budget.release();
                    throw th;
                }
            }
        }

        static /* synthetic */ void lambda$beforeHookedMethod$1(final Frame frame, final Field field, final Field field2, final Method method) {
            try {
                frame.prepare();
            } catch (Exception | OutOfMemoryError e) {
                frame.release();
            }
            if (!ScreenshotWork.main.post(new Runnable() { // from class: nihil.sunshine.parts.ScreenshotWork$4$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ScreenshotWork.AnonymousClass4.lambda$beforeHookedMethod$0(field, frame, field2, method);
                }
            })) {
                frame.release();
                ScreenshotWork.budget.release();
            }
        }

        static /* synthetic */ void lambda$beforeHookedMethod$0(Field field, Frame frame, Field field2, Method method) {
            try {
                if (field.getLong(frame.owner) != frame.transaction || field2.get(frame.owner) != frame.source) {
                    frame.release();
                }
                ScreenshotWork.current.set(frame);
                try {
                    XposedBridge.invokeOriginalMethod(method, frame.callback, new Object[]{Boolean.TRUE});
                    ScreenshotWork.current.remove();
                } catch (Throwable th) {
                    ScreenshotWork.current.remove();
                    throw th;
                }
            } catch (Throwable th2) {
                try {
                    Log.w("SunshinePartsUI", "Native screenshot continuation failed", th2);
                } finally {
                    frame.release();
                    ScreenshotWork.budget.release();
                }
            }
        }
    }

    private static Field field(Class<?> cls, String str) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private ScreenshotWork() {
    }
}
