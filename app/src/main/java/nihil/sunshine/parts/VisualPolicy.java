package nihil.sunshine.parts;

/* JADX INFO: loaded from: classes.dex */
final class VisualPolicy {
    static boolean nativeAnimations() {
        return true;
    }

    static float motionScale() {
        int iLevel = AdaptiveWorkload.level();
        if (iLevel == 1 || iLevel == 3) {
            return 0.8f;
        }
        return iLevel == 2 ? 0.5f : 1.0f;
    }

    static boolean nativeBlur() {
        return AdaptiveWorkload.level() > 0 || Config.nativeBlur;
    }

    static boolean preparedBlur() {
        if (AdaptiveWorkload.level() == 3 || AdaptiveWorkload.level() == 1) {
            return true;
        }
        return AdaptiveWorkload.level() == 0 && Config.preparedBlur && nativeBlur();
    }

    static boolean capturePanel() {
        return AdaptiveWorkload.level() != 3;
    }

    static boolean workerBlur() {
        return nativeBlur() && (AdaptiveWorkload.level() > 0 || preparedBlur());
    }

    static boolean tileLayout() {
        return AdaptiveWorkload.level() > 0 || Config.tileLayout;
    }

    static boolean tileWork() {
        return AdaptiveWorkload.level() > 0 || Config.tileWork;
    }

    static boolean leanQs() {
        return AdaptiveWorkload.level() > 0 || Config.leanQs;
    }

    static boolean managed(String str, int i) {
        if ("native_animations".equals(str) || "gpu_blur".equals(str) || "compose_layout".equals(str) || "compose_host".equals(str)) {
            return true;
        }
        return i > 0 && ("native_animations".equals(str) || "native_blur".equals(str) || "prepared_blur".equals(str) || "lean_qs".equals(str) || "media_step".equals(str) || "tile_layout".equals(str) || "tile_work".equals(str));
    }

    private VisualPolicy() {
    }
}
