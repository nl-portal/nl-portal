# Pagina's vanuit de backend

Met pagina's vanuit de backend configureer je pagina's die de standaard app-image toont met LiquidJS, zonder de frontend opnieuw te bouwen. Een pagina is bereikbaar via een [menu-item](menu-entries.md) met `route.type: templated` en `route.page: <sleutel>`. Het menu-item bepaalt dus het pad en de doelgroep, de pagina de inhoud.

## Paginatypes

| Type | Data | Bereikbaar via |
|---|---|---|
| `static` | geen | `GET /api/frontend/pages/<sleutel>` |
| `dynamic` | uit bindings; één binding kan opnieuw worden opgehaald met andere params (paginering, filters) | `GET /api/frontend/pages/<sleutel>`, of met een id `GET /api/frontend/pages/<sleutel>/<id>` |

Een `dynamic` pagina met een id hoort bij een menu-item met een padparameter, bijvoorbeeld `/erfpacht/:id`; de waarde van die parameter is het id. Zo'n menu-item is altijd `hidden`. Met een id haalt elke binding dat ene object op in plaats van een lijst.

De configuratie van een pagina legt alleen de structuur vast: bindings, `overrides` en `projection`. Het template en de teksten van een pagina komen uit een [templatebron](#templatebronnen): een bestand, een object in de Objecten API of een binding.

## Configuratie

```yaml
nl-portal:
  config:
    pages:
      enabled: true                       # zet /api/frontend/pages aan; vereist ook menu-entries-enabled
      properties:
        template-locations: file:/etc/nl-portal/pages/, classpath:pages/
        objecten:
          object-type-url: http://localhost:8010/api/v2/objecttypes/<uuid>   # alleen voor de bron objecten
  frontend:
    toggles:
      menu-entries-enabled: true
    menu-entries:
      - name: over-ons
        route: { type: templated, page: over-ons, path: /over-ons }
      - name: contact
        route: { type: templated, page: contact, path: /contact }
      - name: erfpacht
        audience: [burger, bedrijf]
        route: { type: templated, page: erfpacht, path: /erfpacht }
      - name: erfpacht-product
        hidden: true
        audience: [burger, bedrijf]
        route: { type: templated, page: product, path: /erfpacht/:id }
    pages:
      over-ons:
        type: static
        template: { source: file, params: { file: over-ons.pagetemplate.json } }
      contact:
        type: static
        template: { source: objecten, params: { object: 8a3c2e1f-4d5b-4f6a-9c7e-1b2d3f4a5c60 } }
      erfpacht:
        type: dynamic
        template: { source: file, params: { file: erfpacht.pagetemplate.json } }
        bindings:
          producten:
            resolver: openproduct
            params:
              product-type-code: ERFPACHT
              page: 0
              size: 10
            overrides: [page, filter.status]
            projection: '{items: [.results[] | {id: .uuid, naam, status}], total: .count}'
      product:
        type: dynamic
        template: { source: binding, binding: product }
        bindings:
          product:
            resolver: openproduct
            params:
              content-label: klantportaal
            projection: '{naam, start_datum, eind_datum, status, prijs}'
```

| Property | Standaardwaarde | Beschrijving |
|---|---|---|
| `type` | (verplicht) | `static` of `dynamic`. |
| `template.source` | (verplicht) | De [templatebron](#templatebronnen): `file`, `objecten` of `binding`. |
| `template.params` | – | Params van de templatebron, bijvoorbeeld `file` of `object`. |
| `template.binding` | – | Alleen bij `source: binding`: de alias van de binding die het template levert. |
| `bindings.<alias>` | – | Databronnen van de pagina. `static` heeft er geen, `dynamic` minstens één. De alias is de variabelenaam in het template. |
| `bindings.<alias>.resolver` | (verplicht) | De bron, bijvoorbeeld `openproduct` of `iko`. Zie [Resolvers](#resolvers). |
| `bindings.<alias>.params` | – | Standaardwaarden voor de resolver. |
| `bindings.<alias>.overrides` | – | De params die de pagina mag overschrijven bij opnieuw ophalen. Een key hoeft geen standaardwaarde te hebben. |
| `bindings.<alias>.projection` | `'{}'` | jq-expressie op de data van de resolver. Alleen het resultaat gaat naar de browser, dus ook een eventueel totaal voor paginering. |

De module zelf staat onder `nl-portal.config.pages`. De pagina's staan onder `nl-portal.frontend.pages`, naast de menu-items.

| Property | Standaardwaarde | Beschrijving |
|---|---|---|
| `enabled` | `false` | Zet de pagina's en `/api/frontend/pages` aan. Werkt alleen samen met `nl-portal.frontend.toggles.menu-entries-enabled`; zonder menu-items blijven de pagina's uit. |
| `properties.template-locations` | `classpath:pages/` | Mappen voor de bron `file`, met `classpath:` of `file:` en een `/` aan het eind. De eerste map met het bestand wint. |
| `properties.objecten.object-type-url` | – | Het objecttype `PortaalPaginaTemplate` voor de bron `objecten`. De verbinding met de Objecten API zelf staat onder `nl-portal.config.objectenapi`. |

Een ongeldige pagina of binding houdt de backend niet tegen bij het opstarten. Bij het opstarten staat elk configuratieprobleem als waarschuwing in de log (`Invalid nl-portal.frontend.pages.<sleutel>...`). Templates worden bij het opstarten niet geladen. Een ongeldige pagina, of een pagina waarvan het template niet geladen kan worden, geeft een 404 en een regel in de log. Een ongeldige binding geeft alleen een fout voor die binding.

### Afspraken voor params

De configuratie werkt op dezelfde manier vanuit YAML, een config server, environment variables en het configuratiepaneel. Daarom gelden deze afspraken:

* **Waarden zijn tekst.** `page: 0` wordt `"0"`; de resolver zet het om. Er is geen `null`: een key zonder waarde bestaat niet.
* **Lijsten** schrijf je geïndexeerd (`codes.0`, `codes.1`, zoals YAML-lijsten en environment variables worden) of als één waarde met komma's (`"a, b"`, zoals het configuratiepaneel ze opslaat). Lijstitems bevatten daarom geen komma; een sortering is `veld:asc` of `veld:desc`.
* **Keys worden genormaliseerd:** kleine letters, zonder streepjes, punten blijven staan. `PRODUCTTYPECODE` en `product-type-code` zijn dezelfde param, en de paginasleutel `overons` (uit een environment variable) hoort bij `route.page: over-ons`. Dat geldt ook voor namen die een resolver doorgeeft: een IKO-filter `filter.zaakType` komt bij IKO aan als `zaaktype`, dus configureer de filters in het IKO-profiel ook met genormaliseerde namen.
* **Aliassen** zijn `[a-z][a-z0-9]*` en niet `errors` of `meta`.
* **Bindings** worden altijd op alias gesorteerd, omdat de volgorde van environment variables niet vastligt.

### Als environment variables

```shell
NLPORTAL_CONFIG_PAGES_ENABLED=true
NLPORTAL_CONFIG_PAGES_PROPERTIES_TEMPLATELOCATIONS_0=file:/etc/nl-portal/pages/
NLPORTAL_CONFIG_PAGES_PROPERTIES_TEMPLATELOCATIONS_1=classpath:pages/
NLPORTAL_FRONTEND_PAGES_OVERONS_TYPE=static
NLPORTAL_FRONTEND_PAGES_OVERONS_TEMPLATE_SOURCE=file
NLPORTAL_FRONTEND_PAGES_OVERONS_TEMPLATE_PARAMS_FILE=over-ons.pagetemplate.json
NLPORTAL_FRONTEND_PAGES_ERFPACHT_BINDINGS_PRODUCTEN_RESOLVER=openproduct
NLPORTAL_FRONTEND_PAGES_ERFPACHT_BINDINGS_PRODUCTEN_PARAMS_PRODUCTTYPECODE=ERFPACHT
NLPORTAL_FRONTEND_PAGES_ERFPACHT_BINDINGS_PRODUCTEN_PARAMS_FILTER_STATUS=actief
NLPORTAL_FRONTEND_PAGES_ERFPACHT_BINDINGS_PRODUCTEN_PARAMS_SORT_0=naam:asc
NLPORTAL_FRONTEND_PAGES_ERFPACHT_BINDINGS_PRODUCTEN_OVERRIDES_0=page
```

Een `_` in de naam wordt een `.`: `..._PARAMS_FILTER_STATUS` is de param `filter.status`, `..._PARAMS_SORT_0` het eerste item van `sort`. Streepjes vallen weg en alles wordt kleine letters, vandaar de afspraken hierboven.

### Via het configuratiepaneel

Het configuratiepaneel slaat geneste keys plat op, zoals `nl-portal.frontend.pages.erfpacht.bindings.producten.params.filter.status`. Lijsten worden één waarde met komma's (`page, filter.status`) en lege waarden worden niet opgeslagen. Menu-items (een lijst van objecten) kun je nog niet via het paneel beheren.

## API

| Methode | Pad | Body | Antwoord |
|---|---|---|---|
| GET | `/api/frontend/pages/{page}` | – | pagina |
| GET | `/api/frontend/pages/{page}/{id}` | – | pagina (`dynamic`; elke binding haalt object `id` op) |
| POST | `/api/frontend/pages/{page}/bindings/{alias}` | `{ "params": { "page": "1" } }` | één binding (`dynamic`, alleen lijsten) |

```json
{
  "type": "dynamic",
  "template": "…",
  "messages": { "nl-NL": { "titel": "Erfpacht" }, "en-GB": { "titel": "Leasehold" } },
  "root": null,
  "bindings": [
    { "alias": "producten", "data": { "items": [ … ], "total": 23 }, "error": false,
      "meta": { "params": { "page": "0" } } }
  ]
}
```

* De endpoints vereisen een JWT en bestaan alleen als `nl-portal.config.pages.enabled` en `menu-entries-enabled` allebei aan staan.
* Een pagina is alleen beschikbaar als de gebruiker een `templated` menu-item naar die pagina mag zien (ook een `hidden` item). Anders is het antwoord 404.
* `messages` bevat alle teksten van het template, per locale. De frontend kiest de teksten van de taal van de gebruiker, zonder terugval op een andere taal.
* `root` is de alias van de binding als het template uit een binding komt (`source: binding`). Het template wordt dan getoond met de data van die binding als root; `template` is `null` als de binding geen template leverde.
* `meta.params` bevat de huidige waarden van de keys uit `overrides`. Een lege waarde in een POST zet een key terug naar de standaardwaarde.
* Een key die niet in `overrides` staat, of een waarde die de resolver afkeurt, geeft een 400. Foutmeldingen bevatten nooit configuratiedetails.
* De frontend stuurt `Accept-Language` mee met de locale van de app (`nl-NL`, `en-GB`). Alleen resolvers gebruiken de taal, bijvoorbeeld voor het content-element van Open Product. Van taal wisselen haalt de pagina niet opnieuw op.

## Templatebronnen

| Bron | Module | Opslag |
|---|---|---|
| `file` | core | een `.pagetemplate.json`-bestand op het classpath of in een vaste map |
| `objecten` | `zgw/objectenapi` | een `PortaalPaginaTemplate`-object in de Objecten API |
| `binding` | core | het template dat de resolver van een binding levert |

### Het contract `PortaalPaginaTemplate`

Een bestand (`file`) en een object (`objecten`) hebben hetzelfde formaat, zodat een template zonder aanpassing van de ene bron naar de andere kan. Het volgt [het schema](portaal-pagina-template.schema.json) (versie 1.0.0), zoals [Externe Klanttaak](https://dienstverleningsplatform.gitbook.io/platform-generieke-dienstverlening-public/patronen/taken/externe-klanttaak) een contract is voor taken:

| Veld | Type | Verplicht | Gebruik |
|---|---|---|---|
| `naam` | tekst | ja | om het template terug te vinden; wordt niet getoond |
| `template` | tekst | ja | het Liquid-template |
| `messages` | object | nee | per locale (`nl-NL`, `en-GB`, `fy-NL`, …) een object van message-id naar tekst |

```json
{
  "naam": "Over ons",
  "template": "{% heading level: 1 %}{{ 'titel' | i18n }}{% endheading %}\n{% paragraph %}{{ 'intro' | i18n }}{% endparagraph %}",
  "messages": {
    "nl-NL": { "titel": "Over ons", "intro": "Welkom bij de gemeente." },
    "en-GB": { "titel": "About us", "intro": "Welcome to the municipality." }
  }
}
```

Andere velden zijn niet toegestaan en elke tekst is een JSON-string. Een template dat niet aan het contract voldoet, geeft een 404 en een waarschuwing in de log.

### `file`

```yaml
template: { source: file, params: { file: over-ons.pagetemplate.json } }
```

Het bestand staat in een van de `template-locations`, bijvoorbeeld `src/main/resources/pages/` van je eigen app (`classpath:pages/`) of een gemounte map (`file:/etc/nl-portal/pages/`). Zet de gemounte map vóór het classpath, dan overschrijft een bestand daar het template uit de image.

* `file` is een relatieve naam van letters, cijfers, `.`, `_`, `-` en `/`, zonder `..`, die eindigt op `.pagetemplate.json`. Zo kan een pagina geen ander bestand lezen, zoals de Spring-configuratie.
* Een bestand in een `file:`-map dat via een symlink buiten die map uitkomt, wordt niet gelezen.
* Het bestand volgt het [contract](#het-contract-portaalpaginatemplate).

### `objecten`

```yaml
template: { source: objecten, params: { object: 8a3c2e1f-4d5b-4f6a-9c7e-1b2d3f4a5c60 } }
```

Het template staat in een object van het objecttype `PortaalPaginaTemplate`; `record.data` volgt het [contract](#het-contract-portaalpaginatemplate). Open Object controleert het schema bij het opslaan. Het objecttype stel je één keer in met `nl-portal.config.pages.properties.objecten.object-type-url`; een object van een ander type telt als niet gevonden. Zonder die instelling geeft elke pagina met deze bron een waarschuwing bij het opstarten en een 404.

Het object-UUID blijft gelijk bij een nieuwe versie van het object, dus na een wijziging hoeft de configuratie niet aangepast te worden. Een wijziging is zichtbaar bij de volgende keer dat de pagina geladen wordt.

### `binding`

```yaml
template: { source: binding, binding: product }
```

Het template komt van de resolver van binding `product`, bijvoorbeeld het content-element van het producttype in Open Product, en wordt getoond met de data van die binding als root (`{{ naam }}`). Dit kan alleen bij een `dynamic` pagina. Zonder template toont de pagina "Er zijn geen details beschikbaar.". Templates uit een binding hebben geen `messages`; de bron levert ze al per taal.

### Een eigen templatebron

Een templatebron is een Spring bean die `PageTemplateSource` implementeert, met een eigen `type`. `validate` controleert de params, `load` geeft het template met de teksten terug, of `null` als het niet bestaat. Templates zijn gedeelde inhoud: een bron laadt ze zonder het token van de gebruiker.

## Templates

Templates zijn [LiquidJS](https://liquidjs.com/) met `strictVariables`, `strictFilters` en `ownPropertyOnly`: een onbekende variabele of filter geeft een fout. HTML in templates wordt niet gerenderd; opmaak gaat via de tags hieronder.

Context van een template:

* `static`: geen data.
* `dynamic`: per binding `<alias>` de geprojecteerde data, plus `errors.<alias>` (`true` als de binding mislukt is) en `meta.<alias>` (de huidige params).
* `source: binding`: de data van die binding als root, bijvoorbeeld `{{ naam }}`.

### Teksten

Teksten schrijf je als message-id met het filter `i18n`; de teksten zelf staan per locale in `messages` van het template:

```liquid
{{ 'titel' | i18n }}
{{ 'aantal' | i18n: count: producten.total }}
```

* Argumenten zijn ICU-waarden, bijvoorbeeld voor `{count, plural, one {# product} other {# producten}}`.
* Een id zoekt eerst in de teksten van het template, dan in de teksten van de app.
* Een id zonder tekst in de taal van de gebruiker toont het id, zoals elk ander message in de portal. Er is geen terugval op een andere taal, dus geef elke tekst in elke taal.
* Teksten zijn platte tekst; opmaak hoort in het template.
* `title`-argumenten van tags zijn ook message-ids en zoeken op dezelfde manier.

### Tags

Argumenten die een naam zijn (`binding`, `param`, `page-param`, `href`, `src`, `title`) schrijf je tussen aanhalingstekens. Zonder aanhalingstekens is het een variabele.

| Tag | Argumenten | Inhoud | Toont |
|---|---|---|---|
| `heading` | `level` (1–4) | tekst | een kop |
| `paragraph` | – | tekst | een alinea |
| `link` | `href`, `title` | – (geen eindtag) | een link; `href` is relatief of `https:`, `http:`, `tel:`, `mailto:`, `//` |
| `image` | `src`, `alt` | – (geen eindtag) | een afbeelding; `src` is relatief of `https:` |
| `description-list` | `title` | rijen `label \| waarde` | een beschrijvingslijst |
| `table` | `title`, `binding`, `page-param`, `page-size`, `total` | rijen met cellen, gescheiden door `\|`; de eerste rij is de kop | een tabel, met paginering als `binding`, `page-param`, `page-size` en `total` gezet zijn |
| `filter` | `binding`, `param`, `title`, `page-param` | rijen `waarde \| label` | een keuzelijst; de lege waarde haalt het filter weg, `page-param` gaat terug naar de eerste pagina |
| `tabs` | `binding`, `param`, `page-param` | rijen `waarde \| label` | tabbladen die `param` zetten, zoals `filter`; het tabblad met de huidige waarde is geselecteerd. De inhoud eronder volgt via de binding |
| `search` | `binding`, `param`, `title`, `page-param` | – (geen eindtag) | een zoekveld met de huidige waarde; zoeken zet `param` op de tekst, een lege tekst haalt het weg, `page-param` gaat terug naar de eerste pagina |
| `alert` | `variant` (`info`, `success`, `warning`, `error`), `title` | tekst | een melding |
| `link-list` | – | rijen `label \| href` | een lijst met links; `href` als bij `link` |

`title` is een message-id uit de teksten van het template of de app; zonder tekst wordt het id zelf getoond.

In de rijen van `description-list`, `table`, `filter`, `tabs` en `link-list`:

* `\|` is een letterlijk sluisteken;
* in `table` maakt `**tekst**` een cel een kopcel;
* in `table` maakt `[tekst](/pad)` een cel een link; de eerste link in een rij geldt voor de hele rij.

Het `template` van een voorbeeld, met de teksten `titel`, `persoon`, `naam`, `geboortedatum`, `status`, `alle`, `open` en `gesloten` in `messages`:

```liquid
{% heading level: 1 %}{{ 'titel' | i18n }}{% endheading %}
{% description-list title: "persoon" %}
{{ 'naam' | i18n }} | {{ personen.naam | escape }}
{{ 'geboortedatum' | i18n }} | {{ personen.geboortedatum | date | escape }}
{% enddescription-list %}
{% filter binding: "vergunningen" param: "filter.status" title: "status" page-param: "page" %}
 | {{ 'alle' | i18n }}
open | {{ 'open' | i18n }}
gesloten | {{ 'gesloten' | i18n }}
{% endfilter %}
{% table binding: "vergunningen" page-param: "page" page-size: 5 total: vergunningen.total %}
{{ 'naam' | i18n }} | {{ 'status' | i18n }}
{% for vergunning in vergunningen.items %}{{ vergunning.naam | escape }} | {{ vergunning.status | capitalize | escape }}
{% endfor %}{% endtable %}
```

### Filters

Naast de standaardfilters van LiquidJS:

* `date`: zonder argument een korte datum in de taal van de gebruiker, met een strftime-argument de standaard LiquidJS-`date` (`{{ datum | date: "%d-%m-%Y" }}`);
* `currency`: een bedrag, standaard in euro (`{{ prijs | currency }}`).
* `i18n`: een tekst uit `messages` (zie [Teksten](#teksten)).

Gebruik `escape` voor data in de rijen van tags; de frontend toont de waarde daarna als tekst.

## Beveiliging

* Het subject (BSN of KVK) komt altijd uit het token, nooit uit de request.
* De browser kan alleen een paginasleutel, een id, een alias en waarden voor de keys uit `overrides` meesturen. Al het andere wordt geweigerd voordat een resolver iets doet.
* Alleen het resultaat van de `projection` gaat naar de browser. De standaard `'{}'` stuurt niets door.
* Resolvers geven nooit identifiers van andere subjects terug (Open Product: `eigenaren` wordt verwijderd).
* HTML in templates wordt niet gerenderd. Links en afbeeldingen gaan door een protocolcontrole.

## Resolvers

### `openproduct`

Producten van de ingelogde burger of het ingelogde bedrijf uit Open Product. Zonder id een lijst, met een id één product met een content-element als template (voor `source: binding`).

| Param | Gebruik |
|---|---|
| `product-type-code`, `product-type-codes` | filter op producttype; met een id moet het product van dit type zijn |
| `filter.status` | filter op status (`actief`, `gereed`, …) |
| `page` (vanaf 0), `size` (1–100) | paginering; standaard 0 en 10 |
| `content-label` | met een id: label van het content-element dat het template is |

Vast (niet in `overrides`): `product-type-code`, `product-type-codes`, `content-label`.

Met een id is het template de inhoud van het eerste content-element van het producttype met het label `content-label`, in de taal van de gebruiker. Zonder zo'n element toont de pagina "Er zijn geen details beschikbaar.". Een product dat niet van de gebruiker is, niet gepubliceerd is of een ongeldig id heeft, geeft een 404.

### `iko`

Een Aggregated Data Profile uit IKO voor de ingelogde burger of het ingelogde bedrijf, alleen zonder id. Vereist de [IKO API-module](../configuratie/iko-api.md); het profiel moet in `allowed-profiles` staan.

| Param | Gebruik |
|---|---|
| `profile` | naam van het profiel (verplicht) |
| `container` | container voor paginering, sortering en filters |
| `page` (vanaf 0), `size` (1–100) | paginering van de container; standaard 0 en 10 |
| `sort` | lijst van `veld:asc` of `veld:desc` |
| `filter.<veld>` | filters van de container |

Vast: `profile`, `container`. `page`, `size`, `sort` en `filter.*` vereisen een `container`.

### Een eigen resolver

Een resolver is een Spring bean die `PageDataResolver` implementeert, met een eigen `type`. De resolver krijgt de params en het token van de gebruiker, beperkt de data altijd tot dat subject en controleert params in `validate`. Voor pagina's met een id implementeert de resolver ook `resolve(context, params, id)`, die `null` teruggeeft als het object niet bestaat of niet van de gebruiker is.

Test in elk geval dat by-id voor een andere gebruiker `null` geeft, dat een storing een fout geeft, dat er geen identifiers van andere subjects in de output staan en dat `validate` ongeldige params weigert.

## Beperkingen

* Op de route van een `hidden` menu-item is de breadcrumb "Overzicht › titel", zonder markering in het menu.
* De titel komt uit het message `menu-entries.<name>` in de app. Zonder dat message is er geen kruimel en is de paginatitel de naam van de app.
* Data in de rijen van tags wordt niet geëscaped voor de rijsyntax: een waarde met `|`, een regeleinde of `](…` kan cellen, rijen of een linkdoel toevoegen. Vertrouw de inhoud van de bron of projecteer zulke velden weg.
* Geef `table` en `description-list` altijd een `title`. Zonder `title` toont `table` het message-id `tableList.title` en `description-list` de kop "Details".
* Pagina's zijn alleen-lezen.
* Een template staat als JSON-string in het bestand of in `record.data`. Meerdere regels schrijf je met `\n`; een redactie-omgeving voor templates is er nog niet.
* Elke wijziging van een template-object is direct zichtbaar; concepten en voorvertoning zijn er nog niet.
* Een template uit een binding (bijvoorbeeld Open Product) wisselt pas van taal bij de volgende navigatie.
