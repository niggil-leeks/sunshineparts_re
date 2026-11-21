package nihil.lain.sunshine;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicLong;
import nihil.sunshine.parts.RuntimeHooks;

/* JADX INFO: loaded from: classes2.dex */
final class MediaWave {
    static final String CLASS = "com.android.systemui.media.audiovisseekbar.renderer.track.auto.MultiWaveAreaTrackRenderer";
    static final int OPTIMIZED_STEP = 10;
    static final int STOCK_STEP = 2;
    private final Constructor<?> constructor;
    private volatile boolean failed;
    private final Field step;
    private final AtomicLong adjusted = new AtomicLong();
    private final AtomicLong preserved = new AtomicLong();

    MediaWave(ClassLoader classLoader) throws Exception {
        Class<?> cls = Class.forName(CLASS, false, classLoader);
        this.constructor = cls.getDeclaredConstructor(View.class, Class.forName("com.android.systemui.media.audiovisseekbar.config.AudioVisSeekBarConfig", false, classLoader));
        Field field = SunshineModule.field(cls, "stepX");
        this.step = field;
        if (field.getType() != Integer.TYPE || Modifier.isStatic(field.getModifiers())) {
            throw new NoSuchFieldException("stepX must be an instance int");
        }
    }

    void install() {
        RuntimeHooks.hookMethod(this.constructor, new XC_MethodHook() { // from class: nihil.lain.sunshine.MediaWave.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (MediaWave.this.failed || methodHookParam.hasThrowable()) {
                    return;
                }
                try {
                    if (MediaWave.this.step.getInt(methodHookParam.thisObject) != MediaWave.STOCK_STEP) {
                        MediaWave.this.preserved.incrementAndGet();
                        return;
                    }
                    MediaWave.this.step.setInt(methodHookParam.thisObject, MediaWave.OPTIMIZED_STEP);
                    if (MediaWave.this.step.getInt(methodHookParam.thisObject) != MediaWave.OPTIMIZED_STEP) {
                        throw new IllegalStateException("stepX write not visible");
                    }
                    if (MediaWave.this.adjusted.incrementAndGet() == 1) {
                        SunshineModule.log("media-wave ativo: stepX 2 -> 10; animacao preservada");
                    }
                } catch (Throwable th) {
                    MediaWave.this.failed = true;
                    SunshineModule.log("media-wave desativado: " + th.getClass().getSimpleName());
                }
            }
        });
    }

    String report() {
        StringBuilder sb = new StringBuilder("musicWave=");
        sb.append(this.failed ? "falha" : "ok");
        sb.append(" waveStep=10 waveRenderers=");
        sb.append(this.adjusted.get());
        sb.append(" wavePreservados=");
        sb.append(this.preserved.get());
        return sb.toString();
    }
}
