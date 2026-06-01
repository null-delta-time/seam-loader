package sh.ndt.seam.b181.patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.charset.StandardCharsets;

// Intercepts call-sites of Display.setTitle within any class, routing the
// argument through SeamHooks.processTitle so mods can append to the title.
// Uses the ASM4 4-argument visitMethodInsn API — asm-all-4.1.jar is the
// runtime ASM and its ClassReader only calls the 4-argument form.
@SuppressWarnings("deprecation")
public final class TitlePatch {

    private static final String DISPLAY      = "org/lwjgl/opengl/Display";
    private static final String SET_TITLE    = "setTitle";
    private static final String SET_TITLE_D  = "(Ljava/lang/String;)V";
    private static final String HOOKS        = "sh/ndt/seam/b181/hook/SeamHooks";
    private static final String PROCESS_D    = "(Ljava/lang/String;)Ljava/lang/String;";

    public static byte[] apply(String name, byte[] bytes) {
        // Skip Display itself — we only want to intercept callers, not Display's
        // own internal setTitle invocations (which would corrupt lastBaseTitle).
        if (DISPLAY.equals(name)) return null;
        // JVM-generated reflection accessors contain a real INVOKESTATIC setTitle
        // call site; patching them routes SeamHooks' reflected calls back through
        // processTitle and corrupts lastBaseTitle with already-processed titles.
        if (name != null && name.contains("GeneratedMethodAccessor")) return null;
        if (!containsUtf8(bytes, DISPLAY)) return null;

        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        boolean[] patched = { false };

        cr.accept(new ClassVisitor(Opcodes.ASM4, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String mName, String descriptor,
                                             String signature, String[] exceptions) {
                final MethodVisitor mv = super.visitMethod(access, mName, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String iName, String iDesc) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && DISPLAY.equals(owner)
                                && SET_TITLE.equals(iName)
                                && SET_TITLE_D.equals(iDesc)) {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "processTitle", PROCESS_D);
                            patched[0] = true;
                        }
                        mv.visitMethodInsn(opcode, owner, iName, iDesc);
                    }
                };
            }
        }, 0);

        return patched[0] ? cw.toByteArray() : null;
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

    private TitlePatch() {}
}
