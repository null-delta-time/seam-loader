package sh.ndt.seam.core;

import com.google.gson.Gson;
import sh.ndt.seam.api.ModInitializer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SeamLoader {

    private static SeamLoader instance;

    public static SeamLoader getInstance() { return instance; }

    public static void setInstance(SeamLoader loader) {
        if (instance != null) throw new IllegalStateException("SeamLoader already initialized");
        instance = loader;
    }

    private static final Gson GSON = new Gson();
    private final List<ModCandidate> loaded = new ArrayList<>();

    public void discover(Path modsDir) throws IOException {
        if (!Files.isDirectory(modsDir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path jar : stream) {
                try {
                    loadMod(jar);
                } catch (Exception e) {
                    System.out.println("[Seam] Failed to load mod " + jar.getFileName() + ": " + e);
                    e.printStackTrace(System.out);
                }
            }
        }
    }

    private void loadMod(Path jarPath) throws Exception {
        ModMeta meta = readMeta(jarPath);
        if (meta == null) {
            System.out.println("[Seam] Skipping " + jarPath.getFileName() + ": no seam.mod.json");
            return;
        }
        if (meta.id == null || meta.entrypoint == null) {
            System.out.println("[Seam] Skipping " + jarPath.getFileName() + ": seam.mod.json missing id or entrypoint");
            return;
        }

        URLClassLoader modLoader = new URLClassLoader(
            new URL[]{jarPath.toUri().toURL()},
            ClassLoader.getSystemClassLoader()
        );

        Class<?> cls = Class.forName(meta.entrypoint, true, modLoader);
        ModInitializer initializer = (ModInitializer) cls.getDeclaredConstructor().newInstance();
        initializer.onInitialize();

        String displayName = meta.name != null ? meta.name : meta.id;
        String version = meta.version != null ? meta.version : "?";
        loaded.add(new ModCandidate(meta.id, displayName, version, jarPath, modLoader));
        System.out.println("[Seam] Loaded mod: " + displayName + " " + version);
    }

    private static ModMeta readMeta(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("seam.mod.json");
            if (entry == null) return null;
            try (InputStreamReader reader = new InputStreamReader(
                    jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, ModMeta.class);
            }
        }
    }

    public List<ModCandidate> getMods() {
        return Collections.unmodifiableList(loaded);
    }

    public int getModCount() {
        return loaded.size();
    }
}
