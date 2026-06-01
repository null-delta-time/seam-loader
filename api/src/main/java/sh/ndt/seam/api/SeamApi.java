package sh.ndt.seam.api;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class SeamApi {

    private static final List<ClassTransformer> transformers   = new CopyOnWriteArrayList<>();
    private static final List<Runnable>         tickListeners  = new CopyOnWriteArrayList<>();
    private static final List<Supplier<String>> titleAppenders = new CopyOnWriteArrayList<>();

    private static final Map<String, List<ConfigEntry>>       configEntries = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>>     configValues  = new ConcurrentHashMap<>();

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

    // ── config ────────────────────────────────────────────────────────────────

    public static void registerConfig(String modId, List<ConfigEntry> entries) {
        configEntries.put(modId, new ArrayList<>(entries));
        configValues.computeIfAbsent(modId, k -> {
            Map<String, Object> defaults = new LinkedHashMap<>();
            for (ConfigEntry e : entries) defaults.put(e.key(), e.defaultValue());
            return defaults;
        });
    }

    public static List<ConfigEntry> getConfigEntries(String modId) {
        List<ConfigEntry> entries = configEntries.get(modId);
        return entries != null ? entries : Collections.emptyList();
    }

    public static boolean hasConfig(String modId) {
        List<ConfigEntry> entries = configEntries.get(modId);
        return entries != null && !entries.isEmpty();
    }

    public static Object getConfigValue(String modId, String key) {
        Map<String, Object> vals = configValues.get(modId);
        return vals != null ? vals.get(key) : null;
    }

    public static void setConfigValue(String modId, String key, Object value) {
        configValues.computeIfAbsent(modId, k -> new LinkedHashMap<>()).put(key, value);
    }

    public static Map<String, Object> getAllConfigValues(String modId) {
        Map<String, Object> vals = configValues.get(modId);
        return vals != null ? new LinkedHashMap<>(vals) : Collections.emptyMap();
    }

    public static Set<String> getRegisteredConfigModIds() {
        return configEntries.keySet();
    }

    private SeamApi() {}
}
