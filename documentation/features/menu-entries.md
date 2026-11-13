# Menu-items vanuit de backend

Met menu-items vanuit de backend voeg je routes en items in het menu toe aan de app, zonder de frontend opnieuw te bouwen. Een menu-item is een route, en ook een item in het menu als het niet `hidden` is. Wat de route toont, bepaalt het `route.type`: elk type werkt, zolang het in de backend en de frontend is geïmplementeerd. Zie [Route-types](#route-types).

## Aanzetten

De functionaliteit staat standaard uit. Aanzetten gaat in twee stappen:

1. Zet in de backend `nl-portal.frontend.toggles.menu-entries-enabled: true` (env: `NLPORTAL_FRONTEND_TOGGLES_MENUENTRIESENABLED=true`) en configureer `menu-entries`.
2. Gebruik in de frontend `<MenuEntryRoutes />` als element van de `"*"`-route:

   ```tsx
   import { MenuEntryRoutes } from "@nl-portal/nl-portal-user-interface";

   {
     path: "*",
     element: <MenuEntryRoutes />,
   }
   ```

   `MenuEntryRoutes` stuurt elk pad dat geen menu-item is door naar `paths.noMatch`, net als de vorige `"*"`-route.

Zet de toggle niet aan zonder `<MenuEntryRoutes />` te mounten: de menu-items verschijnen dan wel, maar leiden naar de eigen `"*"`-route van de app. De standaard app-images mounten `<MenuEntryRoutes />` al.

Na het inloggen haalt de frontend de menu-items op via `GET /api/frontend/menu`. Dit endpoint vereist een JWT en geeft alleen de items terug die zichtbaar zijn voor de gebruiker.

## Configuratie

```yaml
nl-portal:
  frontend:
    toggles:
      menu-entries-enabled: true
    menu-entries:
      - name: thema-concept
        icon: ParkingIcon
        group: 2
        ordering: 0
        audience: [burger]
        route:
          type: portal-page
          page: template-a
          path: /thema-concept
      - name: thema-concept-detail
        hidden: true
        route:
          type: portal-page
          page: template-a-detail
          path: /thema-concept/:id
```

Het voorbeeld gebruikt het route-type `portal-page` van de [portaalpagina's](portal-pages.md).

| Property | Standaardwaarde | Beschrijving |
|---|---|---|
| `name` | (verplicht) | Unieke naam, alleen `a-z`, `0-9` en `-`. Bepaalt de titel. |
| `icon` | `DocumentIcon` | Naam van een icoon uit `@gemeente-denhaag/icons`, bijvoorbeeld `ParkingIcon`. Een onbekende naam valt terug op `DocumentIcon`. |
| `group` | `1` | Index van de menugroep, `>= 0`. |
| `ordering` | (achteraan) | Positie binnen de groep, `>= 0`. |
| `audience` | (iedereen) | Lijst van `burger`, `bedrijf` en/of `subject`. |
| `hidden` | `false` | `true`: alleen een route, geen item in het menu. |
| `conditional` | – | Voorwaarden per gebruiker, als `<sleutel>: <waarde>`. Zie [Voorwaarden](#voorwaarden). |
| `route.type` | (verplicht) | Het route-type. Zie [Route-types](#route-types). |
| `route.page` | (verplicht) | Gaat naar het route-type; wat de waarde betekent, bepaalt het type. |
| `route.path` | (verplicht) | Uniek pad in React Router-vorm, begint met `/`. Zie [Paden](#paden). |

In env-vorm staat de index in de naam, bijvoorbeeld `NLPORTAL_FRONTEND_MENUENTRIES_0_NAME`, `NLPORTAL_FRONTEND_MENUENTRIES_0_ROUTE_TYPE` en `NLPORTAL_FRONTEND_MENUENTRIES_0_ROUTE_PATH`.

Een ongeldige configuratie houdt de backend niet tegen bij het opstarten. Een ongeldig menu-item wordt weggelaten uit het menu; de frontend ziet het niet en hoort ook niet waarom. Elk probleem staat als fout in de log, per item als `Invalid nl-portal.frontend.menu-entries[i] (<name>): …; the entry is left out`. Zie [Logging](#logging).

* Hebben items dezelfde `name` of hetzelfde `route.path`, dan worden ze allemaal weggelaten.
* Een waarde die Spring niet kan lezen (bijvoorbeeld `ordering: eerste` of `hidden: misschien`) laat dat item weg in plaats van de standaardwaarde te gebruiken.

## Paden

`route.path` is een volledig React Router-pad. Elk segment na de eerste `/` is:

* een vaste tekst van `A-Z`, `a-z`, `0-9`, `.`, `_`, `~` en `-`;
* een parameter `:naam` (een letter, daarna letters en cijfers), bijvoorbeeld `/erfpacht/:id`;
* alleen als laatste segment: `*`, voor alle onderliggende paden.

Regels:

* een afsluitende `*` is toegestaan, tenzij het route-type hem afwijst;
* een pad met een `:parameter` vereist `hidden: true`, want zo'n pad is geen bestemming in het menu.

## Gedrag

* Een menu-item is een route, en ook een item in het menu als het niet `hidden` is.
* Het pad wordt gebruikt zoals het is geconfigureerd. Een pad dat eindigt op `/*` (bij een route-type dat dat toestaat) bedient ook elk pad daaronder, zoals `/eigen/a/b`, en blijft daar gemarkeerd in het menu. Het menu-item linkt naar het pad zonder `/*`.
* Een menu-item waarvan het pad al door een route van de app wordt gebruikt, wordt overgeslagen met een waarschuwing in de console. Dat geldt ook voor routes met parameters (zoals `/zaken/:id`) en paden die alleen in hoofdletters verschillen. Standaard menu-items worden nooit verwijderd of gewijzigd.
* **`group`:** de index van de menugroep. Ontbreekt `group`, dan komt het item in groep 1. Is `group` groter dan of gelijk aan het aantal groepen, dan komt het item in één nieuwe groep onderaan, die alle zulke items delen.
* **`ordering`:** de uiteindelijke positie binnen de groep. Ontbreekt `ordering` of is de waarde groter dan de groep, dan komt het item achteraan. Items met dezelfde `ordering` houden de volgorde uit de configuratie.
* **`audience`:** elke gebruiker hoort bij precies één audience:
  * `burger`: ingelogd als burger (DigiD);
  * `bedrijf`: ingelogd als bedrijf (eHerkenning);
  * `subject`: elke andere ingelogde gebruiker.

  Een item zonder `audience` is zichtbaar voor iedereen. De vergelijking is niet hoofdlettergevoelig.

## Voorwaarden

Met `conditional` is een menu-item alleen zichtbaar als elke voorwaarde geldt voor de ingelogde gebruiker, bovenop `audience`:

```yaml
- name: wonen-en-bouwen
  audience: [burger, bedrijf]
  conditional:
    thema: Wonen en bouwen
  route: { type: portal-page, page: wonen-en-bouwen, path: /wonen-en-bouwen }
```

De sleutel `thema` komt hier van de [Open Product-module](../fundamentals/architectuur/integraties/product.md#voorwaarde-thema).

* Alle voorwaarden moeten gelden. Een item zonder `conditional` is niet afhankelijk van voorwaarden.
* De voorwaarden worden bij elke keer ophalen van het menu opnieuw gecontroleerd, per gebruiker.
* Een onbekende sleutel laat het item weg, met een fout in de log. Een checker die faalt (bijvoorbeeld een storing van de bron) telt als "geldt niet".
* In env-vorm: `NLPORTAL_FRONTEND_MENUENTRIES_0_CONDITIONAL_THEMA=Wonen en bouwen`.

### Een eigen voorwaarde

Een voorwaarde is een Spring bean die `FrontendMenuConditionalChecker` implementeert:

```kotlin
class AbonnementChecker(private val service: AbonnementService) : FrontendMenuConditionalChecker {
    override val key = "abonnement"

    override suspend fun check(authentication: Authentication, value: String): Boolean =
        service.heeftAbonnement(authentication, value)
}
```

`key` is de sleutel onder `conditional`. `check` krijgt het token van de gebruiker en de waarde uit de configuratie, en geeft `false` bij een fout. Registreer de bean alleen als de functie erachter aan staat. Hebben twee checkers dezelfde sleutel, dan wint de eerste (in `@Order`) en staat er een waarschuwing in de log.

## Route-types

Het `route.type` bepaalt wat de route van een menu-item toont. Een type bestaat uit twee delen:

* in de backend een bean die `FrontendMenuRouteType` implementeert. Zonder bean wordt het item weggelaten, met een fout in de log;
* in de frontend een renderer. Zonder renderer wordt het item in de frontend overgeslagen, met een waarschuwing in de console.

Bijvoorbeeld `portal-page` van de [portaalpagina's](portal-pages.md#menu-item).

### Een eigen route-type

In de backend is dat een Spring bean die `FrontendMenuRouteType` implementeert; registreer de bean alleen als de functie aan staat:

```kotlin
class AfsprakenRouteType : FrontendMenuRouteType {
    override val type = "afspraken"

    override fun problems(entry: FrontendMenuEntryProperties): List<String> =
        if (entry.route.path.endsWith("/*")) listOf("route.path may not end in /*") else emptyList()
}
```

`problems` geeft de problemen van een item voor dit type; een item met problemen wordt weggelaten. Hebben twee beans hetzelfde `type`, dan wint de eerste (in `@Order`) en staat er een waarschuwing in de log.

In de frontend geef je `AppProvider` een renderer per type. De lijst vervangt de standaard renderers, dus neem `MENU_ENTRY_RENDERERS` over:

```tsx
import { AppProvider, MENU_ENTRY_RENDERERS, MenuEntryRenderers } from "@nl-portal/nl-portal-user-interface";

const menuEntryRenderers: MenuEntryRenderers = { ...MENU_ENTRY_RENDERERS, afspraken: AfsprakenPage };

<AppProvider menuEntryRenderers={menuEntryRenderers}>…</AppProvider>
```

Een renderer krijgt `{ entry }`. Definieer de lijst buiten het component: de menu-items worden één keer geladen. Een item zonder renderer wordt in de frontend overgeslagen met een waarschuwing in de console.

## Logging

* Configuratieproblemen staan alleen in de log bij het opstarten en na een refresh van de configuratie (Spring Cloud Config), niet bij elke aanvraag.
* **Fout (ERROR):** iets wordt weggelaten, zoals een menu-item of een pagina.
* **Waarschuwing (WARN):** het werkt nog, maar niet zoals bedoeld, bijvoorbeeld twee checkers met dezelfde sleutel. Ook een bron die faalt tijdens een aanvraag, zoals een storing bij een voorwaarde, is een waarschuwing. Die regel noemt alleen het soort fout en de HTTP-status, niet de URL: de URL van een aanvraag kan een BSN of KVK-nummer bevatten.
* **TRACE:** de volledige fout, met URL, voor het zoeken naar de oorzaak. Staat standaard uit. Zet je TRACE aan voor het package van een checker (bijvoorbeeld `nl.nlportal.openproduct`), dan kunnen er BSN's in de log komen; doe dat alleen tijdelijk.

## Vertalingen

De titel van een menu-item is voorlopig het message-id `menu-entries.<name>`, bijvoorbeeld `menu-entries.thema-concept`. Vertalingen vanuit de backend volgen later.

## Beperkingen

* De breadcrumb stopt bij de pagina van het menu-item ("Overzicht › titel"), ook op onderliggende paden van een item met `/*`. Ook op de route van een `hidden` menu-item is de breadcrumb "Overzicht › titel"; het menu markeert dan het zichtbare menu-item erboven (het langste pad), of anders "Overzicht".
* Zonder het message `menu-entries.<name>` in de app heeft de route van een `hidden` menu-item geen kruimel en is de paginatitel de naam van de app.
* Mislukt het ophalen van de menu-items, dan wordt dat pas opnieuw geprobeerd na het herladen van de pagina. Het menu toont dan alleen de standaard items.
