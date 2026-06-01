package sh.ndt.seam.b181.hook;

import sh.ndt.seam.api.SeamApi;
import sh.ndt.seam.b181.SeamMeta;

import java.lang.reflect.Method;

// Called from ASM-patched game bytecode, loaded by LaunchClassLoader.
public final class SeamHooks {

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

    private SeamHooks() {}
}
