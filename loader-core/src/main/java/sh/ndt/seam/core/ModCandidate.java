package sh.ndt.seam.core;

import java.nio.file.Path;

public final class ModCandidate {
    private final String id;
    private final String name;
    private final String version;
    private final Path jar;

    public ModCandidate(String id, String name, String version, Path jar) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.jar = jar;
    }

    public String id()      { return id; }
    public String name()    { return name; }
    public String version() { return version; }
    public Path jar()       { return jar; }
}
