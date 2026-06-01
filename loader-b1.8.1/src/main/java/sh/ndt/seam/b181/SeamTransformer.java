package sh.ndt.seam.b181;

import sh.ndt.seam.api.ClassTransformer;
import sh.ndt.seam.api.SeamApi;
import sh.ndt.seam.b181.patch.ClientTickPatch;
import sh.ndt.seam.b181.patch.MainMenuPatch;
import sh.ndt.seam.b181.patch.TitlePatch;
import sh.ndt.seam.b181.patch.VersionStringPatch;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

public final class SeamTransformer implements ClassFileTransformer {

    private volatile boolean lclExclusionAdded = false;

    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> classBeingRedefined, ProtectionDomain domain,
            byte[] classfileBuffer) {
        if (loader == null) return null;
        tryAddLclExclusion(loader);

        byte[] bytes = classfileBuffer;

        try { bytes = applyBuiltin("VersionStringPatch", className, bytes, VersionStringPatch.apply(bytes)); }
        catch (Throwable t) { System.err.println("[Seam] VersionStringPatch failed on " + className + ": " + t); }

        try { bytes = applyBuiltin("ClientTickPatch", className, bytes, ClientTickPatch.apply(className, bytes)); }
        catch (Throwable t) { System.err.println("[Seam] ClientTickPatch failed on " + className + ": " + t); }

        try { bytes = applyBuiltin("TitlePatch", className, bytes, TitlePatch.apply(className, bytes)); }
        catch (Throwable t) { System.err.println("[Seam] TitlePatch failed on " + className + ": " + t); }

        try { bytes = applyBuiltin("MainMenuPatch", className, bytes, MainMenuPatch.apply(bytes)); }
        catch (Throwable t) { System.err.println("[Seam] MainMenuPatch failed on " + className + ": " + t); }

        for (ClassTransformer t : SeamApi.getTransformers()) {
            try {
                byte[] patched = t.transform(className, bytes);
                if (patched != null) bytes = patched;
            } catch (Throwable t2) {
                System.err.println("[Seam] Mod transformer failed on " + className + ": " + t2);
            }
        }

        return bytes == classfileBuffer ? null : bytes;
    }

    // LaunchClassLoader loads game classes child-first and finds seam-agent.jar on
    // the system classpath, so it would load its own copy of SeamApi — splitting
    // the static lists from the system-CL copy used by mods' URLClassLoaders.
    // Adding "sh.ndt.seam." to LCL's exclusion list forces delegation to system CL.
    private void tryAddLclExclusion(ClassLoader loader) {
        if (lclExclusionAdded) return;
        try {
            loader.getClass().getMethod("addClassLoaderExclusion", String.class)
                .invoke(loader, "sh.ndt.seam.");
            System.out.println("[Seam] Added sh.ndt.seam. exclusion to "
                + loader.getClass().getName());
            lclExclusionAdded = true;
        } catch (NoSuchMethodException ignored) {
            // Not a LaunchClassLoader — keep trying on subsequent calls
        } catch (Throwable t) {
            System.out.println("[Seam] LCL exclusion failed: " + t);
            lclExclusionAdded = true;
        }
    }

    private static byte[] applyBuiltin(String name, String className, byte[] current, byte[] patched) {
        if (patched == null) return current;
        System.out.println("[Seam] Patched " + className + " (" + name + ")");
        return patched;
    }
}
