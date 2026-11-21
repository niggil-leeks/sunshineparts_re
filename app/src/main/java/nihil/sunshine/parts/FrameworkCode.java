package nihil.sunshine.parts;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* JADX INFO: loaded from: classes.dex */
final class FrameworkCode {
    private static Result cached;
    private static String cachedPath;
    static final String[] METHODS = {"Landroid/view/View;->setRequestedFrameRate(F)V", "Landroid/view/View;->getForcedOverrideFrameRateFlag()Z", "Landroid/view/View;->setSelfRequestedFrameRateFlag(Z)V", "Landroid/view/ViewGroup;->setRequestedFrameRate(F)V", "Landroid/view/SurfaceControl$Transaction;->apply(ZZ)V", "Landroid/view/SurfaceControlRegistry;->checkCallStackDebugging(Ljava/lang/String;Landroid/view/SurfaceControl$Transaction;Landroid/view/SurfaceControl;Ljava/lang/String;Z)V", "Landroid/view/View;->setSystemGestureExclusionRects(Ljava/util/List;)V"};
    static final String[] EXPECTED = {"fcc1cc7bd650974ffccfca85e6f0a581f198ad6def611cdbdc21145269787f0c", "a20c7a4d8ba7ff7605201e8fbd4beef63d3d05395ae9154606fe9794b67ae0b8", "cad5d3bfba7982e918c9d69edec4f120300904404a8a8f5f67e88fa05e08ee7d", "79ab21f71df4ffdcd98b299236a2e877b466c969e7fdee9d1cf68c9d0a3444e1", "6e77fd6928b7400c5c5469fe7c672d5127bb90cb09801a862170223ed9513cd3", "a521e319ea225843617ad99e32e67bcbb297808781f269d67cb52ed31a3b5335", "97ec0b6587c834836ac3a1580291613eeb41602319ef453d47dd09925b6fda59"};

    static final class Result {
        final boolean frame;
        final boolean gesture;
        final String status;
        final boolean surface;

        Result(boolean z, boolean z2, boolean z3, String str) {
            this.frame = z;
            this.surface = z2;
            this.gesture = z3;
            this.status = str;
        }
    }

    static synchronized Result inspect(String str) {
        Result result;
        if (str.equals(cachedPath) && cached != null) {
            return cached;
        }
        try {
            ZipFile zipFile = new ZipFile(str);
            try {
                ZipEntry entry = zipFile.getEntry("classes4.dex");
                if (entry == null || entry.getSize() < 112 || entry.getSize() > 16777216) {
                    throw new IOException("DEX size");
                }
                InputStream inputStream = zipFile.getInputStream(entry);
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    try {
                        byte[] bArr = new byte[8192];
                        while (true) {
                            int i = inputStream.read(bArr);
                            if (i == -1) {
                                byte[] byteArray = byteArrayOutputStream.toByteArray();
                                byteArrayOutputStream.close();
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                result = verify(byteArray);
                                zipFile.close();
                                break;
                            }
                            if (byteArrayOutputStream.size() + i > 16777216) {
                                throw new IOException("DEX size");
                            }
                            byteArrayOutputStream.write(bArr, 0, i);
                        }
                        cachedPath = str;
                        cached = result;
                        return result;
                    } catch (Throwable th) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable th4) {
                            th3.addSuppressed(th4);
                        }
                    }
                    throw th3;
                }
            } catch (Throwable th5) {
                try {
                    zipFile.close();
                } catch (Throwable th6) {
                    th5.addSuppressed(th6);
                }
                throw th5;
            }
        } catch (Exception e) {
            result = new Result(false, false, false, "Native fallback: " + e.getClass().getSimpleName());
        }
    }

    static Result verify(byte[] bArr) {
        try {
            Map<String, String> mapFingerprints = new Dex(bArr).fingerprints();
            int length = METHODS.length;
            boolean[] zArr = new boolean[length];
            for (int i = 0; i < length; i++) {
                zArr[i] = EXPECTED[i].equals(mapFingerprints.get(METHODS[i]));
            }
            boolean z = true;
            boolean z2 = zArr[0] && zArr[1] && zArr[2] && zArr[3];
            if (!zArr[4] || !zArr[5]) {
                z = false;
            }
            boolean z3 = zArr[6];
            return new Result(z2, z, z3, "Method code: frame=" + z2 + " surface=" + z + " gesture=" + z3);
        } catch (Exception e) {
            return new Result(false, false, false, "Native fallback: " + e.getClass().getSimpleName());
        }
    }

    static final class Dex {
        final byte[] b;
        final int cc;
        final int co;
        final int fc;
        final int fo;
        final int mc;
        final int mo;
        final int pc;
        final int po;
        final int sc;
        final int so;
        final Map<Integer, String> strings = new HashMap();
        final int tc;
        final int to;

        Dex(byte[] bArr) throws IOException {
            this.b = bArr;
            range(0, 112);
            if (bArr[0] != 100 || bArr[1] != 101 || bArr[2] != 120 || bArr[3] != 10 || bArr[7] != 0 || u32(32) != bArr.length || u32(36) != 112 || u32(40) != 305419896) {
                throw new IOException("DEX header");
            }
            this.sc = u32(56);
            this.so = u32(60);
            table(this.sc, this.so, 4);
            this.tc = u32(64);
            this.to = u32(68);
            table(this.tc, this.to, 4);
            this.pc = u32(72);
            this.po = u32(76);
            table(this.pc, this.po, 12);
            this.fc = u32(80);
            this.fo = u32(84);
            table(this.fc, this.fo, 8);
            this.mc = u32(88);
            this.mo = u32(92);
            table(this.mc, this.mo, 8);
            this.cc = u32(96);
            this.co = u32(100);
            table(this.cc, this.co, 32);
        }

        void range(int i, int i2) throws IOException {
            if (i < 0 || i2 < 0 || ((long) i) + ((long) i2) > this.b.length) {
                throw new IOException("DEX bounds");
            }
        }

        void table(int i, int i2, int i3) throws IOException {
            if (i < 0 || i > this.b.length / i3) {
                throw new IOException("DEX table");
            }
            range(i2, i * i3);
        }

        int u16(int i) throws IOException {
            range(i, 2);
            return ((this.b[i + 1] & 255) << 8) | (this.b[i] & 255);
        }

        int u32(int i) throws IOException {
            range(i, 4);
            return (u16(i + 2) << 16) | u16(i);
        }

        int index(int i, int i2) throws IOException {
            if (i < 0 || i >= i2) {
                throw new IOException("DEX index");
            }
            return i;
        }

        int leb(int[] iArr) throws IOException {
            int i = 0;
            for (int i2 = 0; i2 < 5; i2++) {
                range(iArr[0], 1);
                byte[] bArr = this.b;
                int i3 = iArr[0];
                iArr[0] = i3 + 1;
                int i4 = bArr[i3] & 255;
                if (i2 == 4 && (i4 & 248) != 0) {
                    throw new IOException("DEX LEB");
                }
                i |= (i4 & 127) << (i2 * 7);
                if (i4 < 128) {
                    return i;
                }
            }
            throw new IOException("DEX LEB");
        }

        String string(int i) throws IOException {
            index(i, this.sc);
            String str = this.strings.get(Integer.valueOf(i));
            if (str != null) {
                return str;
            }
            int[] iArr = {u32(this.so + (i * 4))};
            leb(iArr);
            int i2 = iArr[0];
            do {
                range(iArr[0], 1);
                byte[] bArr = this.b;
                int i3 = iArr[0];
                iArr[0] = i3 + 1;
                if (bArr[i3] == 0) {
                    String strHex = FrameworkCode.hex(this.b, i2, (iArr[0] - i2) - 1);
                    this.strings.put(Integer.valueOf(i), strHex);
                    return strHex;
                }
            } while (iArr[0] - i2 <= 65536);
            throw new IOException("DEX string");
        }

        String text(int i) throws IOException {
            String strString = string(i);
            int length = strString.length() / 2;
            byte[] bArr = new byte[length];
            for (int i2 = 0; i2 < length; i2++) {
                int i3 = i2 * 2;
                bArr[i2] = (byte) Integer.parseInt(strString.substring(i3, i3 + 2), 16);
            }
            String str = new String(bArr, StandardCharsets.UTF_8);
            if (str.indexOf(65533) >= 0 || str.indexOf(0) >= 0) {
                throw new IOException("DEX descriptor");
            }
            return str;
        }

        String type(int i) throws IOException {
            return text(u32(this.to + (index(i, this.tc) * 4)));
        }

        String proto(int i) throws IOException {
            int iIndex = this.po + (index(i, this.pc) * 12);
            int iU32 = u32(iIndex + 8);
            StringBuilder sb = new StringBuilder("(");
            if (iU32 != 0) {
                int iU33 = u32(iU32);
                int i2 = iU32 + 4;
                table(iU33, i2, 2);
                for (int i3 = 0; i3 < iU33; i3++) {
                    sb.append(type(u16((i3 * 2) + i2)));
                }
            }
            return sb.append(')').append(type(u32(iIndex + 4))).toString();
        }

        String field(int i) throws IOException {
            int iIndex = this.fo + (index(i, this.fc) * 8);
            return type(u16(iIndex)) + "->" + text(u32(iIndex + 4)) + ":" + type(u16(iIndex + 2));
        }

        String method(int i) throws IOException {
            int iIndex = this.mo + (index(i, this.mc) * 8);
            return type(u16(iIndex)) + "->" + text(u32(iIndex + 4)) + proto(u16(iIndex + 2));
        }

        Map<String, String> fingerprints() throws Exception {
            int iU32;
            HashSet hashSet = new HashSet(Arrays.asList(FrameworkCode.METHODS));
            HashSet hashSet2 = new HashSet();
            for (String str : FrameworkCode.METHODS) {
                hashSet2.add(str.substring(0, str.indexOf("->")));
            }
            HashMap map = new HashMap();
            for (int i = 0; i < this.cc; i++) {
                int i2 = this.co + (i * 32);
                if (hashSet2.contains(type(u32(i2))) && (iU32 = u32(i2 + 24)) != 0) {
                    int[] iArr = {iU32};
                    int iLeb = leb(iArr);
                    int iLeb2 = leb(iArr);
                    int iLeb3 = leb(iArr);
                    int iLeb4 = leb(iArr);
                    if (((long) iLeb) + ((long) iLeb2) > this.fc || ((long) iLeb3) + ((long) iLeb4) > this.mc) {
                        throw new IOException("DEX members");
                    }
                    int[] iArr2 = {iLeb, iLeb2};
                    for (int i3 = 0; i3 < 2; i3++) {
                        int i4 = iArr2[i3];
                        int iLeb5 = 0;
                        for (int i5 = 0; i5 < i4; i5++) {
                            iLeb5 += leb(iArr);
                            index(iLeb5, this.fc);
                            leb(iArr);
                        }
                    }
                    int[] iArr3 = {iLeb3, iLeb4};
                    for (int i6 = 0; i6 < 2; i6++) {
                        int i7 = iArr3[i6];
                        int iLeb6 = 0;
                        for (int i8 = 0; i8 < i7; i8++) {
                            iLeb6 += leb(iArr);
                            index(iLeb6, this.mc);
                            int iLeb7 = leb(iArr);
                            int iLeb8 = leb(iArr);
                            String strMethod = method(iLeb6);
                            if (hashSet.contains(strMethod)) {
                                if (map.containsKey(strMethod) || iLeb8 == 0) {
                                    throw new IOException("DEX duplicate/code");
                                }
                                map.put(strMethod, digest(iLeb8, iLeb7));
                            }
                        }
                    }
                }
            }
            return map;
        }

        String digest(int i, int i2) throws Exception {
            int i3;
            int i4;
            StringBuilder sb;
            StringBuilder sbAppend;
            String strMethod;
            char c = 16;
            range(i, 16);
            int iU16 = u16(i + 6);
            int iU32 = u32(i + 12);
            if (iU16 != 0 || iU32 < 1 || iU32 > 65536) {
                throw new IOException("Unsupported code item");
            }
            int i5 = i + 16;
            range(i5, iU32 * 2);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(i2);
            dataOutputStream.writeInt(u16(i));
            dataOutputStream.writeInt(u16(i + 2));
            dataOutputStream.writeInt(u16(i + 4));
            dataOutputStream.writeInt(iU16);
            dataOutputStream.writeInt(iU32);
            int i6 = 0;
            while (i6 < iU32) {
                int iU17 = u16((i6 * 2) + i5);
                int i7 = iU17 & 255;
                int iWidth = width(i7);
                if (i7 == 0 && iU17 != 0) {
                    throw new IOException("Payload not approved");
                }
                if (iWidth == 0 || (i3 = i6 + iWidth) > iU32) {
                    throw new IOException("Unsupported opcode");
                }
                dataOutputStream.writeInt(iU17);
                boolean z = i7 == 26 || i7 == 27;
                boolean z2 = i7 == 28 || i7 == 31 || i7 == 32 || i7 == 34 || i7 == 35 || i7 == 36 || i7 == 37;
                boolean z3 = i7 >= 82 && i7 <= 109;
                char c2 = c;
                boolean z4 = (i7 >= 110 && i7 <= 114) || (i7 >= 116 && i7 <= 120);
                if (z || z2 || z3 || z4) {
                    int iU18 = u16(((i6 + 1) * 2) + i5);
                    if (i7 == 27) {
                        iU18 |= u16(((i6 + 2) * 2) + i5) << 16;
                        i4 = 2;
                    } else {
                        i4 = 1;
                    }
                    if (z) {
                        sbAppend = new StringBuilder().append("S");
                        strMethod = string(iU18);
                    } else if (z2) {
                        sbAppend = new StringBuilder().append("T");
                        strMethod = type(iU18);
                    } else if (z3) {
                        sb = new StringBuilder();
                        sbAppend = sb.append("F");
                        strMethod = field(iU18);
                    } else {
                        sb = new StringBuilder();
                        sbAppend = sb.append("M");
                        strMethod = method(iU18);
                    }
                    byte[] bytes = sbAppend.append(strMethod).toString().getBytes(StandardCharsets.UTF_8);
                    dataOutputStream.writeInt(bytes.length);
                    dataOutputStream.write(bytes);
                } else {
                    i4 = 0;
                }
                for (int i8 = i4 + 1; i8 < iWidth; i8++) {
                    dataOutputStream.writeInt(u16(((i6 + i8) * 2) + i5));
                }
                i6 = i3;
                c = c2;
            }
            byte[] bArrDigest = MessageDigest.getInstance("SHA-256").digest(byteArrayOutputStream.toByteArray());
            return FrameworkCode.hex(bArrDigest, 0, bArrDigest.length);
        }

        static int width(int i) {
            if (i == 24) {
                return 5;
            }
            if (i == 3 || i == 6 || i == 9 || i == 20 || i == 23 || i == 27 || i == 36 || i == 37 || i == 38 || i == 42 || i == 43 || i == 44 || ((i >= 110 && i <= 114) || (i >= 116 && i <= 120))) {
                return 3;
            }
            if (i == 2 || i == 5 || i == 8 || i == 19 || i == 21 || i == 22 || i == 25 || i == 26 || i == 28 || i == 31 || i == 32 || i == 34 || i == 35 || i == 41 || ((i >= 45 && i <= 61) || ((i >= 68 && i <= 109) || ((i >= 144 && i <= 175) || (i >= 208 && i <= 226))))) {
                return 2;
            }
            if (i <= 18 || i == 29 || i == 30 || i == 33 || i == 39 || i == 40) {
                return 1;
            }
            if (i >= 123 && i <= 143) {
                return 1;
            }
            if (i >= 176 && i <= 207) {
                return 1;
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String hex(byte[] bArr, int i, int i2) {
        char[] cArr = new char[i2 * 2];
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = bArr[i + i3] & 255;
            int i5 = i3 * 2;
            cArr[i5] = "0123456789abcdef".charAt(i4 >>> 4);
            cArr[i5 + 1] = "0123456789abcdef".charAt(i4 & 15);
        }
        return new String(cArr);
    }

    private FrameworkCode() {
    }
}
