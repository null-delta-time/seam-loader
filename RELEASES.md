# Release process

Releases are triggered manually via the [Release workflow](.github/workflows/release-all.yml) on GitHub Actions (Actions → Release → Run workflow).

The version in `gradle.properties` stays `-SNAPSHOT` at all times. The [CI version-bump check](.github/workflows/ci.yml) enforces that any PR touching `src/` bumps the snapshot version. The workflow strips `-SNAPSHOT` at release time — no git tags to push manually.

## Workflow inputs

| Input | Default | Description |
|---|---|---|
| `release_core` | false | Release `api` + `loader-core` (tagged `core-v{VERSION}`) |
| `release_loaders` | `all` | `all`, comma-separated loader short names (e.g. `b181`), or empty/`none` to skip |
| `release_installer` | true | Release the installer (do this whenever releasing loaders) |

Each loader's tag prefix comes from `tag=` in its `gradle.properties`. A loader is only eligible if its `build.gradle` contains `targetMcVersion`.

After a loader release, `versions.json` is updated automatically and committed back to `main`.

## Hotfix on an old release

```
# The release workflow creates a tag (e.g. b181-v0.1.0) — branch from it
git checkout -b hotfix/b181-0.1.x b181-v0.1.0
# fix, bump loader-b1.8.1/gradle.properties to 0.1.1-SNAPSHOT
git commit -am "fix: ..."
git push origin hotfix/b181-0.1.x
# Trigger the Release workflow on the hotfix branch, release_loaders=b181
```

Then cherry-pick the `versions.json` commit onto `main`.
