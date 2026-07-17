# NL-Portal 4.0.0

NL-Portal is samengevoegd tot een monorepo met een enkele, gedeelde versie voor de libraries en de app. Deze release-notes bundelen alle wijzigingen sinds 3.0.x.

## Nieuwe Functionaliteit

* **Monorepo met uniforme versionering.** Backend-libraries, frontend-libraries en de app zijn samengevoegd in één repository (`nl-portal/nl-portal`) met een gedeeld versienummer. App-images worden vanuit de monorepo gepubliceerd op hetzelfde versienummer als de libraries. De docker-compose-stack schakelt met `RUN_MODE` tussen `remote`, `local` en `sources`; zie `GETTING_STARTED.md`.
* **Frontend-features vanuit de backend.** De frontend haalt feature-toggles en -eigenschappen op via `GET /api/public/features` (`nl-portal.config.features.*`, env `NLPORTAL_CONFIG_FEATURES_*`) in plaats van via `window.*`. Hiermee zijn onder andere de onderhoudsmelding op de overzichtspagina, de contactmomenten op de zaakdetailpagina en het aantal zaken in het overzichtsvoorbeeld configureerbaar. Zie de [Deployment guide](../../../configuratie/deployment-guide.md#frontend-features).
* **Uniforme documentafhandeling.** Besluiten-, openproduct- en productdocumenten worden nu op dezelfde manier getoond en gedownload als zaak- en berichtdocumenten. Zie [Document filtering](../../../features/document-filtering/document-filtering.md).
* Den Haag-componenten bijgewerkt naar de laatste versie.

## Bugfixes

* De applicatiebeveiliging is verbeterd.

## Breaking changes

**Voor iedereen (ook gebruikers van de Docker-images):**

* **Images verhuisd naar de monorepo.** De backend- en frontend-images worden nu vanuit `nl-portal/nl-portal` gepubliceerd op een gedeeld versienummer (`ghcr.io/nl-portal/nl-portal-backend` / `-frontend`). Wijs je deployment naar de nieuwe images. Zie migratie-instructies.
* **Frontend feature-configuratie via `window.*` is verwijderd** (`config.js` / `config.template.js`). `window.*` bevat nu alleen nog OIDC- en API-URL-configuratie; alle feature-toggles en -eigenschappen worden nu op de backend gezet. Zie migratie-instructies.

**Alleen voor broncode-ontwikkelaars en maatwerkimplementaties:**

* **Upgrade naar Spring Boot 4.1.0** (Spring Framework 7 / Spring Security 7), Kotlin 2.4.10. Bouwen en uitbreiden vereist Java 21; controleer eigen code, configuratie en dependencies op Spring Boot 4- / Spring Security 7-wijzigingen.
* **Frontend-platform bijgewerkt**: React 19, Vite 8, TypeScript 5.9. Bouwen vereist Node 24 en pnpm 11.

## Deprecations

Er zijn geen deprecations.

## Migratie-instructies

**Frontend-features naar de backend.** Verplaats de voorheen via `window.*` gezette features naar `nl-portal.config.features.*` (of de env-vorm `NLPORTAL_CONFIG_FEATURES_*` met relaxed binding). Mapping oude `window.*` var → nieuwe backend-property:

| Oude `window.*` var | Nieuwe backend-property |
|---|---|
| `USE_THEME_API` | `toggles.theme-api-enabled` |
| `MESSAGE_COUNT_ENABLE` | `toggles.message-count-enabled` |
| `MESSAGE_COUNT_POLLING_INTERVAL` | `properties.message-count-polling-interval` |
| `CASES_PARTIAL_SEARCH` | `toggles.cases-partial-search-enabled` |
| `SHOW_INHABITANT_AMOUNT` | `toggles.my-inhabitant-count-enabled` |
| `OPEN_PRODUCTEN` | `toggles.open-product-enabled` |
| `SHOW_CASE_RESULT_EXPLANATION` | `toggles.cases-result-explanation-enabled` |
| `USE_LEGACY_OGONE_PAYMENT` | `toggles.legacy-payment-enabled` |
| `OVERVIEW_INTRO_ENABLED` | `toggles.overview-intro-enabled` |
| `OVERVIEW_MAINTENANCE_ALERT_ENABLED` | `toggles.overview-maintenance-alert-enabled` |
| `OVERVIEW_MAINTENANCE_ALERT_TITLE_NL/EN` | `properties.overview-maintenance-alert-title-nl/en` |
| `OVERVIEW_MAINTENANCE_ALERT_TEXT_NL/EN` | `properties.overview-maintenance-alert-text-nl/en` |
| `OVERVIEW_CURRENT_CASES_PREVIEW_LENGTH` | `properties.overview-current-cases-preview-length` |
| `THEME_CLASS` | `properties.theme-class` |
| BRP/adres-URL's (`ADDRESS_RESEARCH_URL`, etc.) | `properties.my-address-research-url`, etc. |

Verwijderde variabelen zonder vervanging (werden niet meer gebruikt): `OPEN_KLANT_VERSION`, `ACCOUNT_SHOW_NOTIFICATION_SUBSECTION`, `OVERVIEW_SHOW_CURRENT_CASES_PREVIEW`.

**Images bijwerken.** De `-app`-infix in de imagenaam vervalt. Werk de image-referenties in je deployment (docker-compose, Helm-values, Kubernetes-manifests) bij:

| Oud | Nieuw |
|---|---|
| `ghcr.io/nl-portal/nl-portal-app-backend:3.x.x` | `ghcr.io/nl-portal/nl-portal-backend:4.0.0` |
| `ghcr.io/nl-portal/nl-portal-app-frontend:3.x.x` | `ghcr.io/nl-portal/nl-portal-frontend:4.0.0` |

Tags zijn beschikbaar per volledige versie (`4.0.0`), minor (`4.0`) en major (`4`).

**Broncode / maatwerk.** Bouw en draai met Java 21; bouw de frontend met Node 24 en pnpm 11. Controleer eigen uitbreidingen op Spring Boot 4- / Spring Security 7-wijzigingen.

## Bekende problemen

Er zijn geen bekende problemen.
