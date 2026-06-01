package sh.ndt.seam.b181.patch;

import org.objectweb.asm.*;

import java.nio.charset.StandardCharsets;

// Patches the main menu class (sf, identified by mclogo + menu.mods markers) to:
// 1. Call SeamHooks.onMainMenuInit(this) at the end of initGui (a()V) so we can
//    split "Mods and Texture Packs" into two 98-wide buttons.
// 2. Call SeamHooks.onMainMenuAction(this, button) at the start of actionPerformed
//    (a(Lvj;)V) so we can intercept the new Mods button (id=5).
@SuppressWarnings("deprecation")
public final class MainMenuPatch {

    private static final String MENU_MARKER = "/title/mclogo.png";
    private static final String MODS_MARKER = "menu.mods";
    private static final String HOOKS       = "sh/ndt/seam/b181/hook/SeamHooks";

    public static byte[] apply(byte[] bytes) {
        if (!containsUtf8(bytes, MENU_MARKER)) return null;
        if (!containsUtf8(bytes, MODS_MARKER)) return null;

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
            MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);

            // initGui: a()V — inject hook before each RETURN
            if ("a".equals(name) && "()V".equals(desc)) {
                return new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS,
                                "onMainMenuInit", "(Ljava/lang/Object;)V");
                        }
                        mv.visitInsn(opcode);
                    }
                };
            }

            // actionPerformed: a(Lvj;)V — inject hook at method start
            if ("a".equals(name) && "(Lvj;)V".equals(desc)) {
                return new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    public void visitCode() {
                        mv.visitCode();
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitVarInsn(Opcodes.ALOAD, 1);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS,
                            "onMainMenuAction", "(Ljava/lang/Object;Ljava/lang/Object;)V");
                    }
                };
            }

            return mv;
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

    private MainMenuPatch() {}
}
