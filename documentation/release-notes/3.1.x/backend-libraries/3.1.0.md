# NL-Portal Backend Libraries 3.1.0

## Nieuwe Functionaliteit

De volgende functionaliteiten zijn nieuw toegevoegd:

* **Ondersteuning voor Keycloak Standard Token Exchange (v2).** Naast de legacy token exchange (v1)
  werkt de backend nu ook met de Standard Token Exchange van Keycloak 26.2 en nieuwer. Je kiest de
  variant per omgeving met de nieuwe property
  `nl-portal.authentication.keycloak.token-exchange-version`, of met de environment variabele
  `KEYCLOAK_TOKEN_EXCHANGE_VERSION` van de app image. De standaardwaarde is `v1`, dus bestaande
  omgevingen werken zonder aanpassing verder.

  Met v2 heeft de portal de Keycloak feature `token-exchange:v1` niet meer nodig, en daarmee ook
  `admin-fine-grained-authz:v1` niet. Daardoor komt Fine-Grained Admin Permissions v2 beschikbaar op
  de Keycloak waarop de portal draait. Dat is de belangrijkste reden om over te stappen: die twee
  features sluiten elkaar server-breed uit, en Keycloak voegt token exchange permissions niet toe
  aan Fine-Grained Admin Permissions v2.

  De inrichting staat in de documentatie:

  * [Standard token exchange (v2)](../../../configuratie/keycloak-token-exchange-v2.md), de
    aanbevolen doelconfiguratie
  * [Migratie van v1 naar v2](../../../configuratie/keycloak-token-exchange-v2.md#migratie-van-v1-naar-v2),
    de stappen om over te stappen zonder downtime, inclusief terugdraaien
  * [Legacy token exchange (v1)](../../../configuratie/keycloak-token-exchange-v1.md), de variant die
    tot en met 3.0.x de enige mogelijkheid was
  * [Keycloak configuratie](../../../configuratie/keycloak.md), de keuze tussen beide varianten

* **Audience validatie bruikbaar gemaakt.** De backend controleert een binnenkomende token standaard
  alleen op handtekening en geldigheidsduur, en accepteert daarmee elke token die door hetzelfde
  realm is ondertekend. De v2 opzet geeft zowel de frontend client als de m2m client een audience
  mapper, en daarmee kun je die controle aanzetten met
  `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES`. Tokens van andere clients in het realm
  worden dan geweigerd. Dit is optioneel en staat standaard uit, ook in de Helm charts, omdat het
  alleen werkt wanneer de Keycloak configuratie erbij past. Zie
  [Audience validatie](../../../configuratie/keycloak-token-exchange-v2.md#audience-validatie-aanbevolen).

* **Helm chart 3.2.0.** De `nl-portal-backend` chart 3.2.0 sluit hierop aan met de nieuwe waarde
  `settings.keycloak.tokenExchangeVersion` (standaard `v1`), `settings.keycloak.audience` die alleen
  nog verplicht is bij `v1`, en de optionele `settings.keycloak.acceptedAudiences`. De chart weigert
  `v2` te renderen wanneer de gedeployde versie ouder is dan 3.1.0.

## Bugfixes

De volgende bugs zijn opgelost:

* Wanneer Keycloak een token exchange weigert, wordt de foutmelding van Keycloak nu gelogd, samen met
  de actieve variant. Voorheen verdween die tekst en bleef alleen een HTTP status over.

* Een aantal modules controleerde niet al zijn vereiste modules. `zakenapi`, `berichten`,
  `openproduct` en `product` activeerden zichzelf ook wanneer een vereiste module uitstond, waarna de
  applicatie tijdens het opstarten faalde met een `NoSuchBeanDefinitionException`, en in Kubernetes
  met een crash loop. Deze modules controleren nu al hun vereisten en blijven inactief wanneer er één
  ontbreekt.

  | Module | Aanvullend vereist |
  | ------ | ------------------ |
  | `zakenapi` | `documentenapis`, `besluitenapi` |
  | `berichten` | `objectenapi` |
  | `openproduct` | `catalogiapi`, `documentenapis`, `besluitenapi` |
  | `product` | `catalogiapi`, `documentenapis`, `besluitenapi`, `taak` |

  Een configuratie die eerder werkte verandert hier niet door: die had deze modules al aanstaan, want
  anders startte de applicatie niet op. Een configuratie die eerder crashte start nu wel op, met de
  betreffende module inactief. Zie
  [Module afhankelijkheden](../../../configuratie/module-dependency-guide.md).

* De `openklant2` module hield zich niet aan zijn eigen schakelaar. De client en de service van de
  module werden altijd aangemaakt, ook wanneer `nl-portal.config.openklant2.enabled` uitstond. De
  GraphQL queries van de module keken al wel naar die schakelaar, dus functioneel was de module toen
  ook uit. De module respecteert de schakelaar nu volledig. Implementaties die de beans
  `OpenKlant2Service` of `OpenKlant2KlantinteractiesClient` rechtstreeks injecteren, moeten
  `nl-portal.config.openklant2.enabled: true` zetten.

* De `openproduct` module registreerde de configuratie-eigenschappen van de `taak` module
  (`nl-portal.config.taak.*`). Die binding komt nu uitsluitend uit de `taak` module zelf. Voor
  `openproduct` verandert dit niets, omdat die module `taak` nu al vereist.

## Breaking changes

Er zijn geen breaking changes.

## Deprecations

De legacy token exchange (v1) blijft in 3.x de standaard, maar is door Keycloak als verouderd
gemarkeerd en wordt in een toekomstige Keycloak versie verwijderd. Keycloak voegt geen ondersteuning
voor token exchange permissions toe aan Fine-Grained Admin Permissions v2, dus v1 blijft FGAP v1
vereisen.

De property `token-exchange-version` is nieuw in 3.1.0. In 4.x kan deze een andere naam krijgen of
de standaardwaarde `v2` worden.

## Bekende problemen

Er zijn geen bekende problemen.
