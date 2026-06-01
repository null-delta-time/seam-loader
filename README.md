# Seam

A mod loader for pre-release Minecraft versions, built from scratch. Not a Fabric/Forge backport — a standalone tool that patches the original game JAR at load time using a Java agent.

Currently supports **Beta 1.8.1**. Additional pre-release targets can be added as `loader-*` modules.

## Installation

Download `seam-installer-<version>.jar` from the [releases page](../../releases) and run it:

```
java -jar seam-installer-<version>.jar
```

Or double-click it if your OS associates `.jar` with Java.

The installer:
1. Auto-detects your Minecraft directory
2. Installs `seam-agent.jar` into `<minecraft_dir>/seam/`
3. Creates a version entry in `versions/`
4. Adds a **Seam** profile to `launcher_profiles.json`

Open the Minecraft Launcher, select the **Seam** profile, and hit Play.

### Uninstalling

Open the installer, go to the **Installations** tab, and click **Remove**.

## Adding mods

Drop mod JARs into `<minecraft_dir>/seam/mods/`. Each mod must have a `seam.mod.json` at its JAR root — see [WRITING_MODS.md](WRITING_MODS.md).

## How it works

Seam runs as a Java agent (`-javaagent`) before the game starts. It uses ASM to patch obfuscated game bytecode at load time — no deobfuscation, no intermediary mappings, patches work directly on raw obfuscated names.

| Module | Role |
|---|---|
| `api` | Public contract for mods: `ModInitializer`, `ClassTransformer`, event types |
| `loader-core` | Version-agnostic: discovers mod JARs, reads `seam.mod.json`, classloads and initializes mods |
| `loader-<version>` | Version-specific agent: registers bytecode transformers, wires into LaunchWrapper |
| `installer` | Swing GUI installer: auto-detects Minecraft dir, writes version descriptor, updates launcher profiles |

## Building

```
./gradlew installer:shadowJar   # produces installer/build/libs/seam-installer-<version>.jar
```

## License

MIT — see [LICENSE](LICENSE).
