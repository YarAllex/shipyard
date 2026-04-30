# Shipyard

Gradle plugin: conventional-commits SemVer + Docker image release.

Forked out of a hand-rolled `dockerRelease` flow. Replaces axion-release for the simple case
(SemVer from git tags, bumped by conventional-commits) and bundles Docker login/build/push tasks
that play nicely with `ghcr.io`.

## Apply

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.github.com/YarAllex/shipyard")
        }
    }
}
```

`build.gradle.kts`:

```kotlin
plugins {
    id("dev.yarallex.shipyard") version "0.1.0"
}

shipyard {
    imageRepo.set("ghcr.io/your-org/your-service")
}
```

## Configuration

The `shipyard { ... }` extension has the following properties. Only `imageRepo` is required;
the rest fall back to sensible defaults.

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `imageRepo` | `String` | — *(required)* | Fully-qualified image name without tag, e.g. `ghcr.io/your-org/your-service`. Used by `dockerBuild` / `dockerPush*`. |
| `initialVersion` | `String` | `"0.1.0"` | SemVer assigned when no matching git tag exists yet. The very first `release` will create `<tagPrefix><initialVersion>` without bumping. |
| `tagPrefix` | `String` | `"v"` | Prefix in front of the SemVer in git tags. The plugin scans `git describe --tags --match=<tagPrefix>*` and writes new tags as `<tagPrefix><version>`. Set to `""` to use bare SemVer tags like `1.2.3`. |
| `gitRemote` | `String` | `"origin"` | Remote that `pushTag` pushes the new version tag to. |
| `registryHost` | `String` | `"ghcr.io"` | Host argument for `docker login`. Change to `docker.io`, `registry.gitlab.com`, etc. for non-GHCR registries. |
| `registryUserEnv` | `String` | `"GHCR_USER"` | Name of the environment variable the `dockerLogin` task reads to get the registry username. |
| `registryTokenEnv` | `String` | `"GHCR_TOKEN"` | Name of the environment variable the `dockerLogin` task reads (via stdin) to get the registry password / token. |
| `dockerBin` | `String` | `"docker"` | Path or name of the docker CLI. Override if `docker` is not on `PATH` (e.g. `"/usr/local/bin/docker"`). |
| `gitBin` | `String` | `"git"` | Path or name of the git CLI. Same use case as `dockerBin`. |
| `buildTaskName` | `String` | *(unset)* | Optional name of a task that must run before `dockerBuild`. Set to e.g. `"build"` so the JAR/binary is produced and packaged before the docker image is built. Leave unset for projects where `dockerBuild` is self-sufficient. |
| `requireCleanWorkingTree` | `Boolean` | `true` | When `true`, the `release` task aborts if `git status --porcelain` reports any change. Set to `false` only for sandboxes where dirty trees are expected. |

Example with overrides for a non-GHCR registry and a Spring Boot project:

```kotlin
shipyard {
    imageRepo.set("registry.gitlab.com/acme/api")
    registryHost.set("registry.gitlab.com")
    registryUserEnv.set("GITLAB_USER")
    registryTokenEnv.set("GITLAB_TOKEN")
    buildTaskName.set("bootJar")
    initialVersion.set("1.0.0")
    tagPrefix.set("release-")
}
```

## Tasks

| Task | Description |
| --- | --- |
| `currentVersion` | Print SemVer derived from the latest `v*` git tag. |
| `nextVersion` | Print the next SemVer based on conventional-commits since the last tag. |
| `tagVersion` | Create the next-version git tag locally (no push, no build). |
| `pushTag` | Push the local version tag to the configured remote. |
| `dockerLogin` | Login to the configured registry using env-var credentials. |
| `dockerBuild` | `docker build -t repo:<version> -t repo:latest .` |
| `dockerPushVersion` | Push `repo:<version>` to the registry. |
| `dockerPushLatest` | Push `repo:latest` to the registry. |
| `dockerPush` | Aggregate of `dockerPushVersion` + `dockerPushLatest`. |
| `release` | Full pipeline: `tagVersion` -> `dockerPush` -> `pushTag`. |

## Required env vars

By default the `dockerLogin` task reads `GHCR_USER` and `GHCR_TOKEN` from the environment.
Both names are configurable via `registryUserEnv` / `registryTokenEnv`.

## Versioning rules

- Latest tag matching `<tagPrefix>*` is parsed as SemVer.
- `git log <previousTag>..HEAD` is scanned:
  - `BREAKING CHANGE:` footer or `<type>!:` header -> major
  - `feat[(scope)]:` -> minor
  - `fix[(scope)]:` -> patch
  - otherwise no bump (release task is a no-op).
- No previous tag -> `initialVersion` is used as-is.

## License

Apache 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
