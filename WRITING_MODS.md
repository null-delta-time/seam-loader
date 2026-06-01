# Writing mods for Seam

See [`example/`](example/) in this repo for a working reference mod you can copy as a starting point.

## Mod manifest

Every mod JAR must contain a `seam.mod.json` at its root:

```json
{
  "id": "my-mod",
  "name": "My Mod",
  "version": "1.0.0",
  "entrypoint": "com.example.mymod.MyMod"
}
```

## Entry point

Implement `ModInitializer` from the `api` module:

```java
public class MyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // register transformers, set up event listeners, etc.
    }
}
```

## Events

Subscribe to built-in events in `onInitialize`:

```java
SeamApi.onClientTick(() -> {
    // runs every game tick (~20/s)
});
```

## Bytecode patching

To patch game bytecode, implement `ClassTransformer` and register it in `onInitialize`:

```java
SeamApi.registerTransformer((internalName, bytes) -> {
    if (!"bv".equals(internalName)) return null;
    // use ASM to patch bytes
    return patchedBytes;
});
```

Transformers receive **obfuscated** class names (e.g. `bv`, `sf`). Seam makes no attempt to deobfuscate them — patch against the original Beta 1.8.1 names directly.

## Developer options

Pass these as JVM arguments (e.g. via the launcher's JVM arguments field):

| Property | Default | Description |
|---|---|---|
| `-Dseam.debugStderr=true` | off | Redirect stderr to stdout, each line prefixed with `[stderr]`. |
| `-Dseam.gameDir=<path>` | `.` | Override the game directory used for mod discovery. |
