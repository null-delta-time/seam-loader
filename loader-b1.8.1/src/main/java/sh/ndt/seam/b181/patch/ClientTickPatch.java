package sh.ndt.seam.b181.patch;

import org.objectweb.asm.*;

// Injects SeamHooks.onClientTick() at the start of the Minecraft game-tick method.
// net/minecraft/client/Minecraft.k()V — called ~20 times/s inside the timer loop in run().
public final class ClientTickPatch {

    private static final String TARGET_CLASS  = "net/minecraft/client/Minecraft";
    private static final String TICK_METHOD   = "k";
    private static final String HOOKS         = "sh/ndt/seam/b181/hook/SeamHooks";

    public static byte[] apply(String className, byte[] bytes) {
        if (!TARGET_CLASS.equals(className)) return null;

        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new Visitor(cw), 0);
        return cw.toByteArray();
    }

    private static final class Visitor extends ClassVisitor {
        Visitor(ClassVisitor cv) { super(Opcodes.ASM4, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                String sig, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
            if (TICK_METHOD.equals(name) && "()V".equals(desc)) {
                return new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS,
                            "onClientTick", "()V");
                    }
                };
            }
            return mv;
        }
    }

    private ClientTickPatch() {}
}
