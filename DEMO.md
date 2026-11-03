# Template pages demo

> **Disclaimer:** this is an MVP candidate for the hackathon, not a final implementation. It should be
> improved on, especially the template contract (the tags, their arguments and the pipe-row body
> syntax, and the `PortaalPaginaTemplate` document).

A local demo of template pages (nl-portal-issues#170): pages configured in the backend and rendered by
the standard frontend with LiquidJS, without a frontend rebuild. See
[documentation/features/pages.md](./documentation/features/pages.md) for the feature itself.

## What it shows

| Page                   | Type    | Template source            | Data                                                   |
|------------------------|---------|----------------------------|--------------------------------------------------------|
| `/over-ons`            | static  | file                       | –; alert, image and link list                          |
| `/contact`             | static  | Objects API object         | –                                                      |
| `/erfpacht`            | dynamic | file                       | OpenProduct list with status tabs and paging           |
| `/erfpacht/<id>`       | dynamic | binding (content element)  | one OpenProduct product                                |
| `/parkeervergunningen` | dynamic | file                       | OpenProduct parking permits; rows open the detail page |
| `/vergunningen`        | dynamic | file                       | IKO profiles `personen` and `vergunningen` (burger)    |

The pages and menu entries are in `backend/app-dev/src/main/resources/config/application.yml` and
`application-demo.yml`, the template files in `backend/app-dev/src/main/resources/pages/`.

## Running it

Requirements: Docker with the compose plugin, Java 21, pnpm and lsof. Ports 8080 and 3000 must be free.

```shell
./run-demo.sh          # start everything, or: ./run-demo.sh up
./run-demo.sh logs backend|frontend|<container>
./run-demo.sh ps
./run-demo.sh down
```

`up` starts the supporting services with `RUN_MODE=demo`, then the app-dev backend with the Spring
profile `demo` and the app-dev frontend, both from sources in the background. It waits until they
answer and prints the URLs. The app logs go to `$TMPDIR/nl-portal-demo` (override with `LOG_DIR`).

The same by hand:

```shell
cd docker-compose && RUN_MODE=demo docker compose --profile zgw --profile haalcentraal up -d
cd backend && ./gradlew :app-dev:bootRun --args='--spring.profiles.active=demo'
cd frontend && pnpm install && pnpm dev
```

Open http://localhost:3000 and log in with DigiD as `burger`/`burger` (BSN 999993847) or with
eHerkenning as `bedrijf`/`bedrijf` (KVK 14127293). `/vergunningen` is only in the burger menu.

## Services

`RUN_MODE=demo` is the `sources` mode (no app containers, app ports free) plus:

| Service                                | Port |
|----------------------------------------|------|
| WireMock (IKO page sources, index API) | 8083 |
| IKO                                    | 8084 |
| IKO Database (postgres)                | 8085 |
| IKO Redis                              | 8086 |

The demo data is in `docker-compose/imports/demo/`. The `iko` Keycloak realm
(`docker-compose/imports/keycloak/iko-realm.json`) is imported in every mode, but only used here.

The IKO seed (`imports/demo/iko/*.sql`) only runs on a fresh `nl-portal-iko-database-data` volume.
To reseed, run `./run-demo.sh down` and remove that volume.

## Limitations

* Use the app-dev frontend (`pnpm dev`). The menu titles and breadcrumbs (`menu-entries.<name>`) are
  only in app-dev's messages, so `pnpm app:dev` shows the message ids instead.
* After a language switch, a template from a binding (the OpenProduct content element on
  `/erfpacht/<id>`) stays in the old language until the next navigation.
* The WireMock `vergunningen` source ignores paging and filters: the IKO request carries them, but the
  list doesn't get shorter.
