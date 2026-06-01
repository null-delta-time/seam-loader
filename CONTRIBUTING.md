# Contributing

## Building

Requires JDK 21 and Gradle (wrapper included).

```
./gradlew assemble
```

Output: `installer/build/libs/seam-installer-<version>.jar`

## Adding a new MC version target

1. Create the module directory with a `gradle.properties`:
   ```properties
   tag=b170
   version=0.1.0-SNAPSHOT
   ```

2. Declare the MC version in its `build.gradle`:
   ```groovy
   ext.targetMcVersion = 'Beta 1.7.0'
   ```

The release workflow and version manifest updater pick it up automatically.

## Releasing

See [RELEASES.md](RELEASES.md).
