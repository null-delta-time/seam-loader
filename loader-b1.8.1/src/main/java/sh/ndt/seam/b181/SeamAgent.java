package sh.ndt.seam.b181;

import sh.ndt.seam.b181.SeamMeta;
import sh.ndt.seam.core.SeamLoader;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SeamAgent {

    public static void premain(String args, Instrumentation inst) throws Exception {
        // -Dseam.debugStderr=true redirects stderr → stdout with a [stderr] tag so all
        // output lands in one stream. Off by default; the launcher shows both streams anyway.
        if ("true".equals(System.getProperty("seam.debugStderr"))) {
            System.setErr(new PrintStream(new OutputStream() {
                private final ByteArrayOutputStream line = new ByteArrayOutputStream();
                @Override public void write(int b) {
                    if (b == '\n') {
                        System.out.println("[stderr] " + line);
                        line.reset();
                    } else {
                        line.write(b);
                    }
                }
                @Override public void write(byte[] b, int off, int len) {
                    for (int i = off; i < off + len; i++) write(b[i]);
                }
            }, true));
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println("[Seam] CRASH on thread " + thread.getName() + ": " + throwable);
            for (StackTraceElement e : throwable.getStackTrace()) {
                System.out.println("[Seam]   at " + e);
            }
            Throwable cause = throwable.getCause();
            if (cause != null) {
                System.out.println("[Seam] Caused by: " + cause);
                for (StackTraceElement e : cause.getStackTrace()) {
                    System.out.println("[Seam]   at " + e);
                }
            }
        });

        // No appendToBootstrapClassLoaderSearch: transformer classes stay in the system
        // classloader (which has ASM on java.class.path). LaunchClassLoader gets
        // seam-agent.jar via the system CL's URL list, so SeamHooks is reachable from
        // patched bytecode. Mod count shared via system property to avoid the two-CL
        // class identity problem.
        inst.addTransformer(new SeamTransformer());

        // Discover mods early so the count is set before the main menu renders.
        Path gameDir = resolveGameDir();
        SeamLoader loader = new SeamLoader();
        loader.discover(gameDir.resolve("seam/mods"));
        System.setProperty("seam.modCount", String.valueOf(loader.getModCount()));

        System.out.println("[Seam] " + SeamMeta.VERSION + " — " +
            loader.getModCount() + " mod(s) found");
        System.out.println("[Seam] java.class.path=" + System.getProperty("java.class.path"));
    }

    private static Path resolveGameDir() {
        // The launcher passes --gameDir <path> as program arguments; we can't
        // read those in premain. Fall back to the working directory, which the
        // launcher sets to the game directory before launch.
        String override = System.getProperty("seam.gameDir");
        return override != null ? Paths.get(override) : Paths.get(".");
    }

    private SeamAgent() {}
}
