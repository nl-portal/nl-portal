# NL-Portal 4.0.0

NL-Portal is samengevoegd tot een monorepo met een enkele, gedeelde versie voor de libraries en de app. Deze release-notes bundelen alle wijzigingen sinds 3.0.x.

## Nieuwe Functionaliteit

* **Monorepo met uniforme versionering.** Backend-libraries, frontend-libraries en de app zijn samengevoegd in één repository (`nl-portal/nl-portal`) met een gedeeld versienummer. App-images worden vanuit de monorepo gepubliceerd op hetzelfde versienummer als de libraries. De docker-compose-stack schakelt met `RUN_MODE` tussen `remote`, `local` en `sources`; zie `GETTING_STARTED.md`.
* **Frontend-features vanuit de backend.** De frontend haalt feature-toggles en -eigenschappen op via `GET /api/public/frontend` (`nl-portal.frontend.features.*`, env `NLPORTAL_FRONTED_FEATURES_*`) in plaats van via `window.*`. Hiermee zijn onder andere de onderhoudsmelding op de overzichtspagina, de contactmomenten op de zaakdetailpagina en het aantal zaken in het overzichtsvoorbeeld configureerbaar. Zie de [Deployment guide](../../../configuratie/deployment-guide.md#frontend-features).
* **Snelle links in de mobiele weergave.** Frontend-`MenuItems` kunnen nu ook worden getoond in de nieuwe sectie **Quick Links** op de overzichtspagina. Zo kunnen gebruikers in de mobiele weergave sneller naar belangrijke onderdelen navigeren.
* **Uniforme documentafhandeling.** Besluiten-, openproduct- en productdocumenten worden nu op dezelfde manier getoond en gedownload als zaak- en berichtdocumenten. Zie [Document filtering](../../../features/document-filtering/document-filtering.md).
* Den Haag-componenten bijgewerkt naar de laatste versie.
* Er is een optie toegevoegd om het zoeken naar klantcontact(en) te beperken op basis van de referentie van het gekoppelde object. Hiermee kunnen implementaties het zoeken naar contacten beperken tot contacten die zijn gekoppeld aan een OpenZaak-zaak, een OpenProduct-product of beide.
* Voor monitoring tools is er een ping endpoint beschikbaar gekomen `GET /api/public/ping`, response is platte tekst. Dit endpoint is standaard uit gezet. Via property kan deze aangezet worden via `nl-portal.support.ping-endpoint.enabled=true` of met de environment variabele `NLPORTAL_SUPPORT_PINGENDPOINT_ENABLED=true`.
* Samenvoeging van Objecttype API en Objecten API in Open Object in docker compose en fixtures samengevoegd
* **Ondersteuning voor Keycloak standard token exchange (v2).** De variant kies je met `nl-portal.authentication.keycloak.token-exchange-version` (`v1` of `v2`), of via de env-variabele `KEYCLOAK_TOKEN_EXCHANGE_VERSION`. `v1` blijft de standaardwaarde, dus bestaande omgevingen wijzigen niet van gedrag. Met `v2` zijn de `KC_FEATURES` vlaggen, de fine-grained token-exchange permission en de aparte token-exchange client niet langer nodig; twee clients volstaan. De docker-compose demo-omgeving draait nu op v2. Zie [Standard token exchange (v2)](../../../configuratie/keycloak-token-exchange-v2.md). **Draaide je de demo-omgeving al eerder?** Keycloak importeert met strategie `IGNORE_EXISTING` en houdt dan het oude v1-realm vast, waardoor elke login mislukt; zie [Referentieconfiguratie](../../../configuratie/keycloak.md#referentieconfiguratie) voor hoe je dat herstelt.

## Bugfixes

* Diverse backend- en frontend-dependencies zijn bijgewerkt naar nieuwere versies.
* De toegestane status `in_aanvraag` van een product ging mis bij het serialiseren en deserialiseren. Dit is opgelost.
* Form.io-componenten in taakformulieren tonen het label en de beschrijving niet meer dubbel. Dit gold onder andere voor `datetime`, `phoneNumber`, `url`, `currency`, `time`, `address` en `radio`.
* De instelling `hideLabel` (Hide label) werkt nu voor tekstvelden, tekstvakken, datum/tijd, select, adres, radio en selectboxes. Het label wordt verborgen en het veld krijgt een `aria-label`, zodat het voor schermlezers benoemd blijft. Formulieren die `hideLabel` al aan hadden staan, tonen dit label vanaf deze versie dus niet meer.

## Breaking changes

**Voor iedereen (ook gebruikers van de Docker-images):**
* De enum `OpenProductFrequentie` is gewijzigd en ondersteunt nu ook een lege waarde.
* **Images verhuisd naar de monorepo.** De backend- en frontend-images worden nu vanuit `nl-portal/nl-portal` gepubliceerd op een gedeeld versienummer (`ghcr.io/nl-portal/nl-portal-backend` / `-frontend`). Wijs je deployment naar de nieuwe images. Zie migratie-instructies.
* **Frontend feature-configuratie via `window.*` is verwijderd** (`config.js` / `config.template.js`). `window.*` bevat nu alleen nog OIDC- en API-URL-configuratie; alle feature-toggles en -eigenschappen worden nu op de backend gezet. Zie migratie-instructies.
* **Documenten API: `rsin` en `document-type-url` zijn verplicht.** Elke configuratie onder `nl-portal.config.documentenapis.properties.configurations.<naam>` moet nu een `rsin` en een `document-type-url` hebben (env: `NLPORTAL_CONFIG_DOCUMENTENAPIS_PROPERTIES_CONFIGURATIONS_<NAAM>_RSIN` en `..._DOCUMENTTYPEURL`). `document-type-url` is het standaard informatieobjecttype (bijvoorbeeld "Bijlage") voor uploads waarbij het formulier geen specifiek type meegeeft, zodat elk document een type heeft. Ontbrak een van beide, dan gaf uploaden een nullpointer; nu weigert de Documenten API de upload. Zie migratie-instructies.

**Alleen voor broncode-ontwikkelaars en maatwerkimplementaties:**

* **Upgrade naar Spring Boot 4.1.0** (Spring Framework 7 / Spring Security 7), Kotlin 2.4.10. Bouwen en uitbreiden vereist Java 21; controleer eigen code, configuratie en dependencies op Spring Boot 4- / Spring Security 7-wijzigingen.
* **Frontend-platform bijgewerkt**: React 19, Vite 8, TypeScript 5.9. Bouwen vereist Node 24 en pnpm 11.
* **Den Haag design tokens bijgewerkt naar `@gemeente-denhaag/design-tokens` 1.6.0.** Deze versie hernoemt de kleurschalen en verwijdert een aantal tokens. De Den Haag-componenten die met de libraries meekomen gebruiken de nieuwe namen. Overschrijf je in je eigen thema Den Haag-tokens, of verwijs je ernaar (bijvoorbeeld in je `--nlportal-*` tokens), dan hebben die geen effect meer: de Den Haag-kleuren komen weer door en spacing valt weg. Zie migratie-instructies.
* **Haalcentraal HR:** in de GraphQL-response is `hoofdvestiging` gewijzigd naar `vestiging`. Hierdoor krijg je ook het juiste adres als je bent ingelogd voor een vestiging.
* `DocumentApisConfig.DocumentenApisConfigProperties.DocumentApiConfig`: `rsin` en `documentTypeUrl` zijn niet langer nullable (`String` met standaardwaarde `""`).

## Deprecations

* **Keycloak legacy token exchange (v1).** De legacy token exchange is verouderd. In de hele 4.x-lijn blijft `v1` de standaardwaarde, zodat bestaande omgevingen niets hoeven te wijzigen; draai je op `v1`, dan meldt de applicatie dat bij het opstarten met een waarschuwing. In **5.0** wordt `v2` de standaard — dat is een breaking change — en in een latere release verdwijnt `v1` helemaal. Migreren kan zonder downtime, zie [Migratie van v1 naar v2](../../../configuratie/keycloak-token-exchange-v2.md#migratie-van-v1-naar-v2).

## Removals

* De `payment`-module (Ogone) is verwijderd. Gebruik in plaats daarvan de `payment-direct`-module.
* De `messaging`-module (`nl.nl-portal:messaging`) is verwijderd, samen met de Spring Cloud Stream consumers voor portal case- en taakberichten (`UpdatePortalCaseMessage`, `UpdateExternalIdPortalCaseMessage` en vergelijkbare berichten).

Bekend probleem in 3.x (ook beschreven in de release notes van 3.1.2), voor wie nog op 3.x zit:
* De `messaging`-module publiceert bij het aanmaken van een case een `CreateExternalCaseMessage` (destination `create-external-case`) met de **ruwe** `submission` zoals de gebruiker die heeft ingediend, en niet de versie die tegen het JSON-schema van de case definition is gefilterd en gevalideerd. Dit is bewust: afnemers van de libraries kunnen use cases hebben die wij niet kunnen voorzien. Verwerk je deze berichten, bijvoorbeeld om een zaak aan te maken? Valideer de `submission` dan zelf tegen het schema van de case definition (`caseDefinitionId`) en negeer velden die niet in het schema staan.

## Migratie-instructies

Kom je van een oudere 3.x-versie? Lees eerst de release notes van de 3.x-versies na de jouwe in de 3.x-documentatie (versiekiezer linksboven).

**Messaging-module verwijderd.** Verwijder de dependency `nl.nl-portal:messaging` uit je `pom.xml` of `build.gradle`, en verwijder de `spring.cloud.stream`-bindings voor de portal case- en taakberichten uit je configuratie.

**Documenten API-configuratie.** Controleer dat elke documenten-API-configuratie een `rsin` en een `document-type-url` heeft. Ontbreekt er een, dan mislukken uploads naar die documenten-API.

**Keycloak token exchange v1 → v2 (optioneel).** Niet verplicht voor 4.x: laat je alles staan, dan blijft je omgeving op `v1` werken. Wil je nu al over, dan beschrijft [Migratie van v1 naar v2](../../../configuratie/keycloak-token-exchange-v2.md#migratie-van-v1-naar-v2) het pad zonder downtime, inclusief terugrolstap. Let op: zet `KEYCLOAK_TOKEN_EXCHANGE_VERSION=v2` en maak `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` in dezelfde wijziging leeg — een audience die uit de v1-opstelling blijft staan laat elke exchange mislukken.

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

**Den Haag design tokens 1.6.0.** Werk `@gemeente-denhaag/design-tokens` bij naar 1.6.0 en hernoem de Den Haag-tokens die je in je eigen thema overschrijft of gebruikt:

| Oud (1.1.0) | Nieuw (1.6.0) |
|---|---|
| `--denhaag-color-<kleur>-1` t/m `-5` (o.a. `green`, `blue`, `grey`, `ocher`) | `--denhaag-color-<kleur>-100`, `-300`, `-500`, `-600`, `-700` |
| `--denhaag-color-white` | `--denhaag-color-neutrals-white` |
| `--denhaag-space-block-*`, `--denhaag-space-inline-*` | `--denhaag-size-*` op gelijke waarde: `xs` → `200` (8px), `sm` → `300`, `md` → `400`, `lg` → `500`, `xl` → `600`, `2xl` → `800`, `3xl` → `1000`, `4xl` → `1200`, `5xl` → `1600` (64px) |
| `--denhaag-typography-scale-base-font-size`, `-lg-`, `-xl-` | `--denhaag-font-size-18`, `-20`, `-24` |
| `--denhaag-border-radius` | `--basis-border-radius-md` |
| `--denhaag-focus-border` | `var(--basis-focus-outline-width) var(--utrecht-focus-outline-style) var(--utrecht-focus-outline-color)` |
| `--denhaag-header-md-height` | `--denhaag-header-lg-height` |

Let op: de nieuwe `--basis-space-*` tokens hebben een andere schaal (`md` is 8px, was 16px). Gebruik ze dus niet als één-op-één vervanging van de oude `--denhaag-space-*` tokens. Zie de [changelog van `@gemeente-denhaag/design-tokens`](https://github.com/nl-design-system/denhaag/blob/main/proprietary/tokens/CHANGELOG.md).

**Broncode / maatwerk.** Bouw en draai met Java 21; bouw de frontend met Node 24 en pnpm 11. Controleer eigen uitbreidingen op Spring Boot 4- / Spring Security 7-wijzigingen.

**Payment-module verwijderd.** Verwijder de dependency op de `payment`-module uit je `pom.xml` of `build.gradle`.

## Bekende problemen

Er zijn geen bekende problemen.
