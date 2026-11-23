# Menu-items vanuit de backend

Met menu-items vanuit de backend voeg je extra pagina's toe aan het menu van de standaard app-image, zonder de frontend opnieuw te bouwen. Een menu-item verwijst naar een LiquidJS-pagina (`templated`) of een module federation-pagina (`custom`).

Maatwerkimplementaties blijven hun eigen pagina's registreren in hun `routes.tsx` en menu-items.

> **Status:** beide types tonen voorlopig een placeholder ("Pagina niet gevonden"). De echte weergave van `templated` (nl-portal-issues#170) en `custom` (nl-portal-issues#171) volgt.

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
          type: templated
          page: template-a
          path: /thema-concept
      - name: extern-portaal
        icon: GridIcon
        route:
          type: custom
          page: remote-a
          path: /extern
```

| Property | Standaardwaarde | Beschrijving |
|---|---|---|
| `name` | (verplicht) | Unieke naam, alleen `a-z`, `0-9` en `-`. Bepaalt de titel. |
| `icon` | `DocumentIcon` | Naam van een icoon uit `@gemeente-denhaag/icons`, bijvoorbeeld `ParkingIcon`. Een onbekende naam valt terug op `DocumentIcon`. |
| `group` | `1` | Index van de menugroep, `>= 0`. |
| `ordering` | (achteraan) | Positie binnen de groep, `>= 0`. |
| `audience` | (iedereen) | Lijst van `burger`, `bedrijf` en/of `subject`. |
| `route.type` | (verplicht) | `templated` of `custom`. |
| `route.page` | (verplicht) | De template-sleutel (`templated`) of de remote module (`custom`). |
| `route.path` | (verplicht) | Uniek pad, begint met `/`, zonder `:` of `*`. |

In env-vorm staat de index in de naam, bijvoorbeeld `NLPORTAL_FRONTEND_MENUENTRIES_0_NAME`, `NLPORTAL_FRONTEND_MENUENTRIES_0_ROUTE_TYPE` en `NLPORTAL_FRONTEND_MENUENTRIES_0_ROUTE_PATH`.

Een ongeldige configuratie laat de backend niet opstarten. De foutmelding noemt alle problemen tegelijk, per item als `menu-entries[i] (<name>)`.

## Gedrag

* Elk menu-item is een item in het menu én een route.
* `templated` is één pagina op precies `path`. `custom` bedient ook elk pad onder `path` (bijvoorbeeld `/extern/a/b`) en blijft daar gemarkeerd in het menu.
* Een menu-item waarvan het pad al door een route van de app wordt gebruikt, wordt overgeslagen met een waarschuwing in de console. Dat geldt ook voor routes met parameters (zoals `/zaken/:id`) en paden die alleen in hoofdletters verschillen. Standaard menu-items worden nooit verwijderd of gewijzigd.
* **`group`:** de index van de menugroep. Ontbreekt `group`, dan komt het item in groep 1. Is `group` groter dan of gelijk aan het aantal groepen, dan komt het item in één nieuwe groep onderaan, die alle zulke items delen.
* **`ordering`:** de uiteindelijke positie binnen de groep. Ontbreekt `ordering` of is de waarde groter dan de groep, dan komt het item achteraan. Items met dezelfde `ordering` houden de volgorde uit de configuratie.
* **`audience`:** elke gebruiker hoort bij precies één audience:
  * `burger`: ingelogd als burger (DigiD);
  * `bedrijf`: ingelogd als bedrijf (eHerkenning);
  * `subject`: elke andere ingelogde gebruiker.

  Een item zonder `audience` is zichtbaar voor iedereen. De vergelijking is niet hoofdlettergevoelig.

## Vertalingen

De titel van een menu-item is voorlopig het message-id `menu-entries.<name>`, bijvoorbeeld `menu-entries.thema-concept`. Vertalingen vanuit de backend volgen later.

## Beperkingen

* De breadcrumb stopt bij de pagina van het menu-item ("Overzicht › titel"), ook op onderliggende paden van een `custom`-item.
* Mislukt het ophalen van de menu-items, dan wordt dat pas opnieuw geprobeerd na het herladen van de pagina. Het menu toont dan alleen de standaard items.
