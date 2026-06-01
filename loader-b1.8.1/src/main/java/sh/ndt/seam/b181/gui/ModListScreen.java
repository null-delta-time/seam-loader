package sh.ndt.seam.b181.gui;

import org.objectweb.asm.*;
import sh.ndt.seam.core.ModCandidate;
import sh.ndt.seam.core.SeamLoader;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

// Handles the Seam mod list screen.
//
// Because GuiScreen (qr) is loaded by LaunchClassLoader and our code lives in
// the system classloader, we cannot write a class that directly extends qr at
// compile time.  Instead, generateAndDefine() uses ASM to create the subclass
// bytecode at runtime and injects it into LCL via defineClass.  All actual
// screen logic is in static methods here, called from the generated class.
//
// B1.8.1 obfuscated name reference:
//   qr  = GuiScreen   vj  = GuiButton   kh  = FontRenderer
//   lf  = GuiDrawable (parent of qr/vj, has drawGradientRect etc.)
public final class ModListScreen {

    // ── obfuscated names ─────────────────────────────────────────────────────
    private static final String C_GUI_SCREEN    = "qr";
    private static final String C_GUI_BUTTON    = "vj";
    private static final String C_FONT_RENDERER = "kh";
    private static final String C_DELEGATE      = "sh/ndt/seam/b181/gui/ModListScreen";
    private static final String C_GENERATED     = "seam/gui/SeamModListScreen";

    // ── button IDs ────────────────────────────────────────────────────────────
    private static final int BTN_DONE        = 100;
    private static final int BTN_CONFIG      = 101;
    private static final int BTN_OPEN_FOLDER = 102;

    // ── layout ────────────────────────────────────────────────────────────────
    private static final int LIST_TOP    = 32;
    private static final int LIST_BOTTOM = 36;
    private static final int ENTRY_H     = 20;

    // ── mutable screen state (one screen at a time) ───────────────────────────
    private static int    scrollOffset = 0;
    private static int    selectedIdx  = -1;
    private static Object configButtonRef = null;
    private static Object parentScreenRef = null;

    // ── generated class cache ─────────────────────────────────────────────────
    private static volatile Class<?> generatedClass = null;
    private static final Object GEN_LOCK = new Object();

    // ─────────────────────────────────────────────────────────────────────────
    // Public entry point — called from SeamHooks when "Mods" button is clicked.
    // ─────────────────────────────────────────────────────────────────────────

    public static void open(Object mc, Object parent) {
        try {
            ClassLoader lcl = mc.getClass().getClassLoader();
            if (generatedClass == null) {
                synchronized (GEN_LOCK) {
                    if (generatedClass == null)
                        generatedClass = generateAndDefine(lcl);
                }
            }
            scrollOffset     = 0;
            selectedIdx      = -1;
            configButtonRef  = null;
            parentScreenRef  = parent;

            Object screen = generatedClass.getDeclaredConstructor().newInstance();
            Class<?> qrCls = Class.forName(C_GUI_SCREEN, false, lcl);
            mc.getClass().getMethod("a", qrCls).invoke(mc, screen);

        } catch (Throwable t) {
            System.err.println("[Seam] Failed to open mod list screen: " + t);
            t.printStackTrace(System.err);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegates — called from the generated SeamModListScreen class
    // ─────────────────────────────────────────────────────────────────────────

    public static void onInit(Object screen) {
        try {
            ClassLoader lcl = screen.getClass().getClassLoader();
            Class<?> qrCls  = Class.forName(C_GUI_SCREEN, false, lcl);
            Class<?> vjCls  = Class.forName(C_GUI_BUTTON, false, lcl);

            List<Object> buttons = getButtonList(qrCls, screen);
            int w = getInt(qrCls, "m", screen);
            int h = getInt(qrCls, "n", screen);

            Constructor<?> ctor3 = vjCls.getConstructor(int.class, int.class, int.class, String.class);
            Constructor<?> ctor5 = vjCls.getConstructor(int.class, int.class, int.class, int.class, int.class, String.class);

            int btnY = h - LIST_BOTTOM + 4;

            // Mirror the Options | Quit split: m/2-100 (98px) | m/2+2 (98px), 4px gap
            buttons.add(ctor5.newInstance(BTN_OPEN_FOLDER, w / 2 - 100, btnY, 98, 20, "Open Mods Folder"));
            buttons.add(ctor5.newInstance(BTN_DONE,        w / 2 + 2,   btnY, 98, 20, "Done"));

            // Open Config — top of detail panel, hidden until a mod is selected
            int rightX = w / 2 + 4;
            int rightW = w / 2 - 8;
            Object cfgBtn = ctor5.newInstance(BTN_CONFIG, rightX, LIST_TOP + 4, rightW, 20, "Open Config");
            setField(vjCls, "i", cfgBtn, false); // visible = false
            buttons.add(cfgBtn);
            configButtonRef = cfgBtn;

        } catch (Throwable t) {
            System.err.println("[Seam] ModListScreen.onInit failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    public static void onDraw(Object screen, int mx, int my, float pt) {
        try {
            ClassLoader lcl = screen.getClass().getClassLoader();
            Class<?> qrCls = Class.forName(C_GUI_SCREEN, false, lcl);

            int w  = getInt(qrCls, "m", screen);
            int h  = getInt(qrCls, "n", screen);
            Object fr = getField(qrCls, "q", screen);

            // Dirt background
            qrCls.getMethod("k").invoke(screen);

            int listBottom = h - LIST_BOTTOM;
            int halfW      = w / 2;

            // Dark overlay — list panel (left)
            drawRect(screen, 0, LIST_TOP, halfW - 2, listBottom, 0xB0000000);
            // Dark overlay — detail panel (right)
            drawRect(screen, halfW + 2, LIST_TOP, w, listBottom, 0xA0000000);
            // Separator
            drawRect(screen, halfW - 1, LIST_TOP, halfW + 1, listBottom, 0xFF555555);

            // Header / footer gradients
            drawGradientRect(screen, 0, 0, w, LIST_TOP, 0xFF000000, 0x00000000);
            drawGradientRect(screen, 0, listBottom, w, h, 0x00000000, 0xFF000000);

            // Title
            drawCenteredString(fr, "Mods", w / 2, 10, 0xFFFFFF);

            // Mod list entries
            drawModList(screen, fr, lcl, mx, my, w, h);

            // Detail panel
            drawDetail(screen, fr, lcl, w, h);

            // Scroll wheel
            handleScroll(h);

            // Update config button visibility
            updateConfigButton(lcl);

        } catch (Throwable t) {
            System.err.println("[Seam] ModListScreen.onDraw failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    public static void onAction(Object screen, Object button) {
        try {
            ClassLoader lcl = screen.getClass().getClassLoader();
            Class<?> qrCls  = Class.forName(C_GUI_SCREEN, false, lcl);
            Class<?> vjCls  = Class.forName(C_GUI_BUTTON, false, lcl);
            int id = getInt(vjCls, "g", button);

            if (id == BTN_DONE) {
                Object mc = getField(qrCls, "l", screen);
                mc.getClass().getMethod("a", qrCls).invoke(mc, parentScreenRef);

            } else if (id == BTN_OPEN_FOLDER) {
                openModsFolder();

            } else if (id == BTN_CONFIG) {
                // TODO: open per-mod config screen (API not defined yet)
                System.out.println("[Seam] Config screen not yet implemented.");
            }

        } catch (Throwable t) {
            System.err.println("[Seam] ModListScreen.onAction failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    public static void onKeyTyped(Object screen, int charCode, int keyCode) {
        // ESC is handled by the generated class calling super.a(CI)V,
        // which navigates back via mc.a(null).
    }

    public static void onMouseClicked(Object screen, int mx, int my, int btn) {
        if (btn != 0) return;
        try {
            ClassLoader lcl = screen.getClass().getClassLoader();
            Class<?> qrCls  = Class.forName(C_GUI_SCREEN, false, lcl);
            int w = getInt(qrCls, "m", screen);
            int h = getInt(qrCls, "n", screen);

            int listX1 = 4;
            int listX2 = w / 2 - 4;
            int listY1 = LIST_TOP + 2;
            int listY2 = h - LIST_BOTTOM;

            if (mx >= listX1 && mx < listX2 && my >= listY1 && my < listY2) {
                int rel = my - listY1 + scrollOffset;
                int idx = rel / ENTRY_H;
                SeamLoader loader = SeamLoader.getInstance();
                if (loader != null && idx >= 0 && idx < loader.getMods().size()) {
                    selectedIdx = idx;
                }
            }
        } catch (Throwable t) {
            System.err.println("[Seam] ModListScreen.onMouseClicked failed: " + t);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void drawModList(Object screen, Object fr, ClassLoader lcl,
                                    int mx, int my, int w, int h) throws Exception {
        SeamLoader loader = SeamLoader.getInstance();
        if (loader == null) return;
        List<ModCandidate> mods = loader.getMods();
        if (mods.isEmpty()) {
            drawString(fr, "No mods loaded.", 8, LIST_TOP + 6, 0xAAAAAA);
            return;
        }

        int listX1 = 4;
        int listX2 = w / 2 - 4;
        int listY1 = LIST_TOP + 2;
        int listY2 = h - LIST_BOTTOM;
        int visW   = listX2 - listX1;

        // Scissor the list area — we just skip drawing entries outside the band
        for (int i = 0; i < mods.size(); i++) {
            int entryY = listY1 + i * ENTRY_H - scrollOffset;
            if (entryY + ENTRY_H <= listY1 || entryY >= listY2) continue;

            ModCandidate mod = mods.get(i);
            boolean hovered = mx >= listX1 && mx < listX2
                && my >= entryY && my < entryY + ENTRY_H;
            boolean selected = (i == selectedIdx);

            int bg = selected ? 0xFF3060A0
                    : hovered ? 0xFF505050
                    : 0x00000000;
            if (bg != 0) drawRect(screen, listX1, entryY, listX2, entryY + ENTRY_H, bg);

            String label = mod.name() + " " + mod.version();
            int textColor = selected ? 0xFFFFFF : 0xDDDDDD;
            drawString(fr, label, listX1 + 3, entryY + 5, textColor);
        }
    }

    private static void drawDetail(Object screen, Object fr, ClassLoader lcl,
                                   int w, int h) throws Exception {
        SeamLoader loader = SeamLoader.getInstance();
        if (loader == null) return;
        List<ModCandidate> mods = loader.getMods();

        int rightX  = w / 2 + 4;
        int detailY = LIST_TOP + 30; // below config button

        if (selectedIdx < 0 || selectedIdx >= mods.size()) {
            drawString(fr, "Select a mod to view details.", rightX, detailY, 0x888888);
            return;
        }

        ModCandidate mod = mods.get(selectedIdx);
        int nameColor = 0xFFFFAA;
        drawString(fr, mod.name() + " v" + mod.version(), rightX, detailY, nameColor);

        String desc = mod.description();
        if (!desc.isEmpty()) {
            int wrapW = w / 2 - 12;
            drawSplitString(fr, desc, rightX, detailY + 14, wrapW, 0xCCCCCC);
        }
    }

    private static void handleScroll(int h) {
        try {
            Class<?> mouseCls = Class.forName("org.lwjgl.input.Mouse");
            int dw = (int) mouseCls.getMethod("getDWheel").invoke(null);
            if (dw != 0) {
                scrollOffset -= Integer.signum(dw) * ENTRY_H;
                SeamLoader sl = SeamLoader.getInstance();
                int maxScroll = sl == null ? 0
                    : Math.max(0, sl.getMods().size() * ENTRY_H - (h - LIST_TOP - LIST_BOTTOM));
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            }
        } catch (Throwable ignored) {}
    }

    private static void updateConfigButton(ClassLoader lcl) {
        if (configButtonRef == null) return;
        try {
            Class<?> vjCls = Class.forName(C_GUI_BUTTON, false, lcl);
            SeamLoader sl = SeamLoader.getInstance();
            boolean hasSelection = sl != null && selectedIdx >= 0
                && selectedIdx < sl.getMods().size();
            setField(vjCls, "i", configButtonRef, hasSelection);
        } catch (Throwable ignored) {}
    }

    private static void openModsFolder() {
        try {
            String gameDir = System.getProperty("seam.gameDir", ".");
            File modsDir = new java.io.File(gameDir, "seam/mods");
            if (!modsDir.exists()) modsDir.mkdirs();
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().open(modsDir);
        } catch (Throwable t) {
            System.err.println("[Seam] Failed to open mods folder: " + t);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Low-level draw wrappers (delegate to lf inherited methods via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    // lf.a(IIIIII)V — drawGradientRect, defined on lf which qr extends
    private static void drawGradientRect(Object screen, int x1, int y1, int x2, int y2,
                                         int c1, int c2) throws Exception {
        // Walk up to find the class that declares this 6-int method (lf)
        Method m = findMethod(screen.getClass(),
            "a", int.class, int.class, int.class, int.class, int.class, int.class);
        m.setAccessible(true);
        m.invoke(screen, x1, y1, x2, y2, c1, c2);
    }

    // lf.a(int x1,y1,x2,y2) — drawRect (solid with current GL color)
    // In practice we want a color, so we use drawGradientRect with same color.
    private static void drawRect(Object screen, int x1, int y1, int x2, int y2, int color)
            throws Exception {
        drawGradientRect(screen, x1, y1, x2, y2, color, color);
    }

    // kh.a(String, x, y, color)V — drawString
    private static void drawString(Object fr, String text, int x, int y, int color)
            throws Exception {
        findMethod(fr.getClass(), "a", String.class, int.class, int.class, int.class)
            .invoke(fr, text, x, y, color);
    }

    // Compute string width then draw centred
    private static void drawCenteredString(Object fr, String text, int cx, int y, int color)
            throws Exception {
        int tw = (int) findMethod(fr.getClass(), "a", String.class).invoke(fr, text);
        drawString(fr, text, cx - tw / 2, y, color);
    }

    // kh.a(String, x, y, wrapW, color)V — drawSplitString (word-wrap)
    private static void drawSplitString(Object fr, String text, int x, int y, int wrapW, int color)
            throws Exception {
        findMethod(fr.getClass(), "a",
            String.class, int.class, int.class, int.class, int.class)
            .invoke(fr, text, x, y, wrapW, color);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reflection utilities
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // ASM — generate SeamModListScreen extends qr at runtime
    // ─────────────────────────────────────────────────────────────────────────

    private static Class<?> generateAndDefine(ClassLoader lcl) throws Exception {
        byte[] bytes = generateBytes();

        // defineClass is protected, reach it via reflection
        Method defineClass = ClassLoader.class.getDeclaredMethod(
            "defineClass", String.class, byte[].class, int.class, int.class);
        defineClass.setAccessible(true);

        return (Class<?>) defineClass.invoke(
            lcl,
            C_GENERATED.replace('/', '.'),
            bytes, 0, bytes.length);
    }

    private static byte[] generateBytes() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        // class seam/gui/SeamModListScreen extends qr
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
            C_GENERATED, null, C_GUI_SCREEN, null);

        // default no-arg constructor — just calls super()
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, C_GUI_SCREEN, "<init>", "()V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // initGui: a()V
        // super.a(); ModListScreen.onInit(this);
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

        // drawScreen: a(IIF)V
        // ModListScreen.onDraw(this, mx, my, pt); super.a(mx, my, pt);
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

        // actionPerformed: a(Lvj;)V
        // ModListScreen.onAction(this, button);
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

        // keyTyped: a(CI)V
        // ModListScreen.onKeyTyped(this, c, key); super.a(c, key);
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "a", "(CI)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, C_DELEGATE,
                "onKeyTyped", "(Ljava/lang/Object;II)V");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, C_GUI_SCREEN, "a", "(CI)V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // mouseClicked: a(III)V
        // ModListScreen.onMouseClicked(this, mx, my, btn); super.a(mx, my, btn);
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "a", "(III)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, C_DELEGATE,
                "onMouseClicked", "(Ljava/lang/Object;III)V");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, C_GUI_SCREEN, "a", "(III)V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    private ModListScreen() {}
}
