# NL-Portal 4.1.0

> **Let op:** deze release-notes beschrijven de stand op de feature branch en zijn nog niet bedoeld voor een release.

## Nieuwe Functionaliteit

* **Menu-items vanuit de backend.** Beheerders voegen routes en menu-items toe onder `nl-portal.frontend.menu-entries`, per doelgroep. Standaard uit. Zie [Menu-items vanuit de backend](../../../features/menu-entries.md).
* **Voorwaarden op menu-items.** Met `conditional` is een menu-item alleen zichtbaar als elke voorwaarde geldt voor de gebruiker. Zie [Voorwaarden](../../../features/menu-entries.md#voorwaarden).
* **Portaalpagina's.** De nieuwe module `nl-portal.config.portal-pages` toont pagina's met LiquidJS-templates uit de backend, met data uit bindings. Standaard uit. Zie [Portaalpagina's](../../../features/portal-pages.md).
* **Open Product.** Nieuwe GraphQL-queries per thema en product, de voorwaarde `thema` en de resolver `openproduct`. Zie [Open Product](../../../fundamentals/architectuur/integraties/product.md).
* **IKO API-module.** Nieuwe module `iko-api` voor het lezen van Aggregated Data Profiles uit IKO, met de resolver `iko`. Zit in de standaard app-images en staat standaard uit. Zie [IKO API](../../../configuratie/iko-api.md).

## Bugfixes

* Support voor uploaden naar een document api die niet de default is.
* Het zijmenu wordt niet meer smaller op pagina's met veel tekst, is maximaal 18rem breed, en lange labels lopen door op een volgende regel.

## Migratie-instructies

**Menu-items vanuit de backend (optioneel).**
1. Zet `nl-portal.frontend.toggles.menu-entries-enabled: true` en configureer `nl-portal.frontend.menu-entries`.
2. Maatwerkimplementaties: vervang het element van de `"*"`-route door `<MenuEntryRoutes />` uit `@nl-portal/nl-portal-user-interface`. Zet de toggle niet aan zonder deze stap. De standaard app-images doen dit al.

Terugdraaien kan door de toggle op `false` te zetten; `<MenuEntryRoutes />` mag dan blijven staan.
