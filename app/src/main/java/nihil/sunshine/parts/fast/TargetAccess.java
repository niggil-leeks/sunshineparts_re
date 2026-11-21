package nihil.sunshine.parts.fast;

import androidx.compose.animation.core.Animatable;
import androidx.compose.animation.core.DeferredTargetAnimation;
import java.util.Objects;
import java.util.function.BiFunction;

/* JADX INFO: loaded from: /home/fwlta/Sunshine_Parts/app/src/main/assets/qs-target.dex */
public final class TargetAccess implements BiFunction<Object, Object, Object> {
    private final Object miss;

    public TargetAccess(Object obj) {
        this.miss = obj;
    }

    @Override // java.util.function.BiFunction
    public Object apply(Object obj, Object obj2) {
        DeferredTargetAnimation deferredTargetAnimation = (DeferredTargetAnimation) obj;
        Animatable animatable = deferredTargetAnimation.animatable;
        if (animatable == null) {
            return this.miss;
        }
        if (!Objects.equals(deferredTargetAnimation._pendingTarget$delegate.getValue(), obj2) || !Objects.equals(animatable.targetValue$delegate.getValue(), obj2)) {
            return this.miss;
        }
        return animatable.internalState.getValue();
    }
}
