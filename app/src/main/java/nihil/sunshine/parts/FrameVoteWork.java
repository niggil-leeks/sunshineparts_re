package nihil.sunshine.parts;

import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes.dex */
final class FrameVoteWork {
    static final int FORCED = 1073741824;
    static final int SELF = Integer.MIN_VALUE;
    static boolean active;
    static long calls;
    static long forced;
    static long writes;
    static String error = "";
    static String pinStatus = "Not checked";

    static final class Access {
        final Field flags;
        final Field group;
        final Field preferred;
        final Field readOnly;

        Access(Class<?> cls) throws Exception {
            this.group = FrameVoteWork.field(cls, "sToolkitViewGroupFrameRateApiFlagValue");
            this.readOnly = FrameVoteWork.field(cls, "sToolkitSetFrameRateReadOnlyFlagValue");
            this.preferred = FrameVoteWork.field(cls, "mPreferredFrameRate");
            this.flags = FrameVoteWork.field(cls, "mPrivateFlags4");
            if (this.group.getType() != Boolean.TYPE || this.readOnly.getType() != Boolean.TYPE || this.preferred.getType() != Float.TYPE || this.flags.getType() != Integer.TYPE || !Modifier.isStatic(this.group.getModifiers()) || !Modifier.isStatic(this.readOnly.getModifiers()) || Modifier.isStatic(this.preferred.getModifiers()) || Modifier.isStatic(this.flags.getModifiers())) {
                throw new NoSuchFieldException("Frame vote fields");
            }
        }

        void apply(Object obj, float f) throws Exception {
            if (this.group.getBoolean(null) && (this.flags.getInt(obj) & FrameVoteWork.FORCED) != 0) {
                FrameVoteWork.forced++;
                FrameVoteWork.calls++;
                return;
            }
            if (this.readOnly.getBoolean(null)) {
                this.preferred.setFloat(obj, f);
                FrameVoteWork.writes++;
            }
            if (this.group.getBoolean(null)) {
                boolean zIsNaN = Float.isNaN(this.preferred.getFloat(obj));
                int i = this.flags.getInt(obj);
                this.flags.setInt(obj, !zIsNaN ? FrameVoteWork.SELF | i : Integer.MAX_VALUE & i);
            }
            FrameVoteWork.calls++;
        }
    }

    static Field field(Class<?> cls, String str) throws Exception {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField;
    }

    static XC_MethodHook hook(final Access access) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.FrameVoteWork.1
            boolean failed;

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (this.failed || !VisualPolicy.leanQs() || methodHookParam.thisObject == null) {
                    return;
                }
                try {
                    access.apply(methodHookParam.thisObject, ((Float) methodHookParam.args[0]).floatValue());
                    methodHookParam.setResult((Object) null);
                } catch (Throwable th) {
                    this.failed = true;
                    FrameVoteWork.error = th.getClass().getSimpleName();
                    ControlBus.feature("frame_vote_work", "Native fallback");
                }
            }
        };
    }

    static boolean pinned(String str) {
        FrameworkCode.Result resultInspect = FrameworkCode.inspect(str);
        pinStatus = resultInspect.status;
        return resultInspect.frame;
    }

    static boolean install(ClassLoader classLoader) {
        try {
            if (!pinned("/system/framework/framework.jar")) {
                ControlBus.feature("frame_vote_work", "Native fallback: framework fingerprint not recognized");
                return false;
            }
            Access access = new Access(View.class);
            Method declaredMethod = View.class.getDeclaredMethod("setRequestedFrameRate", Float.TYPE);
            Method declaredMethod2 = ViewGroup.class.getDeclaredMethod("setRequestedFrameRate", Float.TYPE);
            Method declaredMethod3 = Class.forName("androidx.compose.ui.platform.Api35Impl", false, classLoader).getDeclaredMethod("setRequestedFrameRate", View.class, Float.TYPE);
            QsHotPaths.deoptimize(declaredMethod2);
            QsHotPaths.deoptimize(declaredMethod3);
            if (RuntimeHooks.hookMethod(declaredMethod, hook(access)) == null) {
                return false;
            }
            active = true;
            ControlBus.feature("frame_vote_work", "Native frame votes and flags without diagnostic formatting");
            ControlBus.feature("compose_frame_diagnostics", "View vote diagnostic path removed; native ViewGroup forwarding retained");
            return true;
        } catch (Throwable th) {
            error = th.getClass().getSimpleName();
            ControlBus.feature("frame_vote_work", "Native fallback");
            return false;
        }
    }

    private FrameVoteWork() {
    }
}
