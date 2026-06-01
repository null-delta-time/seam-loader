# Release process

Tag and push — the workflow handles the rest.

```
git tag b181-v1.0.0
git push origin main --tags
```

The [release workflow](.github/workflows/release.yml) finds the subproject by matching the tag prefix against `tag=` in each module's `gradle.properties`, builds the JAR with the version from the tag, updates `versions.json` for loader modules, and publishes to GitHub Releases.

The version in `gradle.properties` stays `-SNAPSHOT` at all times. The [CI version-bump check](.github/workflows/ci.yml) enforces that any PR touching `src/` bumps the snapshot version — so by the time you tag, the version you're releasing is already in the file as `X.Y.Z-SNAPSHOT`.

## Hotfix on an old release

```
git checkout -b hotfix/b181-1.0.x b181-v1.0.0
# fix, bump version in loader-b1.8.1/gradle.properties to 1.0.1-SNAPSHOT
git commit -am "fix: ..."
git tag b181-v1.0.1
git push origin hotfix/b181-1.0.x --tags
```

Then cherry-pick the `versions.json` commit onto `main`.
