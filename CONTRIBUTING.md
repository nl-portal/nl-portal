# Contributing to NL Portal

Thank you for your interest in contributing to NL Portal. This repository is a monorepo containing
the backend libraries + application, the frontend libraries + application, the local development
stack and the documentation. Backend and frontend release at a single, shared version.

When contributing, please first discuss the change you wish to make with the maintainers via a
[Request For Change](https://github.com/nl-portal/nl-portal-issues) (larger features) or an
[issue](https://github.com/nl-portal/nl-portal/issues) (smaller tasks and bugs).

## Ownership

This project is owned by [Ritense](https://ritense.com/) and developed together with its community.

## How to contribute

1. Open an RFC (bigger features) or an issue (smaller tasks/bugs) and let it be discussed.
2. Create a branch off `main` (see [Branch model](#branch-model)).
3. Write the code, with tests, following the existing style of the surrounding code.
4. Open a pull request against the relevant branch.
5. A maintainer reviews; once approved and green, it is squash-merged.

## Branch model

NL Portal follows a Spring-style branch model:

- **`main`** - the development trunk and default branch. All new work targets `main`.
- **`release/<major>.x`** - a long-lived maintenance line per major version (for example
  `release/3.x`, `release/4.x`). Minor and patch releases are **tags** on these branches. At most
  two lines are kept alive at once: the current major and the previous one.
- **`rc/*`** - ephemeral release-candidate branches cut from `main` when preparing a release.

Contributor branches are cut from `main`:

- **`feature/*`** - new functionality (optionally split into short-lived `story/*` branches that
  merge back into the feature branch).
- **`bugfix/*`** - bug fixes targeting `main`.
- **`hotfix/*`** - urgent patches for a `release/<major>.x` line that cannot wait for the next
  version. A hotfix is authored on the maintenance line and then propagated as described in
  [Fix propagation](#fix-propagation).

### Fix propagation

A fix must reach every supported line it affects. How it travels depends on the major:

- **Current major** (the major `main` is still developing): author the fix on its
  `release/<major>.x` line, then **merge that line up into `main`**. The merge preserves the fix
  commit, which keeps the release line fast-forwardable at the next release. Maintainers perform
  this merge.
- **Previous major** (the older maintenance line): the fix stays on that line. If the same bug also
  affects the current major, it is **cherry-picked and adapted** onto the current line (and then
  follows the rule above). The two maintenance lines are otherwise independent.
- The one merge that is **not** allowed is merging a previous-major line into `main` - their
  histories have diverged too far, so such fixes are re-applied, not merged.

Security fixes must reference the relevant vulnerability identifier **in the fix commit on each
line** they apply to - that reference is how a vulnerability is confirmed patched everywhere it
matters.

## Commit messages

This repository uses [Conventional Commits](https://www.conventionalcommits.org/). Format:

```
<type>(<optional scope>): <description>
```

Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`. Example:

```
fix(zgw): handle empty zaak result in case overview
```

Because pull requests are **squash-merged**, the PR title is what lands on the branch - make it a
valid Conventional Commit.

## Building and testing locally

- **Backend**: from `backend/`, `./gradlew build` (integration tests auto-provision a database via
  the docker-compose plugin; Docker must be running).
- **Frontend**: from `frontend/`, `pnpm install` then `pnpm -r build`, `pnpm test`, `pnpm lint` and
  `pnpm prettier`.

See `backend/README.md`, `frontend/README.md` and `docker-compose/README.md` for details.

## Code of Conduct

By participating you agree to abide by our [Code of Conduct](./CODE_OF_CONDUCT.md).

## Security

Please report security issues privately - see the [Security Policy](./SECURITY.md). Do not open a
public issue for vulnerabilities.
