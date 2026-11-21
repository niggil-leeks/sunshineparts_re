package nihil.sunshine.parts.fast;

import com.android.systemui.navigationbar.bandaid.Band;
import com.android.systemui.navigationbar.bandaid.BandAidPack;
import com.android.systemui.navigationbar.store.NavBarModuleDependency;
import com.android.systemui.navigationbar.store.NavBarStoreImpl;
import com.android.systemui.navigationbar.store.NavBarStoreImpl$;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

/* JADX INFO: loaded from: /home/fwlta/Sunshine_Parts/app/src/main/assets/qs-target.dex */
public final class NavSelection implements BiFunction<Object, Object, Object> {
    private static final NameCache NAMES;
    private static final Comparator<Band> PRIORITY;
    private static final String REFLECT = "com.android.systemui.navigationbar.util.NavBarReflectUtil";

    private static final class Names {
        final String enclosing;
        final String name;
        final boolean reflect;

        Names(Class<?> cls) {
            this.name = cls.getTypeName();
            Class<?> enclosingClass = cls.getEnclosingClass();
            this.enclosing = enclosingClass == null ? null : enclosingClass.getTypeName();
            this.reflect = cls.toString().contains(NavSelection.REFLECT);
        }
    }

    private static final class NameCache extends ClassValue<Names> {
        private NameCache() {
        }

        @Override // java.lang.ClassValue
        protected /* bridge */ /* synthetic */ Names computeValue(Class cls) {
            return computeValue2((Class<?>) cls);
        }

        @Override // java.lang.ClassValue
        /* JADX INFO: renamed from: computeValue, reason: avoid collision after fix types in other method */
        protected Names computeValue2(Class<?> cls) {
            return new Names(cls);
        }
    }

    private static final class Priority implements Comparator<Band> {
        private Priority() {
        }

        @Override // java.util.Comparator
        public int compare(Band band, Band band2) {
            return Integer.compare(band.priority, band2.priority);
        }
    }

    static {
        NAMES = new NameCache();
        PRIORITY = new Priority();
    }

    public NavSelection(Object obj) {
    }

    private static boolean standard(List<?> list) {
        String name = list.getClass().getName();
        return name.equals("java.util.ArrayList") || name.equals("java.util.Arrays$ArrayList") || name.equals("java.util.Collections$SingletonList") || name.equals("java.util.Collections$EmptyList") || name.startsWith("java.util.ImmutableCollections$") || name.equals("kotlin.collections.EmptyList");
    }

    private static boolean classes(List<Type> list) {
        Iterator<Type> it = list.iterator();
        while (it.hasNext()) {
            if (!(it.next() instanceof Class)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(Band band, NavBarStoreImpl$.ExternalSyntheticLambda1 externalSyntheticLambda1, Names names, Names names2) {
        boolean z;
        boolean z2;
        if (!band.bandAidDependency.getEnabled() || !band.runeDependency) {
            return false;
        }
        String str = band.sPluginTag;
        int i = band.targetDisplayId;
        if (str.length() > 0) {
            if (names == null) {
                return ((Boolean) externalSyntheticLambda1.invoke(band)).booleanValue();
            }
            return names.name.contains(str) && i == externalSyntheticLambda1.f$3;
        }
        if (i != -1 && i != externalSyntheticLambda1.f$3) {
            return false;
        }
        if (names == null || !classes(band.targetEvents)) {
            return ((Boolean) externalSyntheticLambda1.invoke(band)).booleanValue();
        }
        Iterator it = band.targetEvents.iterator();
        while (true) {
            if (!it.hasNext()) {
                z = false;
                break;
            }
            if (NAMES.get((Class) ((Type) it.next())).name.equals(names.name)) {
                z = true;
                break;
            }
        }
        if (!z) {
            return false;
        }
        if (names2 == null || !classes(band.targetModules)) {
            return ((Boolean) externalSyntheticLambda1.invoke(band)).booleanValue();
        }
        Iterator it2 = band.targetModules.iterator();
        while (true) {
            if (!it2.hasNext()) {
                z2 = false;
                break;
            }
            Names names3 = NAMES.get((Class) ((Type) it2.next()));
            if (names3.reflect || names2.name.equals(names3.name) || names2.name.contains(names3.name) || Objects.equals(names2.enclosing, names3.name)) {
                z2 = true;
                break;
            }
        }
        if (!z2) {
            return false;
        }
        if (!classes(band.moduleDependencies)) {
            return ((Boolean) externalSyntheticLambda1.invoke(band)).booleanValue();
        }
        for (Type type : band.moduleDependencies) {
            NavBarModuleDependency navBarModuleDependency = (NavBarModuleDependency) externalSyntheticLambda1.f$0.navDependencies.get(Integer.valueOf(externalSyntheticLambda1.f$3));
            if (navBarModuleDependency == null || navBarModuleDependency.modules.get(NAMES.get((Class) type).name) == null) {
                return false;
            }
        }
        return true;
    }

    @Override // java.util.function.BiFunction
    public Object apply(Object obj, Object obj2) {
        NavBarStoreImpl$.ExternalSyntheticLambda1 externalSyntheticLambda1 = ((NavBarStoreImpl.sam.java_util_function_Predicate.0) obj2).function;
        if (!(externalSyntheticLambda1 instanceof NavBarStoreImpl$.ExternalSyntheticLambda1)) {
            throw new LinkageError("Unknown Retriever predicate");
        }
        final NavBarStoreImpl$.ExternalSyntheticLambda1 externalSyntheticLambda2 = externalSyntheticLambda1;
        Names names = externalSyntheticLambda2.f$2 == null ? null : NAMES.get(externalSyntheticLambda2.f$2.getClass());
        Names names2 = externalSyntheticLambda2.f$1 != null ? NAMES.get(externalSyntheticLambda2.f$1.getClass()) : null;
        final ArrayList arrayList = new ArrayList();
        Iterator it = ((List) obj).iterator();
        while (it.hasNext()) {
            List<Band> bands = ((BandAidPack) it.next()).getBands();
            if (standard(bands)) {
                for (Band band : bands) {
                    if (matches(band, externalSyntheticLambda2, names, names2)) {
                        arrayList.add(band);
                    }
                }
            } else {
                Stream stream = bands.stream();
                try {
                    ((Stream) stream.sequential()).forEachOrdered(new Consumer() { // from class: nihil.sunshine.parts.fast.NavSelection$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj3) {
                            NavSelection.lambda$apply$0(externalSyntheticLambda2, arrayList, (Band) obj3);
                        }
                    });
                    if (stream != null) {
                        stream.close();
                    }
                } catch (Throwable th) {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            }
        }
        arrayList.sort(PRIORITY);
        return arrayList;
    }

    static /* synthetic */ void lambda$apply$0(NavBarStoreImpl$.ExternalSyntheticLambda1 externalSyntheticLambda1, ArrayList arrayList, Band band) {
        if (((Boolean) externalSyntheticLambda1.invoke(band)).booleanValue()) {
            arrayList.add(band);
        }
    }
}
