package sh.ndt.seam.b181.patch;

import org.objectweb.asm.*;

import java.nio.charset.StandardCharsets;

// Patches the main menu class (identified by the mclogo constant) to replace
// LDC "Minecraft Beta 1.8.1" with a call to SeamHooks.patchVersionString.
public final class VersionStringPatch {

    private static final String TARGET = "Minecraft Beta 1.8.1";
    // Unique constant present only in the main menu class (GuiMainMenu equivalent)
    private static final String MENU_MARKER = "/title/mclogo.png";
    private static final String HOOKS  = "sh/ndt/seam/b181/hook/SeamHooks";

    public static byte[] apply(byte[] bytes) {
        if (!containsUtf8(bytes, TARGET)) return null;
        if (!containsUtf8(bytes, MENU_MARKER)) return null;

        ClassReader  cr = new ClassReader(bytes);
        ClassWriter  cw = new ClassWriter(cr, 0);
        cr.accept(new Visitor(cw), 0);
        return cw.toByteArray();
    }

    private static final class Visitor extends ClassVisitor {
        Visitor(ClassVisitor cv) { super(Opcodes.ASM4, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                String sig, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM4,
                    super.visitMethod(access, name, desc, sig, exceptions)) {
                @Override
                public void visitLdcInsn(Object value) {
                    super.visitLdcInsn(value);
                    if (TARGET.equals(value)) {
                        // 4-arg form: compatible with asm-all-4.1 which is provided at runtime
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS,
                            "patchVersionString", "(Ljava/lang/String;)Ljava/lang/String;");
                    }
                }
            };
        }
    }

    private static boolean containsUtf8(byte[] bytes, String s) {
        byte[] needle = s.getBytes(StandardCharsets.UTF_8);
        outer:
        for (int i = 0, lim = bytes.length - needle.length; i <= lim; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (bytes[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private VersionStringPatch() {}
}
