# NL Portal Backend

![Java 21](https://img.shields.io/badge/Java-21-green)

The backend stack of the [NL Portal monorepo](../README.md). It is a single Kotlin/Spring Boot
multi-module Gradle project containing:

- the **backend libraries** - the backend-for-frontend (BFF) that the NL Portal frontend talks to,
  published as `nl.nl-portal:*` Maven artifacts; and
- **`app/`** - the shippable backend application that assembles those modules into a runnable
  service and is packaged as the `nl-portal-backend` image (see [app/README.md](./app/README.md)).

The backend is independent of process- or case-management systems and communicates a-sync with
supporting (ZGW) services.

## Requirements

- Java 21
- Docker (integration tests and the local supporting-services stack rely on it)

## Building

From `backend/`:

```shell
./gradlew build
```

Integration tests auto-provision a PostgreSQL container through the docker-compose Gradle plugin, so
Docker must be running. To build only the application jar:

```shell
./gradlew :app:bootJar
```

## Running locally

Start the supporting services from the monorepo's [`docker-compose/`](../docker-compose/README.md)
stack, then run the app:

```shell
cd ../docker-compose && docker compose --profile zgw up -d
cd ../backend && ./gradlew :app:bootRun
```

See [app/README.md](./app/README.md) for configuration details.

## Project layout

- `app/` - the shippable backend application (`bootJar`; Dockerfile at [`backend/Dockerfile`](./Dockerfile)).
- `core/`, `graphql/`, `case/`, `form/`, `zgw/`, ... - library modules, consumed by the app via
  `project(":…")` references.

Modules are configured centrally (`build.gradle.kts` + `buildSrc/`); shared dependency versions live
in `buildSrc`.

## Known issues

**IntelliJ on macOS** may fail to start the Docker containers used by tests:

```
Cannot run program "docker" ... error=2, No such file or directory
```

This happens when IntelliJ is launched from the JetBrains Toolbox or the macOS UI. Work around it by
starting IntelliJ from the terminal (`open -a "IntelliJ IDEA Ultimate"`) or running the Gradle
command from the terminal.

## More information

See the [monorepo README](../README.md) and the [documentation](https://nl-portal.nl).
