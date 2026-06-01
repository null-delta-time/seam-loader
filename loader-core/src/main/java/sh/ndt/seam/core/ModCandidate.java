package sh.ndt.seam.core;

import java.net.URLClassLoader;
import java.nio.file.Path;

public final class ModCandidate {
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final Path jar;
    private final URLClassLoader loader;

    public ModCandidate(String id, String name, String version, String description, Path jar, URLClassLoader loader) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.jar = jar;
        this.loader = loader;
    }

    public String id()             { return id; }
    public String name()           { return name; }
    public String version()        { return version; }
    public String description()    { return description; }
    public Path jar()              { return jar; }
    public URLClassLoader loader() { return loader; }
}
