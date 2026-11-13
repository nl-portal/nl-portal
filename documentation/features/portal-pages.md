# Portaalpagina's

Met portaalpagina's configureer je pagina's die de standaard app-image toont met LiquidJS, zonder de frontend opnieuw te bouwen. Een pagina is bereikbaar via een [menu-item](menu-entries.md) met `route.type: portal-page` en `route.page: <sleutel>`. Het menu-item bepaalt dus het pad en de doelgroep, de pagina de inhoud.

## Paginatypes

| Type | Data | Bereikbaar via |
|---|---|---|
| `static` | geen | `GET /api/frontend/portal-pages/<sleutel>` |
| `dynamic` | uit bindings; één binding kan opnieuw worden opgehaald met andere params (paginering, filters) | `GET /api/frontend/portal-pages/<sleutel>`, of met een id `GET /api/frontend/portal-pages/<sleutel>/<id>` |

Een `dynamic` pagina met een id hoort bij een menu-item met een padparameter, bijvoorbeeld `/erfpacht/:id`; de waarde van die parameter is het id. Zo'n menu-item is altijd `hidden`. Met een id haalt elke binding dat ene object op in plaats van een lijst.

De configuratie van een pagina legt alleen de structuur vast: bindings, `overrides` en `projection`. Het template en de teksten van een pagina komen uit een [templatebron](#templatebronnen): een bestand, een object in de Objecten API of een binding.

## Gouden regels

Voor wie templates schrijft of pagina's configureert. Jij bent verantwoordelijk voor wat een template met data doet; [Veilige uitvoer](#veilige-uitvoer) beschrijft wat de portal zelf doet.

1. **Toon data alleen met `{{ }}`**, nooit met `raw`, `echo`, `cycle`, `include`, `render` of `layout`.
2. **Gebruik geen `escape`:** `{{ }}` toont data al als tekst.
3. **Gebruik `html_text` voor velden met HTML of entities.**
4. **Begin een link of afbeelding met een vast pad of een vaste site**, nooit met data.
5. **Projecteer alleen wat de pagina toont.**
6. **Schrijf tekens zoals ze zijn** (`&`, niet `&amp;`), en `\|` voor een letterlijk sluisteken in een rij.
7. **Gebruik `lines` voor regeleinden uit data.**

## Veilige uitvoer

Dit is wat de portal zelf doet; al het andere is aan de template-schrijver. De frontend maakt van elke `{{ }}` eerst een plaatshouder. De tag leest zijn rijen, cellen en links uit de tekst met plaatshouders, en vult de waarden pas daarna in, als tekst. Een waarde met `|`, een regeleinde, `[x](…)`, `**…**` of `<b>` kan dus geen structuur toevoegen. Een link uit data gaat daarna door dezelfde controle als een link in het template.

* Plaatshouders gelden alleen voor `{{ }}`. `raw`, `echo` en `cycle` maken hun uitvoer deel van het template en kunnen rijen, cellen en links maken (regel 1).
* `include`, `render` en `layout` laden een ander template via de browser (regel 1).
* Argumenten van tags (`title: personen.naam`) zijn geen uitvoer. Ze gaan als tekst naar de component; een `href` of `src` gaat door de controle.
* De controle weigert, nadat de waarden zijn ingevuld, een link of afbeelding met een ander protocol dan de tag toestaat, een `//` aan het begin, een backslash, spaties aan het begin of eind of stuurtekens; het hele template wordt dan weggelaten (zie [Fouten in een template](#fouten-in-een-template)). Data aan het begin van een `href` of `src` kan nog steeds naar elke `https:`-site wijzen (regel 4).
* Een regeleinde in het template is een regeleinde in `paragraph`, `rich-text` en `page-header`; een regeleinde in data niet (regel 7).
* Of een veld platte tekst, HTML-gecodeerde tekst of HTML is, hangt af van de bron en is niet aan de waarde te zien. Daarom kiest de beheerder met `html_text` waar gedecodeerd wordt.

| Data | Template | Getoond |
|---|---|---|
| `Jan "de" Vries-O'Neill \ <b>&amp;</b>` + regeleinde + `regel 2` (IKO-mock `personen.naam`) | `Naam \| {{ personen.naam }}` in `description-list` | één rij: `Jan "de" Vries-O'Neill \ <b>&amp;</b> regel 2` |
| `Caf&eacute; &amp; bar` | `{{ v \| html_text }}` | `Café & bar` |
| `<p>Welkom</p>` | `{{ v }}` | `<p>Welkom</p>` |
| `<p>Welkom</p>` | `{{ v \| html_text }}` | `Welkom` |
| `x \| [klik](https://evil.example)` | `{{ v }}` in een `table`-cel | één cel met precies die tekst, zonder link |
| `null` | `[{{ v }}]` | `[]` |
| `javascript:alert(1)` | `[link]({{ v }})` in een `table`-cel | het hele template wordt weggelaten: "Deze inhoud kan niet worden weergegeven." |
| `een` + regeleinde + `twee` | `{{ v }}` in `paragraph` | `een twee` (geen regeleinde) |
| `een` + regeleinde + `twee` | de `lines`-loop in `paragraph` | `een`, regeleinde, `twee` |

## Configuratie

```yaml
nl-portal:
  config:
    portal-pages:
      enabled: true                       # zet /api/frontend/portal-pages aan; vereist ook menu-entries-enabled
      properties:
        template-locations: file:/etc/nl-portal/portal-pages/, classpath:portal-pages/
        object:
          object-type-url: http://localhost:8010/api/v2/objecttypes/<uuid>   # alleen voor de bron object
  frontend:
    toggles:
      menu-entries-enabled: true
    menu-entries:
      - name: over-ons
        route: { type: portal-page, page: over-ons, path: /over-ons }
      - name: contact
        route: { type: portal-page, page: contact, path: /contact }
      - name: erfpacht
        audience: [burger, bedrijf]
        route: { type: portal-page, page: erfpacht, path: /erfpacht }
      - name: erfpacht-product
        hidden: true
        audience: [burger, bedrijf]
        route: { type: portal-page, page: product, path: /erfpacht/:id }
    pages:
      over-ons:
        type: static
        template: { source: file, params: { file: over-ons.portal-page-template.json } }
      contact:
        type: static
        template: { source: object, params: { object-url: http://localhost:8010/api/v2/objects/8a3c2e1f-4d5b-4f6a-9c7e-1b2d3f4a5c60 } }
      erfpacht:
        type: dynamic
        template: { source: file, params: { file: erfpacht.portal-page-template.json } }
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
| `template.source` | (verplicht) | De [templatebron](#templatebronnen): `file`, `object` of `binding`. |
| `template.params` | – | Params van de templatebron, bijvoorbeeld `file` of `object`. |
| `template.binding` | – | Alleen bij `source: binding`: de alias van de binding die het template levert. |
| `bindings.<alias>` | – | Databronnen van de pagina. `static` heeft er geen, `dynamic` minstens één. De alias is de variabelenaam in het template. |
| `bindings.<alias>.resolver` | (verplicht) | De bron, bijvoorbeeld `openproduct` of `iko`. Zie [Resolvers](#resolvers). |
| `bindings.<alias>.params` | – | Standaardwaarden voor de resolver. |
| `bindings.<alias>.overrides` | – | De params die de pagina mag overschrijven bij opnieuw ophalen. Een key hoeft geen standaardwaarde te hebben. |
| `bindings.<alias>.projection` | `'{}'` | jq-expressie op de data van de resolver. Alleen het resultaat gaat naar de browser, dus ook een eventueel totaal voor paginering. |

De module zelf staat onder `nl-portal.config.portal-pages`. De pagina's staan onder `nl-portal.frontend.pages`, naast de menu-items.

| Property | Standaardwaarde | Beschrijving |
|---|---|---|
| `enabled` | `false` | Zet de pagina's en `/api/frontend/portal-pages` aan. Werkt alleen samen met `nl-portal.frontend.toggles.menu-entries-enabled`; zonder menu-items blijven de pagina's uit. |
| `properties.template-locations` | `classpath:portal-pages/` | Mappen voor de bron `file`, met `classpath:` of `file:` en een `/` aan het eind. De eerste map met het bestand wint. |
| `properties.object.object-type-url` | – | Het objecttype `PortaalPaginaTemplate` voor de bron `object`. De verbinding met de Objecten API zelf staat onder `nl-portal.config.objectenapi`. |

Een ongeldige pagina of binding houdt de backend niet tegen bij het opstarten, ook niet als Spring een waarde niet kan lezen (bijvoorbeeld `template: '…'` als tekst). Elk configuratieprobleem staat als fout in de log, bij het opstarten en na een refresh van de configuratie (`Invalid nl-portal.frontend.pages.<sleutel>...`), niet bij elke aanvraag. Een ongeldige pagina geeft een 404 en de menu-items ernaartoe worden weggelaten. Een ongeldige binding geeft alleen een fout voor die binding. Templates worden bij het opstarten niet geladen; een template dat niet geladen kan worden, geeft een 404 en een waarschuwing in de log. Zie ook [Logging](menu-entries.md#logging).

### Afspraken voor params

De configuratie werkt op dezelfde manier vanuit YAML, een config server, environment variables en het configuratiepaneel. Daarom gelden deze afspraken:

* **Waarden zijn tekst.** `page: 0` wordt `"0"`; de resolver zet het om. Er is geen `null`: een key zonder waarde bestaat niet.
* **Lijsten** schrijf je geïndexeerd (`codes.0`, `codes.1`, zoals YAML-lijsten en environment variables worden) of als één waarde met komma's (`"a, b"`, zoals het configuratiepaneel ze opslaat). Lijstitems bevatten daarom geen komma; een sortering is `veld:asc` of `veld:desc`.
* **Keys worden genormaliseerd:** kleine letters, zonder streepjes, punten blijven staan. `PRODUCTTYPECODE` en `product-type-code` zijn dezelfde param, en de paginasleutel `overons` (uit een environment variable) hoort bij `route.page: over-ons`. Dat geldt ook voor namen die een resolver doorgeeft: een IKO-filter `filter.zaakType` komt bij IKO aan als `zaaktype`, dus configureer de filters in het IKO-profiel ook met genormaliseerde namen.
* **Aliassen** zijn `[a-z][a-z0-9]*` en niet `errors` of `meta`.
* **Bindings** worden altijd op alias gesorteerd, omdat de volgorde van environment variables niet vastligt.

### Als environment variables

```shell
NLPORTAL_CONFIG_PORTALPAGES_ENABLED=true
NLPORTAL_CONFIG_PORTALPAGES_PROPERTIES_TEMPLATELOCATIONS_0=file:/etc/nl-portal/portal-pages/
NLPORTAL_CONFIG_PORTALPAGES_PROPERTIES_TEMPLATELOCATIONS_1=classpath:portal-pages/
NLPORTAL_FRONTEND_PAGES_OVERONS_TYPE=static
NLPORTAL_FRONTEND_PAGES_OVERONS_TEMPLATE_SOURCE=file
NLPORTAL_FRONTEND_PAGES_OVERONS_TEMPLATE_PARAMS_FILE=over-ons.portal-page-template.json
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
| GET | `/api/frontend/portal-pages/{page}` | – | pagina |
| GET | `/api/frontend/portal-pages/{page}/{id}` | – | pagina (`dynamic`; elke binding haalt object `id` op) |
| POST | `/api/frontend/portal-pages/{page}/bindings/{alias}` | `{ "params": { "page": "1" } }` | één binding (`dynamic`, alleen lijsten) |

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

* De endpoints vereisen een JWT en bestaan alleen als `nl-portal.config.portal-pages.enabled` en `menu-entries-enabled` allebei aan staan.
* Een pagina is alleen beschikbaar als de gebruiker een `portal-page` menu-item naar die pagina mag zien (ook een `hidden` item). Anders is het antwoord 404.
* `messages` bevat alle teksten van het template, per locale. De frontend kiest de teksten van de taal van de gebruiker, zonder terugval op een andere taal.
* `root` is de alias van de binding als het template uit een binding komt (`source: binding`). Het template wordt dan getoond met de data van die binding als root; `template` is `null` als de binding geen template leverde.
* `meta.params` bevat de huidige waarden van de keys uit `overrides`. Een lege waarde in een POST zet een key terug naar de standaardwaarde.
* Een key die niet in `overrides` staat, of een waarde die de resolver afkeurt, geeft een 400. Foutmeldingen bevatten nooit configuratiedetails.
* De frontend stuurt `Accept-Language` mee met de locale van de app (`nl-NL`, `en-GB`). Alleen resolvers gebruiken de taal, bijvoorbeeld voor het content-element van Open Product. Van taal wisselen haalt de pagina niet opnieuw op.

## Templatebronnen

| Bron | Module | Opslag |
|---|---|---|
| `file` | core | een `.portal-page-template.json`-bestand op het classpath of in een vaste map |
| `object` | `zgw/objectenapi` | een `PortaalPaginaTemplate`-object in de geconfigureerde Objecten API |
| `binding` | core | het template dat de resolver van een binding levert |

### Het contract `PortaalPaginaTemplate`

Een bestand (`file`) en een object (`object`) hebben hetzelfde formaat, zodat een template zonder aanpassing van de ene bron naar de andere kan. Het volgt [het schema](portaal-pagina-template.schema.json) (versie 1.0.0), zoals [Externe Klanttaak](https://dienstverleningsplatform.gitbook.io/platform-generieke-dienstverlening-public/patronen/taken/externe-klanttaak) een contract is voor taken:

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
template: { source: file, params: { file: over-ons.portal-page-template.json } }
```

Het bestand staat in een van de `template-locations`, bijvoorbeeld `src/main/resources/portal-pages/` van je eigen app (`classpath:portal-pages/`) of een gemounte map (`file:/etc/nl-portal/portal-pages/`). Zet de gemounte map vóór het classpath, dan overschrijft een bestand daar het template uit de image.

* `file` is een relatieve naam van letters, cijfers, `.`, `_`, `-` en `/`, zonder `..`, die eindigt op `.portal-page-template.json`. Zo kan een pagina geen ander bestand lezen, zoals de Spring-configuratie.
* Een bestand in een `file:`-map dat via een symlink buiten die map uitkomt, wordt niet gelezen.
* Het bestand volgt het [contract](#het-contract-portaalpaginatemplate).

### `object`

```yaml
template: { source: object, params: { object-url: http://localhost:8010/api/v2/objects/8a3c2e1f-4d5b-4f6a-9c7e-1b2d3f4a5c60 } }
```

Het template staat in een object van het objecttype `PortaalPaginaTemplate`; `record.data` volgt het [contract](#het-contract-portaalpaginatemplate). Open Object controleert het schema bij het opslaan. Het objecttype stel je één keer in met `nl-portal.config.portal-pages.properties.object.object-type-url`. De URL moet eindigen op het UUID van het objecttype. Alleen dat UUID wordt vergeleken, want de Objecttypen API kan op een andere host draaien en een object kan het objecttype met een interne URL aanduiden. Een object van een ander type telt als niet gevonden. Zonder die instelling geeft elke pagina met deze bron een fout in de log bij het opstarten en een 404.

`object-url` is de URL van het object zoals Open Object die toont (`…/api/v2/objects/<uuid>`). Het object komt altijd uit de Objecten API van `nl-portal.config.objectenapi`: een URL op een andere host is ongeldig, geeft een fout in de log bij het opstarten en een 404, en wordt nooit opgevraagd.

De URL blijft gelijk bij een nieuwe versie van het object, dus na een wijziging hoeft de configuratie niet aangepast te worden. Een wijziging is zichtbaar bij de volgende keer dat de pagina geladen wordt.

### `binding`

```yaml
template: { source: binding, binding: product }
```

Het template komt van de resolver van binding `product`, bijvoorbeeld het content-element van het producttype in Open Product, en wordt getoond met de data van die binding als root (`{{ naam }}`). Dit kan alleen bij een `dynamic` pagina. Zonder template toont de pagina "Er zijn geen details beschikbaar.". Templates uit een binding hebben geen `messages`; de bron levert ze al per taal.

### Een eigen templatebron

Een templatebron is een Spring bean die `PortalPageTemplateSource` implementeert, met een eigen `type`. `validate` controleert de params, `load` geeft het template met de teksten terug, of `null` als het niet bestaat. Templates zijn gedeelde inhoud: een bron laadt ze zonder het token van de gebruiker.

## Templates

Templates zijn [LiquidJS](https://liquidjs.com/) met `strictVariables`, `strictFilters` en `ownPropertyOnly`: een onbekende variabele of filter geeft een [fout](#fouten-in-een-template). HTML in templates wordt niet gerenderd; opmaak gaat via de tags hieronder.

### Fouten in een template

Een fout in een template laat het hele template weg. In plaats van de inhoud toont de pagina "Deze inhoud kan niet worden weergegeven."; het menu, de kop en de rest van de app blijven werken. Zo'n fout is bijvoorbeeld:

* een onbekende variabele, filter of tag, of een fout in de Liquid-syntax;
* een tag met een ongeldig argument, zoals `heading` met `level: 5`;
* een link of afbeelding die de [controle](#veilige-uitvoer) weigert, ook als de waarde uit data komt. Eén waarde met zo'n link laat dus het hele template weg.

De reden staat niet op het scherm. Test een template daarom met echte data.

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
* Argumenten van tags die tekst zijn (`title`, `sub-title`) zijn een message-id; bestaat er geen tekst met dat id, dan wordt de waarde zelf getoond. `title: "Mijn zaken"` werkt dus ook zonder message. Tekst in de inhoud van een tag is letterlijk; gebruik daar `{{ 'id' | i18n }}`.

### Tags

Argumenten die een naam zijn (`binding`, `param`, `page-param`, `href`, `src`, `title`) schrijf je tussen aanhalingstekens. Zonder aanhalingstekens is het een variabele.

| Tag | Argumenten | Inhoud | Toont |
|---|---|---|---|
| `heading` | `level` (1–4) | tekst | een kop |
| `paragraph` | – | tekst | een alinea |
| `link` | `href`, `title`, `target` (`_self`, `_blank`) | – (geen eindtag) | een link; `href` is relatief of `https:`, `http:`, `tel:`, `mailto:` (geen `//`, backslash of stuurtekens). Een relatieve link blijft in de app; `_blank` opent een nieuw tabblad |
| `image` | `src`, `alt` | – (geen eindtag) | een afbeelding; `src` is relatief of `https:` (geen `//`, backslash of stuurtekens) |
| `description-list` | `title` | rijen `label \| waarde` | een beschrijvingslijst |
| `table` | `title`, `binding`, `page-param`, `page-size`, `total` | rijen met cellen, gescheiden door `\|`; de eerste rij is de kop | een tabel, met paginering als `binding`, `page-param`, `page-size` en `total` gezet zijn |
| `filter` | `binding`, `param`, `title`, `page-param` | rijen `waarde \| label` | een keuzelijst; de lege waarde haalt het filter weg, `page-param` gaat terug naar de eerste pagina |
| `tabs` | `binding`, `param`, `page-param` | rijen `waarde \| label` | tabbladen die `param` zetten, zoals `filter`; het tabblad met de huidige waarde is geselecteerd. De inhoud eronder volgt via de binding |
| `search` | `binding`, `param`, `title`, `page-param` | – (geen eindtag) | een zoekveld met de huidige waarde; zoeken zet `param` op de tekst, een lege tekst haalt het weg, `page-param` gaat terug naar de eerste pagina |
| `alert` | `variant` (`info`, `success`, `warning`, `error`), `title` | tekst | een melding |
| `link-list` | – | rijen `label \| href` | een lijst met links; `href` als bij `link` |
| `back-link` | `href`, `title` | – (geen eindtag) | de terug-link van de app, bovenaan een detailpagina. Met `href` (een pad in de app, `/…`, zonder `//`, backslash of stuurtekens) linkt hij altijd daarheen: gebruik dat als de pagina één bovenliggende pagina heeft. Zonder `href` gaat hij terug naar de vorige pagina in de app, en zonder vorige pagina toont hij niets. Zonder `title` is de tekst "Terug" |
| `page-header` | `title`, `sub-title` | tekst en tags | de paginakop, met de inhoud eronder |
| `rich-text` | – | tekst en tags | opgemaakte tekst, bijvoorbeeld meerdere `paragraph`s met links |
| `unordered-list` | – | één item per regel | een opsomming |

`title` en `sub-title` zijn een message-id uit de teksten van het template of de app; zonder tekst wordt de waarde zelf getoond (zie [Teksten](#teksten)).

De inhoud van `paragraph`, `rich-text` en `page-header` mag andere tags bevatten, zoals een `link` midden in een alinea:

```liquid
{% paragraph %}Kijk op {% link href: "https://www.denhaag.nl/erfpacht" title: "denhaag.nl" target: "_blank" %} voor meer.{% endparagraph %}
```

In de rijen van `description-list`, `table`, `filter`, `tabs` en `link-list`:

* `\|` is een letterlijk sluisteken;
* in `table` maakt `**tekst**` een cel een kopcel;
* in `table` maakt `[tekst](/pad)` een cel een link; de eerste link in een rij geldt voor de hele rij. Data mag in de tekst en het pad: `[{{ product.naam }}](/erfpacht/{{ product.id }})`. Het pad wordt gecontroleerd nadat de waarden zijn ingevuld.

Het `template` van een voorbeeld, met de teksten `titel`, `persoon`, `naam`, `geboortedatum`, `status`, `alle`, `open` en `gesloten` in `messages`:

```liquid
{% heading level: 1 %}{{ 'titel' | i18n }}{% endheading %}
{% description-list title: "persoon" %}
{{ 'naam' | i18n }} | {{ personen.naam }}
{{ 'geboortedatum' | i18n }} | {{ personen.geboortedatum | date }}
{% enddescription-list %}
{% filter binding: "vergunningen" param: "filter.status" title: "status" page-param: "page" %}
 | {{ 'alle' | i18n }}
open | {{ 'open' | i18n }}
gesloten | {{ 'gesloten' | i18n }}
{% endfilter %}
{% table binding: "vergunningen" page-param: "page" page-size: 5 total: vergunningen.total %}
{{ 'naam' | i18n }} | {{ 'status' | i18n }}
{% for vergunning in vergunningen.items %}{{ vergunning.naam }} | {{ vergunning.status | capitalize }}
{% endfor %}{% endtable %}
```

### Widget-tags

Een widget-tag zet een ander onderdeel van de portal op de pagina, met zijn eigen data. Die data komt uit de API van dat onderdeel, altijd beperkt tot de ingelogde gebruiker, en niet uit een binding. Er is dus geen `projection` en het template kan de data niet gebruiken. Alleen onderdelen die al data aan de browser leveren krijgen een widget-tag.

| Tag | Argumenten | Toont |
|---|---|---|
| `theme-cases` | `theme` (verplicht), `page-size` (1–100, standaard 5) | de lopende zaken van de gebruiker in het Open Product-thema met naam `theme`, uit de query `getOpenProductThemaZakenByName` |
| `theme-tasks` | `theme` (verplicht), `page-size` (1–100, standaard 5) | de taken van de gebruiker bij die zaken en bij zijn producten in het thema, uit de query `getOpenProductThemaTakenByName` |

```liquid
{% theme-tasks theme: "Wonen en bouwen" %}
{% theme-cases theme: "Wonen en bouwen" page-size: 10 %}
```

`theme` is de exacte `naam` van een gepubliceerd thema in Open Product, hoofdlettergevoelig. Ook de GraphQL-queries zelf accepteren alleen een `pageSize` van 1 tot en met 100. Een onbekend thema, of een thema zonder zaaktypen, toont een lege lijst. De widget-tags vereisen de Open Product-module.

## Portaalfilters

De filters die de portal zelf registreert, of waarvan de portal de LiquidJS-versie vervangt. De uitkomst van elk filter wordt nog steeds als tekst getoond (zie [Veilige uitvoer](#veilige-uitvoer)).

### `date`

Vervangt de LiquidJS-`date`. Zonder argument een korte datum in de taal van de gebruiker (`31-1-2026`); met een strftime-argument precies de LiquidJS-`date`.

* Invoer: een datum als tekst (`2026-01-31`, ISO 8601) of een getal (milliseconden). Uitvoer: tekst.
* Zonder argument geeft `null` `1-1-1970` en een ongeldige datum `Invalid Date`; zet er bij een optioneel veld een `{% if %}` omheen. Met een argument geeft LiquidJS een ongeldige waarde ongewijzigd terug.
* Gebruik het voor elke datum uit data.

```liquid
{{ product.startDatum | date }}
{{ product.startDatum | date: "%d-%m-%Y" }}
```

### `currency`

Een bedrag in de taal van de gebruiker, standaard in euro; een valutacode als argument kiest een andere valuta.

* Invoer: een getal. Uitvoer: tekst, bijvoorbeeld `€ 12,50`.
* `null` geeft `€ 0,00`; een waarde die geen getal is geeft `€ NaN`.

```liquid
{{ product.prijs | currency }}
{{ product.prijs | currency: "USD" }}
```

### `i18n`

Een tekst uit `messages` van het template of uit de app, met ICU-argumenten als keyword-argumenten. Zie [Teksten](#teksten).

* Invoer: een message-id. Uitvoer: de tekst in de taal van de gebruiker.
* Een id zonder tekst toont het id.

```liquid
{{ 'titel' | i18n }}
{{ 'aantal' | i18n: count: producten.total }}
```

### `html_text`

De tekst van een HTML- of HTML-gecodeerde waarde: tags worden weggehaald en elke entity wordt gedecodeerd (`&amp;`, `&eacute;`, `&#233;`). De waarde wordt in een inert document gelezen, dus scripts en afbeeldingen doen niets.

* Invoer: tekst. Uitvoer: tekst, die nog steeds als tekst getoond wordt; een `|` in het resultaat blijft dus één cel.
* `null` geeft `""`; andere waarden worden eerst tekst.
* Gebruik het alleen voor velden waarvan je weet dat de bron HTML of entities stuurt (regel 3).

```liquid
{{ product.omschrijving | html_text }}
```

`Caf&eacute; &amp; bar` wordt `Café & bar`.

### `lines`

Splitst een waarde op regeleinden (`\n` of `\r\n`) in een lijst van regels, elk zonder spaties aan begin en eind.

* Invoer: tekst. Uitvoer: een lijst.
* `null` geeft een lege lijst.
* Gebruik het voor regeleinden uit data: een loop met een regeleinde in het template geeft per regel een regeleinde in `paragraph`, en elke regel blijft een eigen waarde.

```liquid
{% paragraph %}{% assign regels = adres | lines %}{% for regel in regels %}{{ regel }}
{% endfor %}{% endparagraph %}
```

In een `unordered-list` geeft dezelfde loop één item per regel:

```liquid
{% unordered-list %}{% assign regels = voorwaarden | lines %}{% for regel in regels %}
{{ regel }}{% endfor %}
{% endunordered-list %}
```

In een JSON-templatebestand schrijf je de loop hetzelfde, met `\n` voor het regeleinde in het template; anders dan bij `split: "\n"` hoeft er niets dubbel geëscaped te worden.

## Standaard LiquidJS-filters

Alle andere [filters van LiquidJS](https://liquidjs.com/filters/overview.html) (versie 10, zie `package.json` van `user-interface`) werken ongewijzigd. Een onbekend filter geeft een fout (`strictFilters`). Omdat uitvoer tekst is, gedragen deze zich anders dan je misschien verwacht:

* **`raw`** maakt de uitvoer deel van het template. Data door `raw` kan rijen, cellen en links toevoegen; gebruik het nooit op data uit een bron (regel 1).
* **`escape`, `escape_once`, `xml_escape`** zijn niet nodig voor de veiligheid. Hun entities worden als tekst getoond: `Jan &amp; Piet`.
* **`newline_to_br`** toont `<br />` als tekst; gebruik [`lines`](#lines).
* **`strip_html`** haalt tags weg maar decodeert geen entities; gebruik [`html_text`](#html_text).

## Beveiliging

* Template-schrijvers zijn verantwoordelijk voor wat hun template met data doet en volgen de [Gouden regels](#gouden-regels); [Veilige uitvoer](#veilige-uitvoer) beschrijft wat de portal zelf doet.
* Het subject (BSN of KVK) komt altijd uit het token, nooit uit de request.
* De browser kan alleen een paginasleutel, een id, een alias en waarden voor de keys uit `overrides` meesturen. Al het andere wordt geweigerd voordat een resolver iets doet.
* Alleen het resultaat van de `projection` gaat naar de browser. De standaard `'{}'` stuurt niets door.
* Resolvers geven nooit identifiers van andere subjects terug (Open Product: `eigenaren` wordt verwijderd).
* HTML in templates en data wordt niet gerenderd. Links en afbeeldingen gaan door een controle, ook als de link uit data komt; welke data in een link komt, bepaalt de template-schrijver (regel 4).

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

Een resolver is een Spring bean die `PortalPageBindingResolver` implementeert, met een eigen `type`. De resolver krijgt de params en het token van de gebruiker, beperkt de data altijd tot dat subject en controleert params in `validate`. Voor pagina's met een id implementeert de resolver ook `resolve(context, params, id)`, die `null` teruggeeft als het object niet bestaat of niet van de gebruiker is.

Test in elk geval dat by-id voor een andere gebruiker `null` geeft, dat een storing een fout geeft, dat er geen identifiers van andere subjects in de output staan en dat `validate` ongeldige params weigert.

## Beperkingen

* Op de route van een `hidden` menu-item is de breadcrumb "Overzicht › titel". Het menu markeert het zichtbare menu-item erboven (het langste pad); is er geen, dan "Overzicht".
* De titel komt uit het message `menu-entries.<name>` in de app. Zonder dat message is er geen kruimel en is de paginatitel de naam van de app.
* Geef `table` en `description-list` altijd een `title`. Zonder `title` toont `table` het message-id `tableList.title` en `description-list` de kop "Details".
* Pagina's zijn alleen-lezen.
* Een template staat als JSON-string in het bestand of in `record.data`. Meerdere regels schrijf je met `\n`; een redactie-omgeving voor templates is er nog niet.
* Elke wijziging van een template-object is direct zichtbaar; concepten en voorvertoning zijn er nog niet.
* Een template uit een binding (bijvoorbeeld Open Product) wisselt pas van taal bij de volgende navigatie.
