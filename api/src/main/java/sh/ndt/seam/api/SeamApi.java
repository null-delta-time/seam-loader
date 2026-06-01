package sh.ndt.seam.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class SeamApi {

    private static final List<ClassTransformer> transformers   = new CopyOnWriteArrayList<>();
    private static final List<Runnable>         tickListeners  = new CopyOnWriteArrayList<>();
    private static final List<Supplier<String>> titleAppenders = new CopyOnWriteArrayList<>();

    public static void registerTransformer(ClassTransformer t) {
        transformers.add(t);
    }

    public static List<ClassTransformer> getTransformers() {
        return transformers;
    }

    public static void onClientTick(Runnable listener) {
        tickListeners.add(listener);
    }

    public static void fireClientTick() {
        for (Runnable r : tickListeners) {
            try { r.run(); } catch (Throwable t) {
                System.err.println("[Seam] Tick listener threw: " + t);
            }
        }
    }

    /** Appends a dynamic suffix to the window title on every refresh. */
    public static void appendToTitle(Supplier<String> suffix) {
        titleAppenders.add(suffix);
    }

    public static boolean hasTitleAppenders() {
        return !titleAppenders.isEmpty();
    }

    /** Applies registered title appenders to the given base string. Pure — no side effects. */
    public static String processTitle(String base) {
        if (titleAppenders.isEmpty()) return base;
        StringBuilder sb = new StringBuilder(base);
        for (Supplier<String> s : titleAppenders) {
            try { sb.append(s.get()); } catch (Throwable t) {
                System.err.println("[Seam] Title appender threw: " + t);
            }
        }
        return sb.toString();
    }

    private SeamApi() {}
}
