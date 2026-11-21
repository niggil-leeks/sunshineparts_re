package nihil.sunshine.parts;

import dalvik.system.InMemoryDexClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;

/* JADX INFO: loaded from: classes.dex */
final class FastTargetAccess {
    static BiFunction<Object, Object, Object> load(ClassLoader classLoader, Object obj) {
        return load(classLoader, obj, "nihil.sunshine.parts.fast.TargetAccess");
    }

    static BiFunction<Object, Object, Object> loadNav(ClassLoader classLoader) {
        return load(classLoader, null, "nihil.sunshine.parts.fast.NavSelection");
    }

    private static BiFunction<Object, Object, Object> load(ClassLoader classLoader, Object obj, String str) {
        try {
            InputStream resourceAsStream = FastTargetAccess.class.getClassLoader().getResourceAsStream("assets/qs-target.dex");
            if (resourceAsStream == null) {
                if (resourceAsStream != null) {
                    resourceAsStream.close();
                }
                return null;
            }
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[4096];
                do {
                    int i = resourceAsStream.read(bArr);
                    if (i == -1) {
                        BiFunction<Object, Object, Object> biFunction = (BiFunction) Class.forName(str, true, new InMemoryDexClassLoader(ByteBuffer.wrap(byteArrayOutputStream.toByteArray()), classLoader)).getConstructor(Object.class).newInstance(obj);
                        if (resourceAsStream != null) {
                            resourceAsStream.close();
                        }
                        return biFunction;
                    }
                    byteArrayOutputStream.write(bArr, 0, i);
                    return null;
                } while (byteArrayOutputStream.size() <= 65536);
                if (resourceAsStream != null) {
                    resourceAsStream.close();
                }
                return null;
            } catch (Throwable th) {
                if (resourceAsStream != null) {
                    try {
                        resourceAsStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (Throwable th3) {
            return null;
        }
    }

    private FastTargetAccess() {
    }
}
