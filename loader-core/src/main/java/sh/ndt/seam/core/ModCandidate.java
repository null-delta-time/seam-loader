package sh.ndt.seam.core;

import java.net.URLClassLoader;
import java.nio.file.Path;

public final class ModCandidate {
    private final String id;
    private final String name;
    private final String version;
    private final Path jar;
    private final URLClassLoader loader;

    public ModCandidate(String id, String name, String version, Path jar, URLClassLoader loader) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.jar = jar;
        this.loader = loader;
    }

    public String id()             { return id; }
    public String name()           { return name; }
    public String version()        { return version; }
    public Path jar()              { return jar; }
    public URLClassLoader loader() { return loader; }
}
