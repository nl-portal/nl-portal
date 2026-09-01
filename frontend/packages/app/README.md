# nl-portal-app

Part of the [NL Portal](https://github.com/nl-portal/nl-portal) frontend libraries, a collection of
packages providing a configurable portal implementation for municipalities.

This package is the reference portal implementation. It wires the `@nl-portal/*` libraries together
and is where configuration such as routing, theming, and backend endpoints takes place.

It is not published to npm. It is built and released as a container image,
`ghcr.io/nl-portal/nl-portal-app-frontend`, from the [frontend Dockerfile](../../Dockerfile).

## Running from sources

From the [frontend](../..) root, after `pnpm install`:

```bash
pnpm app:dev
```

This starts this package together with the `@nl-portal/*` libraries it depends on, so changes in the
libraries are picked up while running. Use `pnpm app:build` for a production build and
`pnpm app:preview` to serve it.

Note that `pnpm dev` starts the development app (`nl-portal-app-dev`) instead of this one.

See the [frontend README](../../README.md) for the full development setup.

## Documentation

See [nl-portal.nl](https://nl-portal.nl) for setup, configuration, and architecture.

## License

[EUPL-1.2](https://github.com/nl-portal/nl-portal/blob/main/frontend/LICENSE.MD)
