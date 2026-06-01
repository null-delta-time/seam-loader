package sh.ndt.seam.api;

public interface ClassTransformer {
    /** Return transformed bytes, or null to leave the class unchanged. */
    byte[] transform(String internalName, byte[] bytes);
}
