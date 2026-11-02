# NL-Portal 4.1.0

> **Let op:** `custom` menu-items tonen nog een placeholder. Deze release-notes beschrijven de stand op de feature branch en zijn nog niet bedoeld voor een release.

## Nieuwe Functionaliteit

* **Menu-items vanuit de backend.** Beheerders kunnen extra menu-items configureren onder `nl-portal.frontend.menu-entries` voor LiquidJS-pagina's (`templated`) en module federation-pagina's (`custom`) in de standaard app-image. Per item zijn groep, volgorde, icoon en doelgroep (`burger`, `bedrijf`, `subject`) in te stellen. De functionaliteit staat standaard uit. Zie [Menu-items vanuit de backend](../../../features/menu-entries.md).
  * `custom` toont voorlopig een placeholder ("Pagina niet gevonden").
  * Met `hidden: true` is een menu-item alleen een route, zonder item in het menu. Paden zijn volledige React Router-paden: een `:parameter` (alleen bij `hidden`) en een afsluitende `/*` (alleen bij `custom`).
  * De frontend haalt de items op via het nieuwe endpoint `GET /api/frontend/menu`. Dit endpoint valt buiten `/api/public/**` en vereist dus een JWT.
* **Pagina's vanuit de backend.** De nieuwe module `nl-portal.config.pages` (standaard uit, vereist ook de menu-items) laat `templated` menu-items pagina's tonen uit `nl-portal.frontend.pages`: `static` en `dynamic` (data uit bindings, met paginering en filters, en via een pad met id één object). Databronnen zijn de resolvers `openproduct` en `iko`. Het template en de teksten (per locale, met het filter `i18n`) staan niet in de configuratie maar komen uit een templatebron: een `.pagetemplate.json`-bestand op het classpath of in een vaste map (`file`) of een object in de Objecten API (`objecten`), allebei volgens het contract `PortaalPaginaTemplate`, of het template van een binding, zoals een content-element in Open Product (`binding`). Alleen het resultaat van de jq-`projection` gaat naar de browser. Nieuwe endpoints `GET /api/frontend/pages/{page}`, `GET /api/frontend/pages/{page}/{id}` en `POST /api/frontend/pages/{page}/bindings/{alias}`, met JWT. Zie [Pagina's vanuit de backend](../../../features/pages.md).
* **IKO API-module.** De nieuwe backend-library `iko-api` is de API-module voor communicatie met IKO. Deze eerste versie ondersteunt alleen het lezen van Aggregated Data Profiles voor de ingelogde burger of het ingelogde bedrijf. De module staat standaard uit, voegt geen GraphQL- of REST-endpoints toe en zit niet in de standaard app-images; voor bestaande installaties verandert er niets. Zie [IKO API](../../../configuratie/iko-api.md).

## Bugfixes

* Het zijmenu wordt niet meer smaller op pagina's met veel tekst, is maximaal 18rem breed, en lange labels lopen door op een volgende regel.

## Migratie-instructies

**Menu-items vanuit de backend (optioneel).**
1. Zet `nl-portal.frontend.toggles.menu-entries-enabled: true` en configureer `nl-portal.frontend.menu-entries`.
2. Maatwerkimplementaties: vervang het element van de `"*"`-route door `<MenuEntryRoutes />` uit `@nl-portal/nl-portal-user-interface`. Zet de toggle niet aan zonder deze stap. De standaard app-images doen dit al.

Terugdraaien kan door de toggle op `false` te zetten; `<MenuEntryRoutes />` mag dan blijven staan.

**Paden van `custom` menu-items.** Ten opzichte van de eerdere stand van de menu-items op deze branch moet een `custom` menu-item `/*` aan het pad toevoegen om onderliggende paden te blijven bedienen, bijvoorbeeld `path: /extern/*`. Een `:` of `*` was eerder niet toegestaan, dus bestaande configuratie zonder `/*` blijft geldig maar bedient alleen het exacte pad.
