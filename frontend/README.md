# NL Portal Frontend

![Node 24](https://img.shields.io/badge/Node-24-green)

The frontend stack of the [NL Portal monorepo](../README.md). It is a single
[pnpm workspace](https://pnpm.io/workspaces) of React packages that together provide a configurable
portal implementation for municipalities, built on components that follow the
[NL Design System](https://designsystem.gebruikercentraal.nl/). Look and feel is customized through
design tokens; the back-end systems it talks to are configured at runtime.

## Packages

Reusable libraries, published as `@nl-portal/*`:

- `api` - GraphQL client + generated hooks (Apollo Client).
- `authentication` - OIDC authentication.
- `localization` - translations and i18n.
- `user-interface` - the portal UI components and pages.

And the shippable application:

- `app` (`nl-portal-app`) - the runnable portal that consumes the libraries via `workspace:*` and is
  packaged as the `nl-portal-frontend` image (Dockerfile at [`frontend/Dockerfile`](./Dockerfile)).

## Requirements

- Node 24
- pnpm 11 (`corepack enable` or see https://pnpm.io/installation)

## Getting started

From `frontend/`:

```shell
pnpm install   # install all workspace dependencies
pnpm dev       # build the libraries in watch mode and serve the app on :3000
```

`pnpm dev` runs every package's dev script in parallel: the libraries build in watch mode (via
`vite build --watch`) and the app (`nl-portal-app`) waits for them, then starts its vite dev server.
For a one-off build of everything instead, use `pnpm build`.

## Quality checks

Run from `frontend/`:

```shell
pnpm test          # vitest
pnpm lint:css      # stylelint
pnpm prettier      # formatting check (pnpm prettier:fix to apply)
```

`pnpm prettier` is the quality gate. A husky pre-commit hook runs `lint-staged` over the staged files
(prettier, stylelint and a `tsc --noEmit` type-check) and `commitlint` over the commit message, so
these checks run automatically before a commit lands. Run `pnpm prettier:fix` to auto-format.

## GraphQL codegen

Only `packages/api` runs codegen (`pnpm codegen`), generating typed hooks from the query files. The
generated output is committed, so consumers reuse it without regenerating. Regeneration requires the
GraphQL endpoint (the backend) to be reachable.

## Configuration

The app reads its runtime configuration from `window.*` globals, set by
`packages/app/public/config.js` (local development defaults) and, in a container,
`config.template.js` (populated from environment variables at startup). These values flow into the
UI through the `user-interface` app context.

## More information

See the [monorepo README](../README.md) and the [documentation](https://nl-portal.nl).
