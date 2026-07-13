# Getting started

NL Portal can be run three ways, each for a different purpose. All of them use the
[`docker-compose/`](./docker-compose/README.md) stack for the supporting services (Keycloak,
databases, ZGW). Pick the mode that matches what you are doing.

## 1. Remote — evaluate the released product

Pull the published images and run them; no build. For demos and evaluation.

```shell
cd docker-compose
docker compose --profile remote --profile zgw up -d
```

The portal is at http://localhost:3000. See [docker-compose/README.md](./docker-compose/README.md)
for profiles, image tags and test-user credentials.

## 2. Local — run your changes as they'd ship

Build the backend and frontend images from this monorepo and run them containerized. For verifying
a change end-to-end in its shipped form: the image build, runtime configuration, and customization
such as theme, logo or feature toggles.

```shell
cd docker-compose
docker compose --profile local --profile zgw up -d --build
```

## 3. From sources — active development (fast inner loop)

Run only the supporting services in compose, and run the backend and frontend yourself for fast
reload. Set `RUN_MODE=sources`<sup>[1](#windows-caveats)</sup> so the stack does **not** publish the
app ports (`8080`/`8000`/`3000`), leaving them free for your locally-run app:

```shell
# supporting services only (app ports left free)
cd docker-compose
RUN_MODE=sources docker compose --profile zgw up -d

# backend (from backend/)
./gradlew :app:bootRun

# frontend (from frontend/) — libraries in watch + the app's vite dev server on :3000
pnpm install   # first time only
pnpm dev
```

Backend GraphQL is at http://localhost:8080/graphql, frontend at http://localhost:3000. `pnpm dev`
builds the `@nl-portal/*` libraries in watch mode and serves the app; changing a library rebuilds it
and the app hot-reloads.

### How `bootRun` differs from the containerized runs

Modes 1 and 2 run the backend as a **container** using the production-shaped configuration
(`backend/app/src/main/resources/config/application.yml`, fully env-driven) fed by
`docker-compose/imports/backend.env` — exactly what ships.

`bootRun` runs that **same** base config — no separate "dev" Spring profile to drift from
production — but sources its environment from an app-owned `backend/app/.env.properties`
(gitignored; copy it from `.env.properties.example`), independent of the containerised
`backend.env`. That file supplies the dev-only differences: `LOGLEVEL=DEBUG`, GraphQL
introspection enabled (for codegen, see below), and `localhost` URLs for services the
container reaches via `host.docker.internal`. See
[`backend/app/README.md`](backend/app/README.md#local-development-bootrun) for the details.

So local development dogfoods the production configuration and differs only by environment
variables.

## GraphQL codegen

Regenerating the frontend GraphQL types introspects a running backend at
http://localhost:8080/graphql:

```shell
cd frontend
pnpm codegen
```

Introspection is disabled in the shipped configuration and enabled only by the `bootRun` overlay, so
run codegen against a `bootRun` backend (mode 3). Container images never expose introspection.

## Windows caveats

**<sup>1</sup> Setting `RUN_MODE` on Windows.** The `RUN_MODE=sources docker compose ...` form is
POSIX-shell syntax and does **not** work in PowerShell or CMD — the variable is silently ignored, the
stack falls back to `full`, and the app ports get published (defeating sources mode). Set it
separately instead:

- **PowerShell:** `$env:RUN_MODE="sources"; docker compose --profile zgw up -d`
- **CMD:** `set "RUN_MODE=sources" && docker compose --profile zgw up -d`
- **Any shell:** add `RUN_MODE=sources` to a `.env` file in `docker-compose/` (Compose reads it
  automatically). This persists across runs, so remove it to go back to `full`.

**Line endings.** The `docker-compose/imports/**` shell scripts must keep LF line endings; if git or
your editor rewrites them to CRLF the containers fail to run them. See
[docker-compose/README.md](./docker-compose/README.md) for the fix.

## More detail

- Backend build, tests, layout: [backend/README.md](./backend/README.md)
- Frontend workspace, scripts: [frontend/README.md](./frontend/README.md)
- Compose profiles, ports, fixtures: [docker-compose/README.md](./docker-compose/README.md)
- Contributing, branch model, commits: [CONTRIBUTING.md](./CONTRIBUTING.md)
