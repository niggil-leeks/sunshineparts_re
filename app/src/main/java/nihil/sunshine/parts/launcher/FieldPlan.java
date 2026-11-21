package nihil.sunshine.parts.launcher;

import java.lang.reflect.Field;

/* JADX INFO: loaded from: classes.dex */
final class FieldPlan {
    private final Field[] fields;

    FieldPlan(Class<?> cls, String... strArr) throws ReflectiveOperationException {
        this.fields = new Field[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            for (Class<?> superclass = cls; superclass != null; superclass = superclass.getSuperclass()) {
                try {
                    this.fields[i] = superclass.getDeclaredField(strArr[i]);
                    break;
                } catch (NoSuchFieldException e) {
                }
            }
            if (this.fields[i] == null) {
                throw new NoSuchFieldException(strArr[i]);
            }
            this.fields[i].setAccessible(true);
        }
    }

    void apply(Object obj, Object... objArr) throws ReflectiveOperationException {
        if (objArr.length != this.fields.length) {
            throw new IllegalArgumentException("Field count");
        }
        Object[] objArr2 = new Object[this.fields.length];
        for (int i = 0; i < this.fields.length; i++) {
            objArr2[i] = this.fields[i].get(obj);
        }
        for (int i2 = 0; i2 < this.fields.length; i2++) {
            try {
                this.fields[i2].set(obj, objArr[i2]);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                for (int i3 = i2 - 1; i3 >= 0; i3--) {
                    try {
                        this.fields[i3].set(obj, objArr2[i3]);
                    } catch (Exception e2) {
                    }
                }
                throw e;
            }
        }
    }
}
