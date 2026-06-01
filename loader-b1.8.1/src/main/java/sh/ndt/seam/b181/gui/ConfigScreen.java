package sh.ndt.seam.b181.gui;

import org.objectweb.asm.*;
import sh.ndt.seam.api.ConfigEntry;
import sh.ndt.seam.api.SeamApi;
import sh.ndt.seam.b181.config.ConfigManager;
import sh.ndt.seam.core.ModCandidate;
import sh.ndt.seam.core.SeamLoader;

import java.lang.reflect.*;
import java.util.*;

// Per-mod config screen: ASM-generated SeamConfigScreen extends qr, delegates to static methods here.
public final class ConfigScreen {

    private static final String C_GUI_SCREEN = "qr";
    private static final String C_GUI_BUTTON = "vj";
    private static final String C_DELEGATE   = "sh/ndt/seam/b181/gui/ConfigScreen";
    private static final String C_GENERATED  = "seam/gui/SeamConfigScreen";

    private static final int BTN_SAVE   = 110;
    private static final int BTN_CANCEL = 111;
    // toggle buttons: 200 + entry index

    // ── screen state ──────────────────────────────────────────────────────────
    private static String pendingModId      = null;
    private static String pendingModName    = null;
    private static Map<String, Object> pendingValues = null;
    private static List<Object> toggleRefs  = new ArrayList<>();
    private static Object parentScreen      = null;
    private static boolean dirty            = false;
    private static Object saveButtonRef     = null;

    // ── generated class cache ─────────────────────────────────────────────────
    private static volatile Class<?> generatedClass = null;
    private static final Object GEN_LOCK = new Object();

    public static void open(Object mc, Object modListScreen, String modId) {
        try {
            ClassLoader lcl = mc.getClass().getClassLoader();
            if (generatedClass == null) {
                synchronized (GEN_LOCK) {
                    if (generatedClass == null)
                        generatedClass = generateAndDefine(lcl);
                }
            }

            pendingModId   = modId;
            pendingModName = modId;
            SeamLoader sl  = SeamLoader.getInstance();
            if (sl != null) {
                for (ModCandidate mod : sl.getMods()) {
                    if (mod.id().equals(modId)) { pendingModName = mod.name(); break; }
                }
            }

            pendingValues = new LinkedHashMap<>(SeamApi.getAllConfigValues(modId));
            for (ConfigEntry e : SeamApi.getConfigEntries(modId))
                pendingValues.putIfAbsent(e.key(), e.defaultValue());

            toggleRefs    = new ArrayList<>();
            parentScreen  = modListScreen;
            dirty         = false;
            saveButtonRef = null;

            Object screen = generatedClass.getDeclaredConstructor().newInstance();
            Class<?> qrCls = Class.forName(C_GUI_SCREEN, false, lcl);
            mc.getClass().getMethod("a", qrCls).invoke(mc, screen);

        } catch (Throwable t) {
            System.err.println("[Seam] Failed to open config screen: " + t);
            t.printStackTrace(System.err);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegates
    // ─────────────────────────────────────────────────────────────────────────

    public static void onInit(Object screen) {
        try {
            ClassLoader lcl = screen.getClass().getClassLoader();
            Class<?> qrCls  = Class.forName(C_GUI_SCREEN, false, lcl);
            Class<?> vjCls  = Class.forName(C_GUI_BUTTON, false, lcl);

            List<Object> buttons = getButtonList(qrCls, screen);
            int w = getInt(qrCls, "m", screen);
            int h = getInt(qrCls, "n", screen);

            Constructor<?> ctor5 = vjCls.getConstructor(
                int.class, int.class, int.class, int.class, int.class, String.class);

            List<ConfigEntry> entries = SeamApi.getConfigEntries(pendingModId);
            toggleRefs = new ArrayList<>();

            int entryX = w / 2 - 100;
            for (int i = 0; i < entries.size(); i++) {
                ConfigEntry e = entries.get(i);
                boolean val   = Boolean.TRUE.equals(pendingValues.get(e.key()));
                String label  = e.displayName() + ": " + (val ? "ON" : "OFF");
                Object btn    = ctor5.newInstance(200 + i, entryX, 40 + i * 24, 200, 20, label);
                buttons.add(btn);
                toggleRefs.add(btn);
            }

            int btnY = h - 28;
            Object saveBtn = ctor5.newInstance(BTN_SAVE, w / 2 - 100, btnY, 98, 20, "Save");
            setField(vjCls, "h", saveBtn, false); // disabled until dirty
            saveButtonRef = saveBtn;
            buttons.add(saveBtn);
            buttons.add(ctor5.newInstance(BTN_CANCEL, w / 2 + 2, btnY, 98, 20, "Cancel"));

        } catch (Throwable t) {
            System.err.println("[Seam] ConfigScreen.onInit failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    public static void onDraw(Object screen, int mx, int my, float pt) {
        try {
            ClassLoader lcl = screen.getClass().getClassLoader();
            Class<?> qrCls  = Class.forName(C_GUI_SCREEN, false, lcl);

            int w  = getInt(qrCls, "m", screen);
            int h  = getInt(qrCls, "n", screen);
            Object fr = getField(qrCls, "q", screen);

            qrCls.getMethod("k").invoke(screen);

            drawGradientRect(screen, 0, 0, w, 32, 0xFF000000, 0x00000000);
            drawGradientRect(screen, 0, h - 36, w, h, 0x00000000, 0xFF000000);

            drawCenteredString(fr, "Config — " + pendingModName, w / 2, 10, 0xFFFFFF);

            if (!dirty) {
                drawCenteredString(fr, "Change a setting to enable Save", w / 2, h - 40, 0x666666);
            }

        } catch (Throwable t) {
            System.err.println("[Seam] ConfigScreen.onDraw failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    public static void onAction(Object screen, Object button) {
        try {
            ClassLoader lcl = screen.getClass().getClassLoader();
            Class<?> qrCls  = Class.forName(C_GUI_SCREEN, false, lcl);
            Class<?> vjCls  = Class.forName(C_GUI_BUTTON, false, lcl);
            int id = getInt(vjCls, "g", button);

            List<ConfigEntry> entries = SeamApi.getConfigEntries(pendingModId);
            if (id >= 200 && id < 200 + entries.size()) {
                ConfigEntry entry = entries.get(id - 200);
                if (entry.type() == ConfigEntry.Type.BOOLEAN) {
                    boolean next = !Boolean.TRUE.equals(pendingValues.get(entry.key()));
                    pendingValues.put(entry.key(), next);
                    setField(vjCls, "f", button, entry.displayName() + ": " + (next ? "ON" : "OFF"));
                    if (!dirty && saveButtonRef != null) {
                        dirty = true;
                        setField(vjCls, "h", saveButtonRef, true);
                    }
                }
            } else if (id == BTN_SAVE) {
                for (ConfigEntry e : entries)
                    SeamApi.setConfigValue(pendingModId, e.key(), pendingValues.get(e.key()));
                ConfigManager.save(pendingModId);
                navigateBack(screen, qrCls);
            } else if (id == BTN_CANCEL) {
                navigateBack(screen, qrCls);
            }

        } catch (Throwable t) {
            System.err.println("[Seam] ConfigScreen.onAction failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    private static void navigateBack(Object screen, Class<?> qrCls) throws Exception {
        Object mc = getField(qrCls, "l", screen);
        mc.getClass().getMethod("a", qrCls).invoke(mc, parentScreen);
    }

    // ── draw helpers ──────────────────────────────────────────────────────────

    private static void drawGradientRect(Object screen, int x1, int y1, int x2, int y2,
                                          int c1, int c2) throws Exception {
        Method m = findMethod(screen.getClass(), "a",
            int.class, int.class, int.class, int.class, int.class, int.class);
        m.setAccessible(true);
        m.invoke(screen, x1, y1, x2, y2, c1, c2);
    }

    private static void drawCenteredString(Object fr, String text, int cx, int y, int color)
            throws Exception {
        int tw = (int) findMethod(fr.getClass(), "a", String.class).invoke(fr, text);
        findMethod(fr.getClass(), "a", String.class, int.class, int.class, int.class)
            .invoke(fr, text, cx - tw / 2, y, color);
    }

    // ── reflection helpers ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<Object> getButtonList(Class<?> qrCls, Object screen) throws Exception {
        Field f = findField(qrCls, "o");
        f.setAccessible(true);
        return (List<Object>) f.get(screen);
    }

    private static int getInt(Class<?> cls, String field, Object obj) throws Exception {
        Field f = findField(cls, field);
        f.setAccessible(true);
        return f.getInt(obj);
    }

    private static Object getField(Class<?> cls, String field, Object obj) throws Exception {
        Field f = findField(cls, field);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static void setField(Class<?> cls, String field, Object obj, Object value)
            throws Exception {
        Field f = findField(cls, field);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params)
            throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredMethod(name, params); } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(name + " not found on " + cls);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name + " not found on " + cls);
    }

    // ── ASM bytecode generation ───────────────────────────────────────────────

    private static Class<?> generateAndDefine(ClassLoader lcl) throws Exception {
        byte[] bytes = generateBytes();
        Method defineClass = ClassLoader.class.getDeclaredMethod(
            "defineClass", String.class, byte[].class, int.class, int.class);
        defineClass.setAccessible(true);
        return (Class<?>) defineClass.invoke(
            lcl, C_GENERATED.replace('/', '.'), bytes, 0, bytes.length);
    }

    private static byte[] generateBytes() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
            C_GENERATED, null, C_GUI_SCREEN, null);

        // <init>()V
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, C_GUI_SCREEN, "<init>", "()V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // initGui: a()V → super.a(); ConfigScreen.onInit(this)
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "a", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, C_GUI_SCREEN, "a", "()V");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, C_DELEGATE, "onInit", "(Ljava/lang/Object;)V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // drawScreen: a(IIF)V → ConfigScreen.onDraw(this, mx, my, pt); super.a(mx, my, pt)
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "a", "(IIF)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.FLOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, C_DELEGATE,
                "onDraw", "(Ljava/lang/Object;IIF)V");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.FLOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, C_GUI_SCREEN, "a", "(IIF)V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // actionPerformed: a(Lvj;)V → ConfigScreen.onAction(this, button)
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "a",
                "(L" + C_GUI_BUTTON + ";)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, C_DELEGATE,
                "onAction", "(Ljava/lang/Object;Ljava/lang/Object;)V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // keyTyped: a(CI)V → super.a(c, key)
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "a", "(CI)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, C_GUI_SCREEN, "a", "(CI)V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    private ConfigScreen() {}
}
