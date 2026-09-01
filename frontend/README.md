# NL Portal Frontend

![3.x maintenance](https://img.shields.io/badge/3.x-maintenance-orange)
![Node 22](https://img.shields.io/badge/Node-22-green)

> **This is the 3.x maintenance line.** Active development happens on the 4.x line (`main`), where
> this documentation is kept up to date. The 3.x line still receives fixes and selected minor
> releases, but new functionality is generally added to 4.x first. Check which line you are on before
> following instructions from either branch.

The frontend stack of the NL Portal. It is a single [pnpm workspace](https://pnpm.io/workspaces) of
React packages that together provide a configurable portal implementation for municipalities, built
on components that follow the [NL Design System](https://designsystem.gebruikercentraal.nl/). Look
and feel is customized through design tokens; the back-end systems it talks to are configured at
runtime.

## Packages

Reusable libraries, published as `@nl-portal/*`:

- `api` - GraphQL client + generated hooks (Apollo Client).
- `authentication` - OIDC authentication.
- `localization` - translations and i18n.
- `user-interface` - the portal UI components and pages.

And the two application packages:

- `app` (`nl-portal-app`) - the shippable portal that consumes the libraries via `workspace:*` and is
  packaged as a container image from [`frontend/Dockerfile`](./Dockerfile). Not published to npm.
- `app-dev` (`nl-portal-app-dev`) - a developer-only sandbox carrying work-in-progress demos, never
  published. See [packages/app-dev](./packages/app-dev/README.md).

## Requirements

- Node 22
- pnpm 11 (`corepack enable` or see https://pnpm.io/installation)

## Getting started

From `frontend/`:

```shell
pnpm install   # install all workspace dependencies
pnpm dev       # build the libraries in watch mode and serve the DEV app on :3000
```

The root scripts default to the dev app; add the `app:` prefix for the shippable app:

| Dev app (default) | Shippable app      |
| ----------------- | ------------------ |
| `pnpm dev`        | `pnpm app:dev`     |
| `pnpm build`      | `pnpm app:build`   |
| `pnpm preview`    | `pnpm app:preview` |

Each `dev` script runs its package's dependencies' dev scripts in parallel: the libraries build in
watch mode (via `vite build --watch`) and the app waits for them, then starts its vite dev server.
For a one-off build of every package instead, use `pnpm -r build` (this is what CI runs).

## Quality checks

Run from `frontend/`:

```shell
pnpm test          # vitest
pnpm typecheck     # tsc --noEmit
pnpm lint:css      # stylelint
pnpm prettier      # formatting check (pnpm prettier:fix to apply)
```

`pnpm prettier` is the quality gate. Husky hooks run `lint-staged` over staged files on commit and
`commitlint` over the commit message; the pre-push hook runs `pnpm lint` (which includes the
copyright header check) and, note, the backend's `./gradlew spotlessCheck` as well.

## GraphQL codegen

Only `packages/api` runs codegen (`pnpm codegen`), generating typed hooks from the query files in
[packages/api/src/queries](./packages/api/src/queries). The generated output is committed, so
consumers reuse it without regenerating. Regeneration requires the GraphQL endpoint (the backend) to
be reachable, as configured in [codegen.yml](./packages/api/codegen.yml).

Once generated, queries are exported as hooks:

```ts
import { useGetZakenQuery } from "@nl-portal/nl-portal-api";

const CasesPage = () => {
  const { data, loading, error, refetch } = useGetZakenQuery();
  // ...
};
```

## Configuration

On this line, runtime configuration is carried entirely by `window.*` globals: the OIDC and API URLs
plus the feature and theming toggles. The full set is declared in [window.d.ts](./window.d.ts).

Values are set by [packages/app/public/config.js](./packages/app/public/config.js) for local
development. In a container, `config.template.js` is populated from environment variables at startup
via nginx `envsubst`, so any of them can be overridden per environment:

```shell
docker run -e OIDC_URL=... -e OIDC_REALM=... -e OIDC_CLIENT_ID=... -e GRAPHQL_URI=... -p 3000:3000 <image>
```

Authentication methods are configured in the app itself, mapping the `middel` claim from the JWT to a
login type:

```ts
const authenticationMethods = {
  person: ["digid", "machtigen"],
  company: ["eherkenning", "bewindvoering"],
  proxy: ["machtigen", "bewindvoering"],
};
```

See [packages/app/src/App.tsx](./packages/app/src/App.tsx) for how these are passed to
`OidcProvider`.

## Published packages

The four libraries publish to npm as `@nl-portal/*`:

```shell
pnpm add @nl-portal/nl-portal-user-interface
```

Release candidates go out under the `rc` dist-tag. A release only takes `latest` when its version is
the newest across all lines; otherwise it gets the major-specific tag. Once 4.x is the newest
release, install from this line explicitly:

```shell
pnpm add @nl-portal/nl-portal-user-interface@v3
```

## More information

See the [documentation](https://nl-portal.nl).
