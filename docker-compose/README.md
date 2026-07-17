# nl-portal-docker-compose

<!-- TOC -->
* [nl-portal-docker-compose](#nl-portal-docker-compose)
  * [Getting started](#getting-started)
    * [Requirements](#requirements)
    * [Compose structure and profiles](#compose-structure-and-profiles)
    * [Running the application](#running-the-application)
      * [Local (build from source)](#local-build-from-source)
      * [Remote (published images)](#remote-published-images)
      * [From sources (active development, fast inner loop)](#from-sources-active-development-fast-inner-loop)
    * [Starting up supporting services only](#starting-up-supporting-services-only)
      * [Including all ZGW related services](#including-all-zgw-related-services)
      * [Keycloak and database only](#keycloak-and-database-only)
  * [Maintenance](#maintenance)
    * [Updating Openzaak data](#updating-openzaak-data)
  * [Known issues](#known-issues)
<!-- TOC -->

## Getting started
### Requirements
- [Docker Desktop](https://docs.docker.com/desktop/install/)
- [Docker Compose](https://docs.docker.com/compose/install/)

> **Linux (Docker Engine, not Docker Desktop):** the `local` build mode (`RUN_MODE=local ... --build`)
> needs the [Buildx](https://docs.docker.com/build/concepts/overview/) plugin, which Docker Desktop
> bundles but a plain Docker Engine install does not. Without it the build fails. Install it from
> Docker's package repo, e.g. `sudo apt-get install docker-buildx-plugin` (Debian/Ubuntu) or the
> equivalent `docker-buildx` package for your distro. The `remote` and `sources` modes do not need it.

### Compose structure and profiles
The compose file is built up out of sets of containers (profiles) that each enable a piece of
functionality, so you only start what you need. A service without a profile is a core
requirement and is always started (even with a bare `docker compose up`). Profiles combine:
e.g. `--profile zgw` starts the full ZGW suite, while the granular `openzaak` / `objecten` /
`openklant` / `openproduct` profiles let you start a single component of it.

Whether (and how) the NL Portal app itself runs is a separate dimension, controlled by the
`RUN_MODE` variable rather than a profile: `sources` (default) runs no app container and leaves the
app ports free, `local` builds and runs the app from source, `remote` runs the published images.
See [Running the application](#running-the-application).

All ports below are exposed via the `localhost` helper container (the services share its
network namespace), so from your host every service is reachable on `localhost:<port>`.

| Service                                | Port  | Profile / RUN_MODE|
|----------------------------------------|-------|-------------------|
| NL Portal Backend (build from source)  | 8080  | RUN_MODE=local    |
| NL Portal Frontend (build from source) | 3000  | RUN_MODE=local    |
| NL Portal Backend (published image)    | 8080  | RUN_MODE=remote   |
| NL Portal Frontend (published image)   | 3000  | RUN_MODE=remote   |
| NL Portal Database (postgres)          | 5432 | - (core)          |
| Keycloak                               | 8082  | - (core)          |
| Keycloak Database (postgres)           | -     | - (core)          |
| Open Zaak                              | 8001  | zgw, openzaak     |
| Open Zaak Database (postgis)           | -     | zgw, openzaak     |
| OpenKlant 2                            | 8014  | zgw, openklant    |
| OpenKlant 2 Database (postgres)        | -     | zgw, openklant    |
| Objecten API                           | 8010  | zgw, objecten     |
| Objecttypen API                        | 8011  | zgw, objecten     |
| Open Notificaties                      | 8012  | zgw, objecten     |
| OpenProduct                            | 8015  | zgw, openproduct  |
| Redis                                  | -     | zgw (sub-profiles)|
| Haalcentraal BRP                       | 5010  | haalcentraal      |
| Haalcentraal Bewoning                  | 5011  | haalcentraal      |
| Configuration Panel Router             | 3001  | config            |
| Configuration Panel Backend            | 8090  | config            |
| Configuration Panel Frontend           | 8091  | config            |
| Configuration Panel Database (postgres)| -     | config            |
| ClamAV virus scan                      | 3310  | clamav            |

> **NB!** `RUN_MODE` selects exactly one app-run mode, so `local` and `remote` can never bind the
> app ports 8080/3000 at the same time. ClamAV is optional: start it with `--profile clamav` and set
> `NLPORTAL_CONFIG_VIRUSSCAN_CLAMAV_ENABLED=true` in `imports/backend.env`.

### Running the application
This stack can run the whole NL Portal product (backend + frontend) together with its
supporting services. NL Portal is now a single monorepo, so the images are named
`nl-portal-backend` and `nl-portal-frontend` (the old `nl-portal-app-*` names are retired).
The `RUN_MODE` variable selects how the app runs (`sources` default / `local` / `remote`); the
supporting-service profiles (`zgw`, `config`, ...) are chosen independently:

#### Local (build from source)
`RUN_MODE=local` builds the backend and frontend images straight from this monorepo's `../backend`
and `../frontend` sources (the frontend builds the app together with its workspace libraries) and
runs them containerized. Use this to verify a change in its shipped form:
```shell
RUN_MODE=local docker compose --profile zgw --profile haalcentraal up -d --build
```

#### Remote (published images)
`RUN_MODE=remote` pulls the released monorepo images `ghcr.io/nl-portal/nl-portal-backend:4.0` and
`ghcr.io/nl-portal/nl-portal-frontend:4.0` instead of building — useful for evaluating
the product without a local build:
```shell
RUN_MODE=remote docker compose --profile zgw --profile haalcentraal up -d
```
> Note: `4.0` (the current minor line) can be replaced with any available tag from GHCR
> (`ghcr.io/nl-portal/nl-portal-backend` and `ghcr.io/nl-portal/nl-portal-frontend`),
> including snapshot tags, provided the `imports/` fixtures and configuration in this
> stack are compatible with the chosen tag.

#### From sources (active development, fast inner loop)
Run only the supporting services in compose and run the backend and frontend yourself for fast
reload. `sources` is the default `RUN_MODE`, so the stack does **not** publish the app ports
(`8080`/`8000`/`3000`), leaving them free for your locally-run app — no `RUN_MODE` needs to be set:
```shell
# supporting services only (app ports left free)
docker compose --profile zgw up -d

# backend (from ../backend) — see backend/app/README.md for .env.properties setup
cd ../backend && ./gradlew :app:bootRun

# frontend (from ../frontend) — libraries in watch + the app's vite dev server on :3000
cd ../frontend && pnpm install && pnpm dev
```
Backend GraphQL is at http://localhost:8080/graphql, frontend at http://localhost:3000. `pnpm dev`
builds the `@nl-portal/*` libraries in watch mode and serves the app; changing a library rebuilds it
and the app hot-reloads. Full config details:
[`backend/app/README.md`](../backend/app/README.md#local-development-bootrun) and
[`frontend/README.md`](../frontend/README.md).

> **Setting `RUN_MODE` on Windows.** This mode is the default, so it needs no `RUN_MODE` on Windows.
> It is the `local`/`remote` modes that do, and `RUN_MODE=remote docker compose ...` inline is
> POSIX-shell syntax that does **not** work in PowerShell or CMD (the variable is silently ignored and
> the stack falls back to the `sources` default, so no app is started). Set it separately instead:
> - **PowerShell:** `$env:RUN_MODE="remote"; docker compose --profile zgw up -d`
> - **CMD:** `set "RUN_MODE=remote" && docker compose --profile zgw up -d`
> - **Any shell:** add `RUN_MODE=remote` to a `.env` file in this directory (Compose reads it
>   automatically). This persists across runs, so remove it to go back to the `sources` default.

### Starting up supporting services only
To run only the supporting services (e.g. when running the application from your IDE),
leave `RUN_MODE` at its default (`sources`), which starts no app container. Two options:
- Including all ZGW related services, like Open Zaak, Open Klant, Objects API and Objecttypes API
- Keycloak and database only

#### Including all ZGW related services
Execute the following command:
```shell
docker compose --profile zgw up -d
```

The following services will be started:

| Service   |      Mapped port      |
|----------|:-------------:|
| NL Portal database (postgres) |  5432         |
| Keycloak |  8082         |
| Keycloak database (postgres) |    -   |
| Open Zaak | 8001 |
| Open Zaak database (postgis) | - |
| Objecten API | 8010 |
| Objecten API database (postgis) | - |
| Objecttypen API | 8011 |
| Objecttypen API database (postgres) | - |
| Open Notificaties | 8012 |
| Open Notificaties database (postgres) | - |
| OpenKlant 2 | 8014 |
| OpenKlant 2 database (postgres) | - |
| OpenProduct | 8015 |
| OpenProduct database (postgres) | - |
| Redis | - |

#### Keycloak and database only
Execute the following command: 
```shell
docker compose up -d
```

| Service   |      Mapped port      |
|----------|:-------------:|
| NL Portal database (postgres) |  5432         |
| Keycloak |  8082         |
| Keycloak database (postgres) |    -   |

## Maintenance

### Updating Openzaak data

There is a script `./imports/open-zaak/resync/resync.sh` that will automatically create an import script for several tables of 
OpenZaak. It requires that you install the postgres CLI tool `pg_dump` found in the postgres CLI tools. 

You should only run this script if you have recently upgraded OpenZaak or added new data in OpenZaak you want to make available 
after clearing the database.

## Known issues

* Running on Windows  
If the services within this compose file fail to start up with the following error or similar:  
    ```log
    2025-11-18 11:15:41.901 | /app/init/init.sh: 2: 
    2025-11-18 11:15:41.902 | Apply database migrations
    2025-11-18 11:15:59.325 | Unknown command: 'migrate\r'. Did you mean migrate?
    2025-11-18 11:15:59.325 | Type 'manage.py help' for usage.
    2025-11-18 11:16:04.229 | Traceback (most recent call last):
    2025-11-18 11:16:04.229 |   File "/usr/local/lib/python3.12/site-packages/django/db/backends/base/base.py", line 279, in ensure_connection
    2025-11-18 11:16:04.235 |     self.connect()
    2025-11-18 11:16:04.235 |   File "/usr/local/lib/python3.12/site-packages/django/utils/asyncio.py", line 26, in inner
    2025-11-18 11:16:04.237 |     return func(*args, **kwargs)
    2025-11-18 11:16:04.237 |            ^^^^^^^^^^^^^^^^^^^^^
    2025-11-18 11:16:04.237 |   File "/usr/local/lib/python3.12/site-packages/django/db/backends/base/base.py", line 256, in connect
    2025-11-18 11:16:04.237 |     self.connection = self.get_new_connection(conn_params)
    2025-11-18 11:16:04.237 |                       
    ```
  Then that means either your editor or git is replacing linux line endings with windows compatible crlf line endings.
  To work with this repository you need to make sure that doesn't happen:
  * Git: https://docs.github.com/en/get-started/git-basics/configuring-git-to-handle-line-endings?platform=windows#about-line-endings  
    * Make sure that the `core.autocrlf` property in git is set to `false` and then pull the project again.