package sh.ndt.seam.installer;

import java.nio.file.Path;

public record InstallationRecord(
    String profileKey,
    String profileName,
    String versionId,
    String inheritsFrom,
    boolean agentExists,
    Path mcDir
) {}
