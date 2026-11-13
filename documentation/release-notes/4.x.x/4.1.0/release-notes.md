# NL-Portal 4.1.0

> **Let op:** deze release-notes beschrijven de stand op de feature branch en zijn nog niet bedoeld voor een release.

## Nieuwe Functionaliteit

* **Menu-items vanuit de backend.** Beheerders kunnen extra menu-items configureren onder `nl-portal.frontend.menu-entries` voor portaalpagina's (`portal-page`) in de standaard app-image. Per item zijn groep, volgorde, icoon en doelgroep (`burger`, `bedrijf`, `subject`) in te stellen. De functionaliteit staat standaard uit. Zie [Menu-items vanuit de backend](../../../features/menu-entries.md).
  * Met `hidden: true` is een menu-item alleen een route, zonder item in het menu. Paden zijn volledige React Router-paden: een `:parameter` (alleen bij `hidden`) en een afsluitende `/*` (niet bij `portal-page`).
  * De frontend haalt de items op via het nieuwe endpoint `GET /api/frontend/menu`. Dit endpoint valt buiten `/api/public/**` en vereist dus een JWT.
  * Een ongeldige configuratie van menu-items of portaalpagina's houdt de backend niet tegen bij het opstarten: ongeldige items en pagina's worden weggelaten, met een fout in de log bij het opstarten en na een refresh van de configuratie. De frontend ziet ze niet.
  * Een `route.type` werkt alleen als de functie erachter aan staat. Implementaties kunnen eigen types toevoegen met een `FrontendMenuRouteType`-bean en een renderer via `AppProvider` (`menuEntryRenderers`). Zie [Route-types](../../../features/menu-entries.md#route-types).
* **Portaalpagina's.** De nieuwe module `nl-portal.config.portal-pages` (standaard uit, vereist ook de menu-items) laat `portal-page` menu-items portaalpagina's tonen uit `nl-portal.frontend.pages`: `static` en `dynamic` (data uit bindings, met paginering en filters, en via een pad met id één object). Databronnen zijn de resolvers `openproduct` en `iko`. Het template en de teksten (per locale, met het filter `i18n`) staan niet in de configuratie maar komen uit een templatebron: een `.portal-page-template.json`-bestand op het classpath of in een vaste map (`file`) of een object in de Objecten API, op URL (`object`), allebei volgens het contract `PortaalPaginaTemplate`, of het template van een binding, zoals een content-element in Open Product (`binding`). Alleen het resultaat van de jq-`projection` gaat naar de browser. Nieuwe endpoints `GET /api/frontend/portal-pages/{page}`, `GET /api/frontend/portal-pages/{page}/{id}` en `POST /api/frontend/portal-pages/{page}/bindings/{alias}`, met JWT. Zie [Portaalpagina's](../../../features/portal-pages.md).
* **Voorwaarden op menu-items.** Met `conditional` is een menu-item alleen zichtbaar als elke voorwaarde geldt voor de ingelogde gebruiker; de pagina van het item volgt dezelfde voorwaarden. De Open Product-module levert `thema: <naam>` (de gebruiker heeft een product in dat thema). Een onbekende sleutel laat het item weg, met een fout in de log. Eigen voorwaarden zijn beans die `FrontendMenuConditionalChecker` implementeren. Zie [Voorwaarden](../../../features/menu-entries.md#voorwaarden).
* **Nieuwe tags voor pagina's.** `page-header`, `rich-text`, `unordered-list` en `back-link` (de terug-link van de app); `link` krijgt `target` (`_self`, `_blank`) en blijft bij een relatieve link in de app; tags mogen in de inhoud van `paragraph`, `rich-text` en `page-header` staan. Tekstargumenten (`title`, `sub-title`) zijn een message-id, of anders letterlijke tekst.
* **Widget-tags.** `theme-cases` en `theme-tasks` tonen de zaken en taken van de gebruiker in een Open Product-thema, met de nieuwe GraphQL-queries `getOpenProductThemaZakenByName` en `getOpenProductThemaTakenByName`. Daarnaast zijn er queries per product: `getOpenProductZaken`, `getOpenProductTaken`, `getOpenProductDocumenten` en `getOpenProductDecisions`.
* **Uitvoer is tekst.** Elke `{{ }}` in een template wordt als tekst getoond en kan geen rijen, cellen, links of regeleinden toevoegen. Template-schrijvers zijn verantwoordelijk voor wat een template met data doet: `raw`, `echo` en `cycle` maken uitvoer deel van het template, en `include`, `render` en `layout` laden een ander template. Nieuwe filters `html_text` (HTML en entities naar tekst) en `lines` (regels uit data). De uitvoer van `escape` wordt niet meer gedecodeerd: verwijder `| escape` uit templates. Zie [Gouden regels](../../../features/portal-pages.md#gouden-regels).
* **IKO API-module.** De nieuwe backend-library `iko-api` is de API-module voor communicatie met IKO. Deze eerste versie ondersteunt alleen het lezen van Aggregated Data Profiles voor de ingelogde burger of het ingelogde bedrijf. De module staat standaard uit, voegt geen GraphQL- of REST-endpoints toe en zit niet in de standaard app-images; voor bestaande installaties verandert er niets. Zie [IKO API](../../../configuratie/iko-api.md).

## Bugfixes

* Support voor uploaden naar een document api die niet de default is.
* Het zijmenu wordt niet meer smaller op pagina's met veel tekst, is maximaal 18rem breed, en lange labels lopen door op een volgende regel.

## Migratie-instructies

**Menu-items vanuit de backend (optioneel).**
1. Zet `nl-portal.frontend.toggles.menu-entries-enabled: true` en configureer `nl-portal.frontend.menu-entries`.
2. Maatwerkimplementaties: vervang het element van de `"*"`-route door `<MenuEntryRoutes />` uit `@nl-portal/nl-portal-user-interface`. Zet de toggle niet aan zonder deze stap. De standaard app-images doen dit al.

Terugdraaien kan door de toggle op `false` te zetten; `<MenuEntryRoutes />` mag dan blijven staan.

**Ongeldige configuratie start.** Een ongeldig menu-item of een ongeldige portaalpagina laat de backend niet meer stoppen bij het opstarten, maar wordt weggelaten met een fout in de log. Controleer na een update de log bij het opstarten op regels `Invalid nl-portal.frontend.`.
