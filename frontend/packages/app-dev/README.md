# nl-portal-app-dev

Part of the [NL Portal](https://github.com/nl-portal/nl-portal) frontend libraries, a collection of
packages providing a configurable portal implementation for municipalities.

This package is a developer-only portal implementation, used to develop and demo the `@nl-portal/*`
libraries from sources. It is private and never published to npm. Use
[nl-portal-app](../app) as the reference implementation.

## Running from sources

Install dependencies from the [frontend](../..) root first, then start this package:

```bash
pnpm dev
```

The `dev` and `build` scripts wait for the library `dist` output, so the libraries must be building
or built before this app starts.

See the [frontend README](../../README.md) for the full development setup.

## Documentation

See [nl-portal.nl](https://nl-portal.nl) for setup, configuration, and architecture.

## License

[EUPL-1.2](https://github.com/nl-portal/nl-portal/blob/main/frontend/LICENSE.MD)
