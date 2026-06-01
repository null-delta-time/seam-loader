package sh.ndt.seam.b181.config;

import sh.ndt.seam.api.ConfigEntry;
import sh.ndt.seam.api.SeamApi;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigManager {

    private static Path configDir;

    public static void init(Path gameDir) {
        configDir = gameDir.resolve("seam/config");
    }

    public static void loadAll() {
        for (String modId : SeamApi.getRegisteredConfigModIds()) {
            load(modId);
        }
    }

    public static void load(String modId) {
        if (configDir == null) return;
        Path file = configDir.resolve(modId + ".json").normalize();
        if (!file.startsWith(configDir)) return;
        if (!Files.exists(file)) return;
        try {
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            for (Map.Entry<String, Object> e : parseJson(json).entrySet()) {
                SeamApi.setConfigValue(modId, e.getKey(), e.getValue());
            }
        } catch (Exception e) {
            System.err.println("[Seam] Failed to load config for " + modId + ": " + e);
        }
    }

    public static void save(String modId) {
        if (configDir == null) return;
        try {
            Files.createDirectories(configDir);
            Map<String, Object> all = SeamApi.getAllConfigValues(modId);
            Map<String, Object> toSave = new LinkedHashMap<>();
            for (ConfigEntry e : SeamApi.getConfigEntries(modId)) {
                Object val = all.get(e.key());
                if (val != null) toSave.put(e.key(), val);
            }
            Path file = configDir.resolve(modId + ".json").normalize();
            if (!file.startsWith(configDir)) return;
            Files.write(file, toJson(toSave).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[Seam] Failed to save config for " + modId + ": " + e);
        }
    }

    // ── minimal JSON helpers (flat key→boolean/string only) ───────────────────

    private static String toJson(Map<String, Object> values) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : values.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\n  \"").append(jsonEscape(e.getKey())).append("\": ");
            Object v = e.getValue();
            if (v instanceof Boolean) sb.append(v);
            else sb.append('"').append(jsonEscape(String.valueOf(v))).append('"');
            first = false;
        }
        sb.append("\n}");
        return sb.toString();
    }

    private static Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        int i = 0, len = json.length();
        while (i < len) {
            int ks = json.indexOf('"', i);
            if (ks < 0) break;
            int ke = json.indexOf('"', ks + 1);
            if (ke < 0) break;
            String key = json.substring(ks + 1, ke);
            int colon = json.indexOf(':', ke + 1);
            if (colon < 0) break;
            int vs = colon + 1;
            while (vs < len && (json.charAt(vs) == ' ' || json.charAt(vs) == '\t'
                    || json.charAt(vs) == '\n' || json.charAt(vs) == '\r')) vs++;
            if (vs >= len) break;
            char c = json.charAt(vs);
            if (c == 't' && json.startsWith("true", vs)) {
                result.put(key, Boolean.TRUE);
                i = vs + 4;
            } else if (c == 'f' && json.startsWith("false", vs)) {
                result.put(key, Boolean.FALSE);
                i = vs + 5;
            } else if (c == '"') {
                int ve = json.indexOf('"', vs + 1);
                if (ve < 0) break;
                result.put(key, json.substring(vs + 1, ve));
                i = ve + 1;
            } else {
                i = vs + 1;
            }
        }
        return result;
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ConfigManager() {}
}
