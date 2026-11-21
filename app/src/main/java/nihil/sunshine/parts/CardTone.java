package nihil.sunshine.parts;

import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
final class CardTone {
    private static final String TAG = "SunshinePartsUI";
    private static final Set<View> toned = Collections.newSetFromMap(new WeakHashMap());

    CardTone() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean light(View view) {
        return (view.getResources().getConfiguration().uiMode & 48) != 32;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean tooBright(int i) {
        return (i >>> 24) > 200 && ((i >>> 16) & 255) > 220 && ((i >>> 8) & 255) > 220 && (i & 255) > 220;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void tone(View view) {
        ColorStateList color;
        if (!Config.lightTone || !light(view) || !NoShadeMotion.region(view)) {
            return;
        }
        Drawable background = view.getBackground();
        if ((background instanceof ColorDrawable) && tooBright(((ColorDrawable) background).getColor())) {
            ((ColorDrawable) background.mutate()).setColor(-5459012);
        } else if ((background instanceof GradientDrawable) && (color = ((GradientDrawable) background).getColor()) != null && !color.isStateful() && tooBright(color.getDefaultColor())) {
            ((GradientDrawable) background.mutate()).setColor(-5459012);
        }
        ColorStateList backgroundTintList = view.getBackgroundTintList();
        if (backgroundTintList != null && !backgroundTintList.isStateful() && tooBright(backgroundTintList.getDefaultColor())) {
            view.setBackgroundTintList(ColorStateList.valueOf(-5459012));
        }
    }

    static void install() {
        try {
            RuntimeHooks.hookMethod(View.class.getDeclaredMethod("setBackgroundColor", Integer.TYPE), new XC_MethodHook() { // from class: nihil.sunshine.parts.CardTone.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (!Config.lightTone || !CardTone.tooBright(((Integer) methodHookParam.args[0]).intValue()) || !CardTone.light((View) methodHookParam.thisObject) || !NoShadeMotion.region((View) methodHookParam.thisObject)) {
                        return;
                    }
                    methodHookParam.args[0] = -5459012;
                }
            });
            RuntimeHooks.hookMethod(View.class.getDeclaredMethod("setBackgroundTintList", ColorStateList.class), new XC_MethodHook() { // from class: nihil.sunshine.parts.CardTone.2
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    ColorStateList colorStateList = (ColorStateList) methodHookParam.args[0];
                    if (Config.lightTone && colorStateList != null && !colorStateList.isStateful() && CardTone.tooBright(colorStateList.getDefaultColor()) && CardTone.light((View) methodHookParam.thisObject) && NoShadeMotion.region((View) methodHookParam.thisObject)) {
                        methodHookParam.args[0] = ColorStateList.valueOf(-5459012);
                    }
                }
            });
        } catch (Throwable th) {
        }
        try {
            RuntimeHooks.hookMethod(View.class.getDeclaredMethod("onAttachedToWindow", new Class[0]), new XC_MethodHook() { // from class: nihil.sunshine.parts.CardTone.3
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (Config.lightTone) {
                        View view = (View) methodHookParam.thisObject;
                        if (CardTone.toned.add(view)) {
                            try {
                                CardTone.tone(view);
                            } catch (Throwable th2) {
                            }
                        }
                    }
                }
            });
        } catch (Throwable th2) {
            Log.w(TAG, "Optional card tone path unavailable");
        }
    }
}
