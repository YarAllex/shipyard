# Shipyard

Gradle plugin for **conventional-commits SemVer** + **Docker image release**.

One `./gradlew ship` does the whole loop:

```
nextVersion → git tag → docker build → docker push → push tag
```

No bash scripts, no axion-release setup, no `gradle.properties` ceremony for credentials.

## Prerequisites

The plugin shells out to `git` and `docker`. It assumes the host environment is already set up:

- **`docker` CLI** on `PATH`, daemon running, able to build the project's `Dockerfile`.
- **`git` CLI** on `PATH`, repository has at least one commit, and the configured remote is **authenticated**:
  - **SSH remote** (`git@github.com:...`) — your key must be loaded into `ssh-agent`. The plugin runs `git push` non-interactively and cannot prompt for a passphrase.
    ```bash
    # macOS, persistent
    ssh-add --apple-use-keychain ~/.ssh/id_ed25519
    # ~/.ssh/config:
    #   Host github.com
    #     AddKeysToAgent yes
    #     UseKeychain yes
    ```
    ```bash
    # Linux, per-shell
    eval "$(ssh-agent -s)" && ssh-add ~/.ssh/id_ed25519
    ```
  - **HTTPS remote** — configure a credential helper or use a PAT:
    ```bash
    git config --global credential.helper osxkeychain   # macOS
    git config --global credential.helper store         # Linux
    git config --global credential.helper manager       # Windows
    ```
- **Registry credentials** for `dockerLogin`. Either set them in the process environment or in a `.env` file at the project root (see [Credentials](#credentials)).

In CI all of the above is normally handled by the standard checkout / login actions; nothing extra is required from this plugin.

## Quick start

`build.gradle.kts`:

```kotlin
plugins {
    id("dev.yarallex.shipyard") version "0.1.1"
}

shipyard {
    imageRepo = "your-org/your-service"
}
```

`registryHost` defaults to `ghcr.io`. The plugin auto-prefixes `imageRepo` with the host when it lacks one, so the example above pushes to `ghcr.io/your-org/your-service`. Pass an already-qualified repo (`docker.io/...`, `registry.gitlab.com/...`) and `registryHost` is derived from it — no need to set it twice.

That is the minimum config for GHCR. Make sure `Dockerfile` exists at the project root.

Cut a release:

```bash
export GHCR_USER=your-user
export GHCR_TOKEN=ghp_xxx        # PAT with write:packages scope
./gradlew ship
```

## How versions are decided

The plugin parses commits between the last `v*` tag and `HEAD`:

| Commit type | Bump |
|---|---|
| `BREAKING CHANGE:` footer or `<type>!:` header | MAJOR |
| `feat:` / `feat(scope):` | MINOR |
| `fix:` / `fix(scope):` | PATCH |
| anything else | none — `ship` is a no-op |

A whole window between two tags collapses into **one** bump — the highest tier wins. Two `feat:` commits do not produce two minor versions; they produce one.

If no matching tag exists yet, `initialVersion` (default `0.1.0`) is used and the first `ship` tags it as-is without bumping.

## Tasks

| Task | What it does |
|---|---|
| `currentVersion` | Print SemVer from the latest matching git tag. |
| `nextVersion` | Print the next SemVer based on commits since the last tag. |
| `tagVersion` | Create the next-version git tag locally (no push, no build). |
| `pushTag` | Push the local version tag to `gitRemote`. |
| `dockerLogin` | Login to `registryHost` using env-var credentials. |
| `dockerBuild` | `docker build -t imageRepo:<version> -t imageRepo:latest .` |
| `dockerPushVersion` | Push `imageRepo:<version>`. |
| `dockerPushLatest` | Push `imageRepo:latest`. |
| `dockerPush` | `dockerPushVersion` + `dockerPushLatest`. |
| `ship` | Full pipeline: `tagVersion` → `dockerPush` → `pushTag`. |

`./gradlew tasks --group=shipyard` lists them in your project.

## Sync `project.version` with the release tag

The plugin exposes lazy providers on the extension:

| Property | Type | Description |
|---|---|---|
| `shipyard.currentVersion` | `Provider<String>` | SemVer of the latest matching git tag (or `initialVersion` if none). |
| `shipyard.nextVersion` | `Provider<String>` | What `ship` would produce now: `currentVersion` + computed bump. |

Use either one to keep your JAR/artifact name aligned with the docker tag:

```kotlin
shipyard {
    imageRepo = "ghcr.io/acme/api"
}

version = shipyard.nextVersion.get()
```

Now `bootJar` produces `api-1.2.3.jar` and the image is tagged `:1.2.3` — single source of truth (git).

## Configuration

All fields on the `shipyard { }` extension. Only `imageRepo` is required.

| Property | Type | Default | Description |
|---|---|---|---|
| `imageRepo` | `String` | — *(required)* | Image name without tag, e.g. `acme/api` or `ghcr.io/acme/api`. If the first segment is not a host (no `.`, `:`, and not `localhost`), `registryHost` is prepended automatically. |
| `initialVersion` | `String` | `"0.1.0"` | Used when no matching tag exists yet. |
| `tagPrefix` | `String` | `"v"` | Prefix for SemVer git tags. Set to `""` for bare `1.2.3` tags. |
| `gitRemote` | `String` | `"origin"` | Remote `pushTag` pushes to. |
| `registryHost` | `String` | `"ghcr.io"` | Fallback host for `docker login` and the auto-prefix. Ignored if `imageRepo` already contains a host segment. |
| `registryUserEnv` | `String` | `"GHCR_USER"` | Env var name the plugin reads for the registry username. |
| `registryTokenEnv` | `String` | `"GHCR_TOKEN"` | Env var name for the registry token / password (read via stdin). |
| `dockerBin` | `String` | `"docker"` | Path or name of the docker CLI. |
| `gitBin` | `String` | `"git"` | Path or name of the git CLI. |
| `buildTaskName` | `String` | *(unset)* | Optional task name that must run before `dockerBuild` (e.g. `"bootJar"`, `"build"`). Leave unset for projects whose `Dockerfile` does the build itself. |
| `requireCleanWorkingTree` | `Boolean` | `true` | `ship` aborts on a dirty working tree. Set `false` only for sandboxes. |
| `envFile` | `RegularFile` | `<project>/.env` | File scanned for `registryUserEnv` / `registryTokenEnv` when they aren't set in the real environment. Real env vars always win. Missing file is silently ignored. |

## Credentials

`dockerLogin` reads `registryUserEnv` and `registryTokenEnv` first from the process environment, then falls back to `envFile` (default `.env` at the project root).

```dotenv
# .env
GHCR_USER=your-user
GHCR_TOKEN=ghp_xxx
```

Format: bare `KEY=value` lines, optional `export ` prefix, `#` comments, single or double quotes around values. No interpolation.

⚠️ **Add `.env` to `.gitignore`.** Never commit it. In CI use real env vars (GitHub Actions secrets, etc.) — they take precedence over the file.

## Examples

### GHCR (GitHub Container Registry)

This is the default. The plugin assumes GHCR unless `imageRepo` carries a different host.

```kotlin
shipyard {
    imageRepo = "your-org/your-service"
}
```

```bash
export GHCR_USER=your-user
export GHCR_TOKEN=ghp_xxx        # PAT with write:packages scope
./gradlew ship
```

In GitHub Actions use `${{ github.actor }}` and `${{ secrets.GITHUB_TOKEN }}` — no PAT needed. See [CI tips](#ci-tips).

### Docker Hub

```kotlin
shipyard {
    imageRepo = "docker.io/yarallex/api"
    registryUserEnv = "DOCKERHUB_USER"
    registryTokenEnv = "DOCKERHUB_TOKEN"
}
```

```bash
export DOCKERHUB_USER=yarallex
export DOCKERHUB_TOKEN=dckr_pat_xxx        # https://hub.docker.com/settings/security
./gradlew ship
```

`registryHost` is auto-derived from `imageRepo` — no need to set it explicitly.

### GitLab Container Registry

```kotlin
shipyard {
    imageRepo = "registry.gitlab.com/acme/api"
    registryUserEnv = "GITLAB_USER"
    registryTokenEnv = "GITLAB_TOKEN"
}
```

```bash
export GITLAB_USER=your-user
export GITLAB_TOKEN=glpat_xxx              # personal/deploy token, scope: write_registry
./gradlew ship
```

### Spring Boot project (full setup)

Recommended pattern: host builds the JAR via `bootJar`, the Dockerfile only copies the artifact and uses Spring's [layered JAR](https://docs.spring.io/spring-boot/reference/packaging/efficient.html) for cache-friendly pushes.

`build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
    id("org.springframework.boot") version "3.5.12"
    id("io.spring.dependency-management") version "1.1.7"
    id("dev.yarallex.shipyard") version "0.1.1"
}

group = "com.acme"

shipyard {
    imageRepo = "ghcr.io/acme/api"
    buildTaskName = "bootJar"          // dockerBuild depends on bootJar
}

version = shipyard.nextVersion.get()    // JAR name matches release tag

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    // ...
}
```

`Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /build
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./
ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]
```

`.dockerignore` (critical — without it `docker build` ships the entire repo as build context):

```
.git
.gradle
build/tmp
build/reports
build/test-results
src
*.md
```

Release flow:

```bash
git commit -am "feat: add /healthz endpoint"
./gradlew ship
# → next version computed, e.g. 1.4.0
# → bootJar produces api-1.4.0.jar
# → docker build extracts layered JAR
# → image tagged ghcr.io/acme/api:1.4.0 + :latest
# → pushed to GHCR
# → git tag v1.4.0 pushed to origin
```

Full source of truth: the git tag. JAR name, image tag, project version — all derived from it via `shipyard.nextVersion`.

### Inspect without releasing

```bash
./gradlew currentVersion        # what tag we're on
./gradlew nextVersion           # what release would produce now
./gradlew tagVersion            # create local tag, do not push, do not build
```

### Skip the git push

Use Gradle's standard `-x` to drop a stage. Useful for testing the docker pipeline without touching the remote:

```bash
./gradlew ship -x pushTag       # tag locally + build + push image, do not push tag
```

## CI tips

- Run `./gradlew ship` on a protected branch only.
- Pass credentials as masked env vars, never as `-P` properties (those leak into Gradle build scans).
- For shallow CI clones, fetch enough history to see the last tag: `git fetch --tags --depth=100`.

### GitHub Actions

`actions/checkout@v4` configures `git` push credentials via `GITHUB_TOKEN` automatically. Wire registry creds and run `ship`:

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0          # need full history for tag detection

- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: 21

- name: Release
  env:
    GHCR_USER: ${{ github.actor }}
    GHCR_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: ./gradlew ship
```

For Docker Hub or another registry, set `registryHost` / `registryUserEnv` / `registryTokenEnv` in `shipyard { }` and pass the matching secrets.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `git push origin v… failed. SSH auth failed.` | Key not loaded into `ssh-agent`, or no SSH key on the host. See [Prerequisites](#prerequisites). |
| `git push … HTTPS auth failed.` | Credential helper not configured, or PAT missing/expired. |
| `denied: requested access to the resource is denied` from `docker push` | The image was tagged for a different registry than `dockerLogin` authenticated to. Make sure `imageRepo` and `registryHost` agree (or rely on auto-prefixing). |
| `'GHCR_TOKEN' is not set in the environment or .env file.` | Neither the env var nor `.env` provided the credential. Export it or add it to `.env`. |
| `nextVersion` keeps printing the same value after multiple commits | Expected: the bump for the whole window between two tags is collapsed into one. Run `tagVersion` (or `ship`) to close the window. |

## License

Apache 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
