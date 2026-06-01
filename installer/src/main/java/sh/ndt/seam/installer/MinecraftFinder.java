package sh.ndt.seam.installer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MinecraftFinder {
    public static Path find() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", "minecraft");
        }
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            return Paths.get(appdata != null ? appdata : home, ".minecraft");
        }
        return Paths.get(home, ".minecraft");
    }
}
