package nihil.sunshine.parts;

import android.os.Looper;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

/* JADX INFO: loaded from: classes.dex */
final class NavDispatchWork {
    private static final String EXTRA = "sunshine.nav.packs";
    static final String PREFIX = "com.android.systemui.navigationbar.store.NavBarStoreImpl$";

    static final class Swap {
        final List<?> original;
        final Object store;
        final Packs wrapped;

        Swap(Object obj, List<?> list, Packs packs) {
            this.store = obj;
            this.original = list;
            this.wrapped = packs;
        }
    }

    static final class Packs extends AbstractList<Object> {
        final BiFunction<Object, Object, Object> select;
        final List<Object> source;

        Packs(List<?> list, BiFunction<Object, Object, Object> biFunction) {
            this.source = list;
            this.select = biFunction;
        }

        @Override // java.util.AbstractList, java.util.List
        public Object get(int i) {
            return this.source.get(i);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public int size() {
            return this.source.size();
        }

        @Override // java.util.AbstractList, java.util.List
        public void add(int i, Object obj) {
            this.source.add(i, obj);
        }

        @Override // java.util.AbstractList, java.util.List
        public Object set(int i, Object obj) {
            return this.source.set(i, obj);
        }

        @Override // java.util.AbstractList, java.util.AbstractCollection, java.util.Collection, java.util.List
        public void clear() {
            this.source.clear();
        }

        @Override // java.util.AbstractList, java.util.List
        public Object remove(int i) {
            return this.source.remove(i);
        }

        @Override // java.util.AbstractList, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.List
        public Iterator<Object> iterator() {
            return this.source.iterator();
        }

        @Override // java.util.AbstractList, java.util.List
        public ListIterator<Object> listIterator(int i) {
            return this.source.listIterator(i);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public boolean addAll(Collection<?> collection) {
            return this.source.addAll(collection);
        }

        @Override // java.util.AbstractList, java.util.List
        public boolean addAll(int i, Collection<?> collection) {
            return this.source.addAll(i, collection);
        }

        @Override // java.util.Collection
        public Stream<Object> stream() {
            return Looper.myLooper() == Looper.getMainLooper() ? Pipeline.create(this.source, this.select) : this.source.stream();
        }

        @Override // java.util.Collection
        public Stream<Object> parallelStream() {
            return this.source.parallelStream();
        }
    }

    static final class Pipeline implements InvocationHandler {
        private static volatile Field mapperField;
        private static volatile Field orderField;
        boolean consumed;
        Stream<?> delegate;
        Function<Object, Stream<?>> mapper;
        Comparator<Object> order;
        Predicate<Object> predicate;
        final BiFunction<Object, Object, Object> select;
        final List<?> source;
        int stage;

        static boolean nativeFunction(Object obj, int i) {
            try {
                Field declaredField = i == 0 ? mapperField : orderField;
                if (declaredField == null || declaredField.getDeclaringClass() != obj.getClass()) {
                    declaredField = obj.getClass().getDeclaredField("function");
                    declaredField.setAccessible(true);
                    if (i == 0) {
                        mapperField = declaredField;
                    } else {
                        orderField = declaredField;
                    }
                }
                Object obj2 = declaredField.get(obj);
                return obj2 != null && obj2.getClass().getName().equals(new StringBuilder().append("com.android.systemui.navigationbar.store.NavBarStoreImpl$$ExternalSyntheticLambda").append(i).toString());
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        Pipeline(List<?> list, BiFunction<Object, Object, Object> biFunction) {
            this.source = list;
            this.select = biFunction;
        }

        static Stream<Object> create(List<?> list, BiFunction<Object, Object, Object> biFunction) {
            return (Stream) Proxy.newProxyInstance(Stream.class.getClassLoader(), new Class[]{Stream.class}, new Pipeline(list, biFunction));
        }

        Stream fallback() {
            if (this.delegate == null) {
                Stream<?> stream = this.source.stream();
                if (this.mapper != null) {
                    stream = stream.flatMap(this.mapper);
                }
                if (this.predicate != null) {
                    stream = stream.filter(this.predicate);
                }
                if (this.order != null) {
                    stream = stream.sorted(this.order);
                }
                this.delegate = stream;
            }
            return this.delegate;
        }

        @Override // java.lang.reflect.InvocationHandler
        public Object invoke(Object obj, Method method, Object[] objArr) throws Throwable {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                if (name.equals("equals")) {
                    return Boolean.valueOf(obj == objArr[0]);
                }
                return name.equals("hashCode") ? Integer.valueOf(System.identityHashCode(obj)) : "Sunshine native rule selection";
            }
            if (this.delegate != null) {
                return invokeNative(method, this.delegate, objArr);
            }
            if (this.consumed) {
                throw new IllegalStateException("Stream has already been consumed");
            }
            if (name.equals("flatMap") && this.stage == 0 && objArr[0].getClass().getName().equals("com.android.systemui.navigationbar.store.NavBarStoreImpl$sam$java_util_function_Function$0") && nativeFunction(objArr[0], 0)) {
                this.mapper = (Function) objArr[0];
                this.stage = 1;
                return obj;
            }
            if (name.equals("filter") && this.stage == 1 && objArr[0].getClass().getName().equals("com.android.systemui.navigationbar.store.NavBarStoreImpl$sam$java_util_function_Predicate$0")) {
                this.predicate = (Predicate) objArr[0];
                this.stage = 2;
                return obj;
            }
            if (name.equals("sorted") && this.stage == 2 && objArr != null && objArr.length == 1 && objArr[0].getClass().getName().equals("com.android.systemui.navigationbar.store.NavBarStoreImpl$sam$java_util_Comparator$0") && nativeFunction(objArr[0], 2)) {
                this.order = (Comparator) objArr[0];
                this.stage = 3;
                return obj;
            }
            if (name.equals("collect") && this.stage == 3 && objArr.length == 1 && (objArr[0] instanceof Collector)) {
                try {
                    List list = (List) this.select.apply(this.source, this.predicate);
                    this.consumed = true;
                    Collector collector = (Collector) objArr[0];
                    Object obj2 = collector.supplier().get();
                    BiConsumer biConsumerAccumulator = collector.accumulator();
                    Iterator it = list.iterator();
                    while (it.hasNext()) {
                        biConsumerAccumulator.accept(obj2, it.next());
                    }
                    return collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) ? obj2 : collector.finisher().apply(obj2);
                } catch (LinkageError e) {
                    return invokeNative(method, fallback(), objArr);
                }
            }
            return invokeNative(method, fallback(), objArr);
        }

        private static Object invokeNative(Method method, Object obj, Object[] objArr) throws Throwable {
            try {
                return method.invoke(obj, objArr);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    static XC_MethodHook dispatchHook(final Field field, final BiFunction<Object, Object, Object> biFunction) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.NavDispatchWork.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (!VisualPolicy.leanQs() || Looper.myLooper() != Looper.getMainLooper()) {
                    return;
                }
                try {
                    Object obj = field.get(methodHookParam.thisObject);
                    if (obj != null && obj.getClass() == ArrayList.class) {
                        Packs packs = new Packs((List) obj, biFunction);
                        field.set(methodHookParam.thisObject, packs);
                        methodHookParam.setObjectExtra(NavDispatchWork.EXTRA, new Swap(methodHookParam.thisObject, (List) obj, packs));
                    }
                } catch (Throwable th) {
                }
            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objectExtra = methodHookParam.getObjectExtra(NavDispatchWork.EXTRA);
                if (objectExtra instanceof Swap) {
                    Swap swap = (Swap) objectExtra;
                    try {
                        if (field.get(swap.store) == swap.wrapped) {
                            field.set(swap.store, swap.original);
                        }
                    } catch (Throwable th) {
                    }
                }
            }
        };
    }

    static XC_MethodHook comparatorHook(final Field field) {
        return new XC_MethodHook() { // from class: nihil.sunshine.parts.NavDispatchWork.2
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                methodHookParam.setResult(Integer.valueOf(Integer.compare(field.getInt(methodHookParam.args[0]), field.getInt(methodHookParam.args[1]))));
            }
        };
    }

    static void install(ClassLoader classLoader, String str) {
        try {
            if (!DensityQueries.pinned(str) || !IconDimensions.pinned(str)) {
                throw new IllegalStateException("Unknown System UI code");
            }
            Class<?> cls = Class.forName("com.android.systemui.navigationbar.store.NavBarStoreImpl", false, classLoader);
            Class<?> cls2 = Class.forName("com.android.systemui.navigationbar.store.EventTypeFactory$EventType", false, classLoader);
            Class<?> cls3 = Class.forName("com.android.systemui.navigationbar.bandaid.Band", false, classLoader);
            Class<?> cls4 = Class.forName("com.android.systemui.navigationbar.store.NavBarStoreImpl$sam$java_util_Comparator$0", false, classLoader);
            final Field declaredField = cls3.getDeclaredField("priority");
            declaredField.setAccessible(true);
            Method declaredMethod = cls4.getDeclaredMethod("compare", Object.class, Object.class);
            if (declaredField.getType() != Integer.TYPE || declaredMethod.getReturnType() != Integer.TYPE) {
                throw new NoSuchFieldException("Priority ABI");
            }
            final Field declaredField2 = cls4.getDeclaredField("function");
            declaredField2.setAccessible(true);
            final Class<?> cls5 = Class.forName("com.android.systemui.navigationbar.store.NavBarStoreImpl$$ExternalSyntheticLambda2", false, classLoader);
            if (RuntimeHooks.hookMethod(declaredMethod, new XC_MethodHook() { // from class: nihil.sunshine.parts.NavDispatchWork.3
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                    Object obj = declaredField2.get(methodHookParam.thisObject);
                    if (obj == null || obj.getClass() != cls5) {
                        return;
                    }
                    methodHookParam.setResult(Integer.valueOf(Integer.compare(declaredField.getInt(methodHookParam.args[0]), declaredField.getInt(methodHookParam.args[1]))));
                }
            }) == null) {
                throw new IllegalStateException("Comparator hook rejected");
            }
            Field declaredField3 = cls.getDeclaredField("packs");
            declaredField3.setAccessible(true);
            Method declaredMethod2 = cls.getDeclaredMethod("handleEvent", Object.class, cls2, Integer.TYPE, Object.class);
            if (declaredField3.getType() != List.class || Modifier.isFinal(declaredField3.getModifiers()) || declaredMethod2.getReturnType() != Object.class) {
                throw new NoSuchFieldException("Dispatcher ABI");
            }
            BiFunction<Object, Object, Object> biFunctionLoadNav = FastTargetAccess.loadNav(classLoader);
            if (biFunctionLoadNav == null) {
                throw new IllegalStateException("Adapter unavailable");
            }
            if (RuntimeHooks.hookMethod(declaredMethod2, dispatchHook(declaredField3, biFunctionLoadNav)) == null) {
                throw new IllegalStateException("Dispatcher hook rejected");
            }
        } catch (Throwable th) {
        }
    }

    private NavDispatchWork() {
    }
}
