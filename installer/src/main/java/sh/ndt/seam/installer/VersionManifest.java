package sh.ndt.seam.installer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.ArrayList;

public class VersionManifest {

    public static final String MANIFEST_URL =
        "https://raw.githubusercontent.com/null-delta-time/seam-loader/main/installer/src/main/resources/sh/ndt/seam/installer/versions.json";

    private static final int TIMEOUT_MS = 4_000;

    public record VersionEntry(String version, String url) {
        public boolean isLocal() { return url == null; }

        public String displayLabel(boolean latest) {
            if (isLocal()) return version + " (local build)";
            return latest ? version + " (latest)" : version;
        }
    }

    private final Map<String, List<VersionEntry>> versions; // mutable — injectLocalBuild may add entries

    private VersionManifest(Map<String, List<VersionEntry>> versions) {
        this.versions = versions;
    }

    public Set<String> mcVersions() {
        return versions.keySet();
    }

    public List<VersionEntry> seamVersions(String mcVersion) {
        return versions.getOrDefault(mcVersion, List.of());
    }

    // ── loading ───────────────────────────────────────────────────────────────

    public static VersionManifest load() {
        VersionManifest m;
        try {
            m = fetchRemote();
        } catch (Exception ignored) {
            m = loadBundled();
        }
        if (InstallerMeta.IS_SNAPSHOT) m.injectLocalBuild();
        return m;
    }

    private void injectLocalBuild() {
        for (InstallerMeta.BundledLoader loader : InstallerMeta.LOADERS) {
            if (!loader.isSnapshot()) continue;
            VersionEntry local = new VersionEntry(loader.version, null);
            List<VersionEntry> list = new ArrayList<>(versions.getOrDefault(loader.mcVersion, List.of()));
            list.removeIf(e -> e.version().equals(local.version()));
            list.add(0, local);
            versions.put(loader.mcVersion, list);
        }
    }

    private static VersionManifest fetchRemote() throws Exception {
        if (MANIFEST_URL.isEmpty()) throw new IllegalStateException("no manifest URL configured");
        HttpURLConnection conn = (HttpURLConnection) new URI(MANIFEST_URL).toURL().openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        try (Reader r = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            return parse(r);
        }
    }

    private static VersionManifest loadBundled() {
        try (InputStream in = VersionManifest.class.getResourceAsStream(
                "/sh/ndt/seam/installer/versions.json")) {
            if (in != null) return parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        var fallback = new LinkedHashMap<String, List<VersionEntry>>();
        for (InstallerMeta.BundledLoader loader : InstallerMeta.LOADERS) {
            fallback.put(loader.mcVersion, new ArrayList<>(List.of(
                new VersionEntry(loader.version, null))));
        }
        return new VersionManifest(fallback);
    }

    private static VersionManifest parse(Reader r) {
        Type type = new TypeToken<LinkedHashMap<String, ArrayList<VersionEntry>>>() {}.getType();
        LinkedHashMap<String, ArrayList<VersionEntry>> map = new Gson().fromJson(r, type);
        if (map == null) return new VersionManifest(new LinkedHashMap<>());
        // cast is safe — ArrayList<VersionEntry> is a List<VersionEntry>
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, List<VersionEntry>> cast = (LinkedHashMap<String, List<VersionEntry>>) (Map<?, ?>) map;
        return new VersionManifest(cast);
    }
}
