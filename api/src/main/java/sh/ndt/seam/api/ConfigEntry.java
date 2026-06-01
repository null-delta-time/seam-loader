package sh.ndt.seam.api;

public final class ConfigEntry {

    public enum Type { BOOLEAN }

    private final String key;
    private final String displayName;
    private final Type type;
    private final Object defaultValue;

    public ConfigEntry(String key, String displayName, Type type, Object defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String key()          { return key; }
    public String displayName()  { return displayName; }
    public Type type()           { return type; }
    public Object defaultValue() { return defaultValue; }
}
