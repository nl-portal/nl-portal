# NL-Portal 4.1.0

> **Let op:** de menu-items vanuit de backend tonen nog een placeholder. Deze release-notes beschrijven de stand op de feature branch en zijn nog niet bedoeld voor een release.

## Nieuwe Functionaliteit

* **Menu-items vanuit de backend.** Beheerders kunnen extra menu-items configureren onder `nl-portal.frontend.menu-entries` voor LiquidJS-pagina's (`templated`) en module federation-pagina's (`custom`) in de standaard app-image. Per item zijn groep, volgorde, icoon en doelgroep (`burger`, `bedrijf`, `subject`) in te stellen. De functionaliteit staat standaard uit. Zie [Menu-items vanuit de backend](../../../features/menu-entries.md).
  * Beide types tonen voorlopig een placeholder ("Pagina niet gevonden").
  * De frontend haalt de items op via het nieuwe endpoint `GET /api/frontend/menu`. Dit endpoint valt buiten `/api/public/**` en vereist dus een JWT.
* **IKO API-module.** De nieuwe backend-library `iko-api` is de API-module voor communicatie met IKO. Deze eerste versie ondersteunt alleen het lezen van Aggregated Data Profiles voor de ingelogde burger of het ingelogde bedrijf. De module staat standaard uit, voegt geen GraphQL- of REST-endpoints toe en zit niet in de standaard app-images; voor bestaande installaties verandert er niets. Zie [IKO API](../../../configuratie/iko-api.md).

## Bugfixes

* Het zijmenu wordt niet meer smaller op pagina's met veel tekst, is maximaal 18rem breed, en lange labels lopen door op een volgende regel.

## Migratie-instructies

**Menu-items vanuit de backend (optioneel).**
1. Zet `nl-portal.frontend.toggles.menu-entries-enabled: true` en configureer `nl-portal.frontend.menu-entries`.
2. Maatwerkimplementaties: vervang het element van de `"*"`-route door `<MenuEntryRoutes />` uit `@nl-portal/nl-portal-user-interface`. Zet de toggle niet aan zonder deze stap. De standaard app-images doen dit al.

Terugdraaien kan door de toggle op `false` te zetten; `<MenuEntryRoutes />` mag dan blijven staan.
