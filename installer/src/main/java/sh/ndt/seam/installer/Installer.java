package sh.ndt.seam.installer;

import com.google.gson.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Installer {

    private final Path mcDir;
    private final Consumer<String> log;

    public Installer(Path mcDir, Consumer<String> log) {
        this.mcDir = mcDir;
        this.log = log;
    }

    public void install(String mcVersion, VersionManifest.VersionEntry entry) throws IOException {
        log("Creating seam/ directory...");
        Path seamDir = mcDir.resolve("seam");
        Files.createDirectories(seamDir.resolve("mods"));

        InstallerMeta.BundledLoader bundled = InstallerMeta.forMcVersion(mcVersion);
        Path agentDest = seamDir.resolve("seam-agent.jar");
        if (entry.url() == null) {
            if (bundled == null) throw new IOException(
                "No bundled agent for MC version '" + mcVersion + "' — rebuild installer or choose a downloadable version.");
            log("Extracting bundled seam-agent.jar...");
            extractResource(bundled.resourceName(), agentDest);
        } else {
            log("Downloading seam-agent " + entry.version() + "...");
            downloadAgent(entry.url(), agentDest);
        }

        log("Writing version descriptor...");
        String versionId = "seam-" + mcVersion;
        Path versionDir = mcDir.resolve("versions").resolve(versionId);
        Files.createDirectories(versionDir);
        writeVersionJson(mcVersion, seamDir.resolve("seam-agent.jar"), versionDir.resolve(versionId + ".json"));

        // The launcher derives the game JAR path from the version ID:
        // versions/<versionId>/<versionId>.jar. It must exist for the launcher to
        // compute ${classpath} and populate the -cp argument. Copy from the vanilla install.
        log("Copying game JAR...");
        copyGameJar(mcVersion, versionDir.resolve(versionId + ".jar"));

        log("Updating launcher_profiles.json...");
        updateProfiles(mcVersion);

        log("Installation complete.");
    }

    private void copyGameJar(String mcVersion, Path dest) throws IOException {
        if (Files.exists(dest)) return;
        Path vanilla = mcDir.resolve("versions").resolve(mcVersion).resolve(mcVersion + ".jar");
        if (!Files.exists(vanilla)) {
            throw new IOException(
                mcVersion + " is not installed. Please run Minecraft " + mcVersion + " at least once before installing Seam.");
        }
        Files.copy(vanilla, dest);
    }

    private void writeVersionJson(String mcVersion, Path agentJar, Path dest) throws IOException {
        String template;
        String path = "/sh/ndt/seam/installer/version-template.json";
        try (InputStream in = Installer.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("version-template.json missing from installer");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // ${game_directory} is not substituted in JVM args by the launcher; embed
        // the absolute agent path at install time.
        String agentPath = agentJar.toAbsolutePath().toString().replace("\\", "/");
        String json = template
            .replace("${game_directory}/seam/seam-agent.jar", agentPath)
            .replace("${seam_version_id}", "seam-" + mcVersion)
            .replace("${mc_version}", mcVersion);
        Files.writeString(dest, json, StandardCharsets.UTF_8);
    }

    private void downloadAgent(String url, Path dest) throws IOException {
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        } catch (Exception e) {
            throw new IOException("Invalid download URL: " + url, e);
        }
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log("Downloaded " + dest.getFileName() + ".");
    }

    private void extractResource(String name, Path dest) throws IOException {
        String path = "/sh/ndt/seam/installer/" + name;
        try (InputStream in = Installer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Installer resource missing: " + name +
                    "\nThe installer JAR may be incomplete — rebuild with `./gradlew :installer:jar`.");
            }
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void updateProfiles(String mcVersion) throws IOException {
        Path profilesFile = mcDir.resolve("launcher_profiles.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject root;
        if (Files.exists(profilesFile)) {
            try (Reader r = Files.newBufferedReader(profilesFile, StandardCharsets.UTF_8)) {
                root = gson.fromJson(r, JsonObject.class);
            }
            if (root == null) root = new JsonObject();
        } else {
            root = new JsonObject();
        }

        if (!root.has("profiles")) root.add("profiles", new JsonObject());

        String versionId = "seam-" + mcVersion;
        String profileKey = "seam-" + mcVersion.replace(".", "");

        JsonObject profile = new JsonObject();
        profile.addProperty("name", "Seam " + mcVersion);
        profile.addProperty("type", "custom");
        profile.addProperty("created", Instant.now().toString());
        profile.addProperty("lastUsed", "1970-01-01T00:00:00.000Z");
        profile.addProperty("lastVersionId", versionId);
        profile.addProperty("icon", "Furnace");

        root.getAsJsonObject("profiles").add(profileKey, profile);

        try (Writer w = Files.newBufferedWriter(profilesFile, StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
        }
    }

    // ── scan ─────────────────────────────────────────────────────────────────────

    public static List<InstallationRecord> scan(Path mcDir) {
        List<InstallationRecord> results = new ArrayList<>();
        Path profilesFile = mcDir.resolve("launcher_profiles.json");
        if (!Files.exists(profilesFile)) return results;

        JsonObject root;
        try (Reader r = Files.newBufferedReader(profilesFile, StandardCharsets.UTF_8)) {
            root = new Gson().fromJson(r, JsonObject.class);
        } catch (Exception e) {
            return results;
        }
        if (root == null || !root.has("profiles")) return results;

        boolean agentExists = Files.exists(mcDir.resolve("seam").resolve("seam-agent.jar"));

        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("profiles").entrySet()) {
            String key = entry.getKey();
            JsonObject profile = entry.getValue().getAsJsonObject();
            if (!profile.has("lastVersionId")) continue;
            String versionId = profile.get("lastVersionId").getAsString();
            if (!versionId.startsWith("seam-")) continue;

            String profileName = profile.has("name") ? profile.get("name").getAsString() : versionId;
            String inheritsFrom = readInheritsFrom(mcDir, versionId);
            results.add(new InstallationRecord(key, profileName, versionId, inheritsFrom, agentExists, mcDir));
        }

        results.sort(Comparator.comparing(InstallationRecord::versionId));
        return results;
    }

    private static String readInheritsFrom(Path mcDir, String versionId) {
        Path versionJson = mcDir.resolve("versions").resolve(versionId).resolve(versionId + ".json");
        if (!Files.exists(versionJson)) return "?";
        try (Reader r = Files.newBufferedReader(versionJson, StandardCharsets.UTF_8)) {
            JsonObject obj = new Gson().fromJson(r, JsonObject.class);
            if (obj != null && obj.has("inheritsFrom")) return obj.get("inheritsFrom").getAsString();
        } catch (Exception ignored) {}
        return "?";
    }

    // ── rename ───────────────────────────────────────────────────────────────────

    public void rename(InstallationRecord record, String newName) throws IOException {
        Path profilesFile = mcDir.resolve("launcher_profiles.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root;
        try (Reader r = Files.newBufferedReader(profilesFile, StandardCharsets.UTF_8)) {
            root = gson.fromJson(r, JsonObject.class);
        }
        if (root == null || !root.has("profiles")) return;
        JsonObject profiles = root.getAsJsonObject("profiles");
        if (!profiles.has(record.profileKey())) return;
        profiles.getAsJsonObject(record.profileKey()).addProperty("name", newName);
        try (Writer w = Files.newBufferedWriter(profilesFile, StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
        }
    }

    // ── remove ───────────────────────────────────────────────────────────────────

    public void remove(InstallationRecord record) throws IOException {
        log("Removing profile '" + record.profileKey() + "' from launcher_profiles.json...");
        removeProfile(record.profileKey());

        log("Deleting version directory versions/" + record.versionId() + "/...");
        Path versionDir = mcDir.resolve("versions").resolve(record.versionId());
        deleteDirectory(versionDir);

        if (scan(mcDir).isEmpty()) {
            log("No remaining installations — removing seam/ directory (agent + mods)...");
            deleteDirectory(mcDir.resolve("seam"));
        }

        log("Removed " + record.profileName() + ".");
    }

    private void removeProfile(String profileKey) throws IOException {
        Path profilesFile = mcDir.resolve("launcher_profiles.json");
        if (!Files.exists(profilesFile)) return;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root;
        try (Reader r = Files.newBufferedReader(profilesFile, StandardCharsets.UTF_8)) {
            root = gson.fromJson(r, JsonObject.class);
        }
        if (root == null || !root.has("profiles")) return;
        root.getAsJsonObject("profiles").remove(profileKey);
        try (Writer w = Files.newBufferedWriter(profilesFile, StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }

    private void log(String msg) {
        log.accept(msg);
    }
}
