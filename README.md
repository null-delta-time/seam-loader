# Seam

A mod loader for Minecraft Beta 1.8.1, built from scratch. Not a Fabric/Forge backport — a standalone tool that patches the original game JAR at load time using a Java agent.

## Installation

Download `seam-installer-<version>.jar` from the [releases page](../../releases) and run it:

```
java -jar seam-installer-0.1.0.jar
```

Or double-click it if your OS associates `.jar` with Java.

The installer:
1. Auto-detects your Minecraft directory
2. Installs `seam-agent.jar` into `<minecraft_dir>/seam/`
3. Creates a `seam-b1.8.1` version entry in `versions/`
4. Adds a **Seam b1.8.1** profile to `launcher_profiles.json`

Open the Minecraft Launcher, select **Seam b1.8.1**, and hit Play.

### Uninstalling

Open the installer, go to the **Installations** tab, and click **Remove**.

## Adding mods

Drop mod JARs into `<minecraft_dir>/seam/mods/`. Each mod must have a `seam.mod.json` at its JAR root — see [WRITING_MODS.md](WRITING_MODS.md).

## How it works

Seam runs as a Java agent (`-javaagent`) before the game starts. It uses ASM to patch obfuscated game bytecode at load time — no deobfuscation, no intermediary mappings, patches work directly on names like `bv`, `iw`, `sf`.

| Module | Role |
|---|---|
| `api` | Public contract for mods: `ModInitializer`, `ClassTransformer`, event types |
| `loader-core` | Version-agnostic: discovers mod JARs, reads `seam.mod.json`, classloads and initializes mods |
| `loader-b1.8.1` | b1.8.1-specific agent: registers bytecode transformers, wires into LaunchWrapper |

## License

MIT — see [LICENSE](LICENSE).
