package sh.ndt.seam.b181.hook;

import sh.ndt.seam.api.SeamApi;
import sh.ndt.seam.b181.SeamMeta;

import java.lang.reflect.Method;

// Called from ASM-patched game bytecode, loaded by LaunchClassLoader.
// Main-menu hooks use reflection so we never reference obfuscated game types directly.
public final class SeamHooks {

    private static final int MODS_BTN_ID = 5;

    private static Method displaySetTitle;
    // Captured from the AWT frame on the first tick, before we start modifying
    // the title. This is the title the OS showed originally ("Minecraft").
    private static String baseTitle = null;

    public static String patchVersionString(String ignored) {
        String count = System.getProperty("seam.modCount", "0");
        return "Minecraft " + SeamMeta.MC_VERSION + ", Seam " + SeamMeta.VERSION
            + " " + count + " mod(s)";
    }

    public static void onClientTick() {
        SeamApi.fireClientTick();
        if (!SeamApi.hasTitleAppenders()) return;

        if (baseTitle == null) {
            baseTitle = readAwtFrameTitle();
        }

        String title = SeamApi.processTitle(baseTitle);

        // LWJGL title (Linux/Windows)
        try {
            if (displaySetTitle == null) {
                displaySetTitle = Class.forName("org.lwjgl.opengl.Display")
                    .getMethod("setTitle", String.class);
            }
            displaySetTitle.invoke(null, title);
        } catch (Throwable t) {
            displaySetTitle = null;
        }

        // AWT Frame title (macOS — LWJGL embeds in an AWT Frame whose title must be
        // updated on the AWT Event Dispatch Thread)
        try {
            final String t = title;
            java.awt.EventQueue.invokeLater(() -> {
                for (java.awt.Frame f : java.awt.Frame.getFrames()) {
                    if (!f.isDisplayable()) continue;
                    f.setTitle(t);
                    break;
                }
            });
        } catch (Throwable ignored) {}
    }

    private static String readAwtFrameTitle() {
        try {
            for (java.awt.Frame f : java.awt.Frame.getFrames()) {
                if (f.isDisplayable()) return f.getTitle();
            }
        } catch (Throwable ignored) {}
        return "Minecraft";
    }

    public static String processTitle(String title) {
        return SeamApi.processTitle(title);
    }

    // ── main-menu hooks ───────────────────────────────────────────────────────

    // Called at the end of GuiMainMenu.initGui().
    // Splits button 3 ("Mods and Texture Packs", 200px) into two 98px buttons:
    //   id=3 "Texture Packs"  and  id=5 "Mods".
    public static void onMainMenuInit(Object screen) {
        try {
            Class<?> qrCls = screen.getClass().getSuperclass(); // qr = GuiScreen
            java.lang.reflect.Field oFld = qrCls.getDeclaredField("o");
            oFld.setAccessible(true);
            java.util.List<?> buttons = (java.util.List<?>) oFld.get(screen);

            // Find button id=3 (menu.mods = "Mods and Texture Packs")
            Object texBtn = null;
            Class<?> vjCls = null;
            for (Object btn : buttons) {
                java.lang.reflect.Field gFld = btn.getClass().getDeclaredField("g");
                gFld.setAccessible(true);
                if (gFld.getInt(btn) == 3) {
                    texBtn = btn;
                    vjCls = btn.getClass();
                    break;
                }
            }
            if (vjCls == null) return;

            java.lang.reflect.Field widthFld = vjCls.getDeclaredField("a"); // protected
            java.lang.reflect.Field labelFld = vjCls.getDeclaredField("f");
            widthFld.setAccessible(true);
            labelFld.setAccessible(true);

            java.lang.reflect.Field cFld = vjCls.getDeclaredField("c");
            java.lang.reflect.Field dFld = vjCls.getDeclaredField("d");
            cFld.setAccessible(true);
            dFld.setAccessible(true);
            int btnX = cFld.getInt(texBtn);
            int btnY = dFld.getInt(texBtn);

            // Resize existing button → "Texture Packs" (98px wide, left slot)
            widthFld.set(texBtn, 98);
            labelFld.set(texBtn, "Texture Packs");

            // Add "Mods" button (98px wide, right slot, gap of 4px)
            java.lang.reflect.Constructor<?> ctor5 = vjCls.getConstructor(
                int.class, int.class, int.class, int.class, int.class, String.class);
            Object modsBtn = ctor5.newInstance(MODS_BTN_ID, btnX + 102, btnY, 98, 20, "Mods");
            //noinspection unchecked
            ((java.util.List<Object>) buttons).add(modsBtn);

        } catch (Throwable t) {
            System.err.println("[Seam] onMainMenuInit failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    // Called at the start of GuiMainMenu.actionPerformed().
    // Intercepts our Mods button (id=5) before the original switch-on-id logic.
    public static void onMainMenuAction(Object screen, Object button) {
        try {
            java.lang.reflect.Field gFld = button.getClass().getDeclaredField("g");
            gFld.setAccessible(true);
            int id = gFld.getInt(button);
            if (id == MODS_BTN_ID) {
                Class<?> qrCls = screen.getClass().getSuperclass();
                java.lang.reflect.Field lFld = qrCls.getDeclaredField("l");
                lFld.setAccessible(true);
                Object mc = lFld.get(screen);
                sh.ndt.seam.b181.gui.ModListScreen.open(mc, screen);
            }
        } catch (Throwable t) {
            System.err.println("[Seam] onMainMenuAction failed: " + t);
            t.printStackTrace(System.err);
        }
    }

    private SeamHooks() {}
}
