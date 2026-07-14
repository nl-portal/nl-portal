# Getting started

NL Portal can be run three ways, each for a different purpose. All of them use the
[`docker-compose/`](./docker-compose/README.md) stack for the supporting services (Keycloak,
databases, ZGW). Pick the mode that matches what you are doing; each links to the fuller docs.

## 1. Remote — evaluate the released product

Pull the published images and run them; no build. For demos and evaluation.

```shell
cd docker-compose
RUN_MODE=remote docker compose --profile zgw --profile haalcentraal up -d
```

The portal is at http://localhost:3000. See
[docker-compose/README.md](./docker-compose/README.md#remote-published-images) for profiles, image
tags and test-user credentials.

## 2. Local — run your changes as they'd ship

Build the backend and frontend images from this monorepo and run them containerized. For verifying
a change end-to-end in its shipped form: the image build, runtime configuration, and customization
such as theme, logo or feature toggles.

```shell
cd docker-compose
RUN_MODE=local docker compose --profile zgw --profile haalcentraal up -d --build
```

See [docker-compose/README.md](./docker-compose/README.md#local-build-from-source).

## 3. From sources — active development (fast inner loop)

Run only the supporting services in compose, and run the backend and frontend yourself for fast
reload. `sources` is the default `RUN_MODE`, so the supporting stack leaves the app ports
(`8080`/`8000`/`3000`) free for your locally-run app; no `RUN_MODE` needs to be set.

```shell
# supporting services only (app ports left free)
cd docker-compose
docker compose --profile zgw up -d

# backend (from backend/)
./gradlew :app:bootRun

# frontend (from frontend/) — libraries in watch + the app's vite dev server on :3000
pnpm install   # first time only
pnpm dev
```

Backend GraphQL is at http://localhost:8080/graphql, frontend at http://localhost:3000. For the
full workflow, `bootRun` configuration (`.env.properties`), GraphQL codegen and the Windows
`RUN_MODE` caveats see:

- [docker-compose/README.md](./docker-compose/README.md#from-sources-active-development-fast-inner-loop)
  — sources default and the Windows `RUN_MODE` caveat
- [backend/app/README.md](./backend/app/README.md#local-development-bootrun) — `bootRun` config
- [frontend/README.md](./frontend/README.md) — `pnpm dev` and GraphQL codegen

## More detail

- Backend build, tests, layout: [backend/README.md](./backend/README.md)
- Frontend workspace, scripts: [frontend/README.md](./frontend/README.md)
- Compose profiles, ports, fixtures: [docker-compose/README.md](./docker-compose/README.md)
- Contributing, branch model, commits: [CONTRIBUTING.md](./CONTRIBUTING.md)
