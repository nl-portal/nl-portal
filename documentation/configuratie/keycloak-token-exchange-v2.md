# Standard token exchange (v2)

Dit is de aanbevolen manier om de token exchange van de NL Portal in te richten. Deze pagina beschrijft één complete doelconfiguratie. Draai je nog op de [legacy variant](keycloak-token-exchange-v1.md), dan hoef je die niet eerst af te breken: [Migratie van v1 naar v2](#migratie-van-v1-naar-v2) beschrijft de overstap zonder downtime.

Vereist Keycloak 26.2 of nieuwer, en NL Portal backend libraries 3.1.0 of nieuwer.

## Wat je niet nodig hebt

* **Geen `KC_FEATURES`.** De standard token exchange is standaard beschikbaar. Er hoeft dus ook geen `admin-fine-grained-authz:v1` aan te staan, waardoor Fine-Grained Admin Permissions v2 beschikbaar blijft voor de hele Keycloak.
* **Geen fine-grained admin permissions.** Er is geen permission of policy nodig op een doelclient.
* **Geen aparte token-exchange client.** Twee clients volstaan.

## Clients

| Client (voorbeeldnaam) | Type | Doel |
| ---------------------- | ---- | ---- |
| `nl-portal` | public, standard flow | Login van de frontend (SPA). Draagt de `middel` mapper en de audience mapper. |
| `nl-portal-m2m` | confidential, service accounts, met secret | Voert de token exchange uit. Draagt de client scope met de `aanvrager` mappers en een audience mapper. |

Het verschil met de legacy variant zit in waar de claims vandaan komen. Bij v2 bouwt Keycloak de nieuwe token op basis van de **aanvragende** client, dus de m2m client. De mappers voor BSN en KVK horen daarom bij die client, niet bij een doelclient.

## Stap 1: client scope met de aanvrager mappers

Maak een client scope aan, bijvoorbeeld `aanvrager`, met twee protocol mappers van het type *User Attribute*:

| Mapper | User attribute | Token claim name |
| ------ | -------------- | ---------------- |
| `bsn` | `bsn` | `aanvrager.bsn` |
| `kvk` | `kvk` | `aanvrager.kvk` |

Zet bij beide *Add to access token* aan.

De punt in `aanvrager.bsn` is functioneel: Keycloak maakt daar geneste JSON van, `{"aanvrager":{"bsn":"..."}}`, en dat is precies wat de backend uitleest. Schrijf de naam dus letterlijk zo.

Koppel deze client scope als **default client scope** aan `nl-portal-m2m`.

Koppel de scope niet aan de frontend client. Dat is wat ervoor zorgt dat de browser nooit een BSN of KVK te zien krijgt.

## Stap 2: audience mappers

Voeg op `nl-portal` een protocol mapper toe van het type *Audience*, met *Included Client Audience* = `nl-portal-m2m`, en *Add to access token* aan.

Dit is nieuw ten opzichte van v1 en verplicht: de standard token exchange accepteert een subject token alleen als de aanvragende client al in de `aud` van die token staat. De mapper voegt alleen een clientnaam toe, geen gebruikersgegevens.

Voeg daarnaast op `nl-portal-m2m` dezelfde mapper toe, ook met *Included Client Audience* = `nl-portal-m2m`. Voor de token exchange zelf is dat niet nodig, maar het zorgt ervoor dat ook de geëxchangede token die audience draagt, wat de voorwaarde is om [audience validatie](#audience-validatie-aanbevolen) te kunnen aanzetten. De referentieconfiguratie in deze repository heeft beide mappers.

## Stap 3: standard token exchange aanzetten

Zet op `nl-portal-m2m` de schakelaar **Standard token exchange** aan (tabblad *Settings*, onder *Capability config*). In een realm export is dit het client attribuut `standard.token.exchange.enabled` met waarde `"true"`.

## Stap 4: backend configuratie

```yaml
nl-portal:
    authentication:
        keycloak:
            token-exchange-version: v2
            resource: nl-portal-m2m
            credentials:
                secret: <secret van de m2m client>
```

Of met de environment variabelen van de app image:

```
KEYCLOAK_TOKEN_EXCHANGE_VERSION=v2
KEYCLOAK_CLIENT_ID=nl-portal-m2m
KEYCLOAK_CLIENT_SECRET=<secret van de m2m client>
```

Of via de Helm chart:

```yaml
settings:
    keycloak:
        tokenExchangeVersion: v2
        clientID: nl-portal-m2m
        clientSecret: <secret van de m2m client>
        audience:
```

De chart weigert `v2` wanneer de gedeployde versie ouder is dan 3.1.0, en meldt dat bij het renderen in plaats van bij het opstarten.

`KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` laat je leeg of weg. In deze opzet bepaalt de m2m client zelf de claims en is een audience niet nodig. Vul je hem wel, dan geeft de backend de waarde door aan Keycloak, en moet het een audience zijn die de m2m client al kan afgeven. Een waarde die nog uit een v1 configuratie stamt is per definitie geen geldige v2 audience.

De backend property en de Keycloak configuratie horen bij elkaar. Zet je alleen de property om, dan verandert er niets aan de realm en andersom.

## Controleren of het werkt

Inloggen dat blijft werken is **geen** bewijs dat je op v2 zit. Een v2-verzoek tegen een realm die nog op v1 staat wordt door de legacy engine gewoon afgehandeld. Controleer daarom aan de Keycloak kant: staat de schakelaar Standard token exchange aan op de m2m client, dan draait deze variant.

De omgekeerde richting is wel hard: een v1-verzoek tegen een v2 client wordt geweigerd.

## Migratie van v1 naar v2

Vereist Keycloak 26.2 of nieuwer en NL Portal 3.1.0 of nieuwer. De volgorde is zo gekozen dat de
portal blijft werken terwijl je bezig bent: stap 1 tot en met 3 zijn toevoegingen aan het realm die
de legacy flow niet raken, en pas stap 4 zet de backend om.

1. Richt de client scope met de `aanvrager` mappers in en koppel die als default scope aan de m2m
   client. Zie [Stap 1](#stap-1-client-scope-met-de-aanvrager-mappers). De mappers staan bij v1 op
   de doelclient; je maakt hier een tweede exemplaar en laat het origineel voorlopig staan.
2. Voeg de audience mappers toe. Zie [Stap 2](#stap-2-audience-mappers). Verplicht voor v2, en
   zonder gevolgen voor v1.
3. Zet op de m2m client de schakelaar **Standard token exchange** aan. Zie
   [Stap 3](#stap-3-standard-token-exchange-aanzetten). Zolang `token-exchange:v1` in `KC_FEATURES`
   staat blijven beide varianten naast elkaar werken.
4. Zet de backend om en deploy:

   ```yaml
   nl-portal:
       authentication:
           keycloak:
               token-exchange-version: v2
   ```

   of met de environment variabele van de app image:

   ```
   KEYCLOAK_TOKEN_EXCHANGE_VERSION=v2
   ```

   **Haal in dezelfde stap `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` weg, of maak hem leeg.** De v1 waarde
   is de doelclient, en die is in v2 ongeldig: Keycloak antwoordt dan
   `Requested audience not available`. Zonder waarde bepaalt de m2m client zelf de claims, wat in
   deze opzet de bedoeling is.

5. Controleer of inloggen werkt, en of Mijn Gegevens nog BRP gegevens toont. Dat laatste bewijst dat
   de `aanvrager.bsn` claim uit de nieuwe client scope komt.
6. Ruim op, in deze volgorde: verwijder de token exchange permission op de doelclient, verwijder de
   doelclient zelf, verwijder de oude `aanvrager` mappers, en haal daarna `token-exchange:v1` en
   `admin-fine-grained-authz:v1` uit `KC_FEATURES`. Vanaf dat moment is Fine-Grained Admin
   Permissions v2 beschikbaar op deze Keycloak.

### Terugdraaien

Zet `token-exchange-version` terug op `v1` en zet `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` terug op de
doelclient. De Keycloak configuratie uit stap 1 tot en met 3 mag blijven staan. Dit werkt zolang
stap 6 nog niet is uitgevoerd.

### Let op bij stap 5

Dat inloggen blijft werken bewijst niet dat je op v2 zit. Zet je alleen de property om zonder stap 1
tot en met 3, dan handelt de legacy engine het verzoek gewoon af en merk je niets. De controle die
wel iets zegt is de schakelaar Standard token exchange op de m2m client, zoals hierboven beschreven.

## Foutmeldingen

| Melding van Keycloak | Oorzaak |
| -------------------- | ------- |
| `Standard token exchange is not enabled for the requested client` | Stap 3 ontbreekt. De schakelaar staat uit op de m2m client. |
| `Parameter 'subject_token_type' required for standard token exchange` | De backend staat nog op `v1` terwijl de realm al op v2 is ingericht. |
| `Requested audience not available: <client>` | Er staat nog een `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` ingevuld die de m2m client niet kan afgeven, meestal de oude doelclient. Haal de waarde weg. |

## Audience validatie (aanbevolen)

Standaard controleert de backend een binnenkomende token alleen op handtekening en geldigheidsduur. De app image configureert een `jwk-set-uri` en verder geen validatie, dus **elke** token die door hetzelfde realm is ondertekend wordt geaccepteerd, ongeacht voor welke client die is uitgegeven. Een andere applicatie in hetzelfde realm kan een token van een gebruiker dus doorspelen naar de portal API.

Met de mappers uit stap 2 dragen beide tokens de audience `nl-portal-m2m`, en kun je die controle aanzetten:

```
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=nl-portal-m2m
```

Of in de configuratie:

```yaml
spring:
    security:
        oauth2:
            resourceserver:
                jwt:
                    audiences: nl-portal-m2m
```

Tokens van andere clients in het realm worden daarna geweigerd, omdat die deze audience niet dragen.

Zet dit alleen aan als beide mappers uit stap 2 aanwezig zijn. De backend gebruikt dezelfde decoder voor de token van de browser en voor de geëxchangede token; ontbreekt de mapper op de m2m client, dan draagt de geëxchangede token de audience niet en faalt elke aanvraag.

Twee dingen om te weten als je de waarde zelf wil kiezen. De property accepteert een lijst, en een token is goedgekeurd zodra de `aud` **één** van de opgegeven waarden bevat. En zet er geen `account` in: die audience zit op vrijwel elke token in het realm, waarmee de controle niets meer tegenhoudt.

In de Helm chart stel je dit in met `settings.keycloak.acceptedAudiences`:

```yaml
settings:
    keycloak:
        acceptedAudiences: nl-portal-m2m
```

Die waarde staat standaard leeg en blijft dat ook, omdat de instelling alleen werkt wanneer de Keycloak configuratie erbij past. Standaard aanzetten zou elke omgeving zonder die mappers een 401 zonder bruikbare melding opleveren.

## Let op bij toekomstige versies

De property `token-exchange-version` is nieuw in 3.1.0 en bestaat om deze variant naast v1 te kunnen draaien. In 4.x kan de property een andere naam krijgen, de standaardwaarde `v2` worden, of verdwijnen omdat v1 niet meer ondersteund wordt. De Keycloak configuratie op deze pagina verandert daar niet door.
