# Gemachtigde en machtigingen

Met een machtiging handelt een ingelogde persoon of organisatie (de **gemachtigde**) namens een ander (de **aanvrager**). Denk aan iemand die via DigiD Machtigen de zaken van een familielid regelt, of een medewerker die via eHerkenning namens een bedrijf inlogt. Deze pagina beschrijft hoe de NL Portal hiermee omgaat, hoe je het inricht en hoe je het lokaal test.

## Hoe het werkt

De NL Portal roept zelf geen machtigingenregister aan. Alles wordt bepaald door claims in de **geëxchangede** token (zie [Token exchange](keycloak.md#token-exchange)). Keycloak, of de identity provider erachter, is dus verantwoordelijk voor het vaststellen van de machtiging.

| Claim | Betekenis |
| ----- | --------- |
| `aanvrager.bsn` / `aanvrager.kvk` / `aanvrager.sub` | De partij namens wie gehandeld wordt. Bepaalt het gebruikerstype (burger, bedrijf of generieke gebruiker, zie [Gebruikerstypen](keycloak.md#gebruikerstypen)) en **wiens gegevens** getoond worden. |
| `gemachtigde.bsn` / `gemachtigde.kvk` / `gemachtigde.sub` | De partij die daadwerkelijk is ingelogd en handelt. |
| `urn:etoegang:core:ServiceUUID` | De machtigingsdienst(en) waarvoor de gemachtigde gemachtigd is. Een enkele waarde of een lijst. Zie [Machtigingsdiensten](#machtigingsdiensten). |

Elk gebruikerstype kan een gemachtigde hebben, en de gemachtigde kan zowel een persoon (BSN) als een organisatie (KVK) zijn.

### Wat de gemachtigde ziet

* **Gegevens van de aanvrager.** Zaken, taken, berichten, objecten, OpenKlant en Mijn Gegevens worden altijd opgehaald voor de aanvrager. Een gemachtigde ziet dus wat de aanvrager zelf zou zien.
* **Wie er handelt.** De GraphQL query `getGemachtigdeV2` geeft de gegevens van de gemachtigde terug: bij een BSN uit de BRP (Haal Centraal), bij een KVK-nummer uit het Handelsregister. De frontend toont dit in de header ("Ingelogd namens") en op de overzichtspagina ("Gemachtigd voor").
* **Beperking op type.** Alleen bij een bedrijf als aanvrager kan een machtigingsdienst de zichtbare zaken, taken en producten beperken. Zie [Machtigingsdiensten](#machtigingsdiensten).

## Keycloak configuratie

De gemachtigde-claims worden, net als de `aanvrager`-claims, toegevoegd door protocol mappers op de client scope van de m2m client (bij [v2](keycloak-token-exchange-v2.md)) of op de token-exchange client (bij [v1](keycloak-token-exchange-v1.md)). Met user attributes zoals in de lokale ontwikkelomgeving ziet dat er zo uit:

| Mapper (type *User Attribute*) | User attribute | Token claim name | Multivalued |
| ------------------------------ | -------------- | ---------------- | ----------- |
| gemachtigde bsn | `gemachtigdeBsn` | `gemachtigde.bsn` | Nee |
| gemachtigde kvk | `gemachtigdeKvk` | `gemachtigde.kvk` | Nee |
| etoegang service uuid | `serviceUuid` | `urn:etoegang:core:ServiceUUID` | Ja |

In productie komen deze waarden doorgaans van de identity provider (DigiD Machtigen, eHerkenning) en map je ze met een identity provider mapper in plaats van met user attributes.

### De middel claim

De frontend bepaalt aan de hand van de `middel` claim of de machtigingsweergave actief is (zie [De middel claim](keycloak.md#de-middel-claim)):

| `middel` | Weergave |
| -------- | -------- |
| `machtigen` | Persoonsweergave én machtigingsweergave. Gebruik dit voor een persoon die namens een persoon handelt. |
| `bewindvoering` | Bedrijfsweergave én machtigingsweergave. Gebruik dit voor handelen namens een bedrijf. |

Er is geen mogelijkheid in de frontend om te wisselen tussen meerdere aanvragers. De gemachtigde handelt per sessie namens één aanvrager.

## Machtigingsdiensten

Een machtigingsdienst beperkt welke zaaktypen, producttypen en taaktypen een gemachtigde mag zien. De backend koppelt de waarde(n) in de claim `urn:etoegang:core:ServiceUUID` aan de geconfigureerde machtigingsdiensten:

```yaml
nl-portal:
    authentication:
        machtingsdienst:  # let op: de property heet "machtingsdienst"
            configurations:
                - uuid: 00000000-0000-0000-0000-000000000001
                  naam: Bezwaar behandelen
                  zaak-types:
                      - 5ffc767c-8a7e-45e1-ad06-37ddc7e9074d
                  product-types: []
                  taak-types: []
            all-machtiging-uuid:  # optioneel
            resource-url:         # optioneel, verouderd
```

| Property | Betekenis |
| -------- | --------- |
| `configurations` | Lijst van machtigingsdiensten. Per dienst de `uuid` (zoals in de ServiceUUID claim), een `naam` en de toegestane `zaak-types` (UUID's van zaaktypen), `product-types` (UUID's van producttypen in OpenProduct) en `taak-types` (waarden van `eigenaar` op het taakobject). |
| `all-machtiging-uuid` | Een ServiceUUID die "alles toegestaan" betekent. Bevat de claim deze waarde, dan geldt er geen beperking. |
| `resource-url` | Verouderd alternatief voor `configurations`: een JSON-bestand (bijvoorbeeld `classpath:machtigingsdiensten.json`) met dezelfde velden in camelCase. Beide bronnen worden samengevoegd. |

Als environment variabelen: `NLPORTAL_AUTHENTICATION_MACHTINGSDIENST_CONFIGURATIONS_0_UUID`, `..._0_NAAM`, `..._0_ZAAKTYPES_0`, enzovoort.

### Wanneer geldt een beperking?

* Alleen als de aanvrager een **bedrijf** is (`aanvrager.kvk`). Voor burgers en generieke gebruikers worden machtigingsdiensten genegeerd, met één uitzondering: de producttypen van de product module (zie de tabel hieronder).
* Alleen voor ServiceUUID's die in de configuratie staan. Een onbekende ServiceUUID, of een dienst zonder zaaktypen, beperkt niets.
* Bij meerdere ServiceUUID's worden de toegestane typen van alle diensten samengevoegd.

| Functionaliteit | Effect |
| --------------- | ------ |
| Zaken (lijst) | Alleen zaken van de toegestane zaaktypen. |
| Zaak (detail) | Toegang geweigerd als het zaaktype niet is toegestaan. |
| Taken | Alleen taken waarvan `eigenaar` in `taak-types` staat. |
| Producten en thema's (OpenProduct) | Alleen de toegestane producttypen en de bijbehorende thema's en zaken. |
| Producttypen (product module) | Elke gebruiker met een ServiceUUID claim (ook een burger) krijgt een lege lijst. |
| Documenten, besluiten, klantcontacten | Geen beperking. |

## Lokaal testen

De lokale ontwikkelomgeving (`docker-compose/`) bevat de mappers uit [Keycloak configuratie](#keycloak-configuratie) en twee testgebruikers:

| Gebruikersnaam / wachtwoord | Aanvrager | Gemachtigde | `middel` | Machtigingsdienst |
| --------------------------- | --------- | ----------- | -------- | ----------------- |
| `gemachtigde-burger` | BSN 999993847 (Merel Kooyman) | BSN 999991723 (Lukas Klamer) | `machtigen` | - |
| `gemachtigde-bedrijf` | BSN 999993847 (Merel Kooyman) | KVK 14127293 | `machtigen` | - |

Het wachtwoord is gelijk aan de gebruikersnaam. Beide gebruikers zijn gemachtigd voor dezelfde burger: `gemachtigde-burger` als persoon, `gemachtigde-bedrijf` als organisatie.

Na inloggen zie je de gegevens en zaken van de aanvrager. Om ook de naam van de gemachtigde te zien moet je de workaround uit [Bekende beperkingen](#bekende-beperkingen) toepassen, omdat de lokale omgeving geen Handelsregister heeft. Voor `gemachtigde-bedrijf` (een KVK-gemachtigde) wordt de naam opgehaald uit het Handelsregister; met de dummy URL uit de workaround geeft `getGemachtigdeV2` dan een foutmelding.

Controleren via GraphQL:

```shell
AT=$(curl -s http://localhost:8082/auth/realms/nlportal/protocol/openid-connect/token \
  -d grant_type=password -d client_id=nl-portal -d scope=openid \
  -d username=gemachtigde-burger -d password=gemachtigde-burger \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

curl -s http://localhost:8080/graphql -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
  -d '{"query":"{ getGemachtigdeV2 { persoon { naam { voornamen geslachtsnaam } } } getPersoonV2 { naam { voornamen geslachtsnaam } } }"}'
```

## Bekende beperkingen

### `getGemachtigdeV2` vereist het Handelsregister

De query `getGemachtigdeV2` wordt alleen geregistreerd als de Handelsregister module aan staat **én** de property `nl-portal.config.haalcentraalhr.enabled` op `true` staat. Die laatste wijkt af van de property waarmee de module zelf aangezet wordt (`nl-portal.config.haalcentraal.hr.enabled`). Ontbreekt een van beide, dan geeft de query zonder foutmelding `null` terug, ook als de gemachtigde alleen een BSN heeft. De backend meldt dit bij het opstarten als `Unmapped fields: {Query=[getGemachtigdeV2, ...]}`.

Workaround, bijvoorbeeld lokaal in `docker-compose/imports/backend.env`:

```properties
NLPORTAL_CONFIG_HAALCENTRAAL_HR_ENABLED=true
NLPORTAL_CONFIG_HAALCENTRAAL_HR_PROPERTIES_URL=http://localhost:9999/
NLPORTAL_CONFIG_HAALCENTRAALHR_ENABLED=true
```

Bij een gemachtigde met alleen een BSN wordt het Handelsregister niet aangeroepen, dus een dummy URL volstaat lokaal. In productie configureer je de echte Handelsregister URL.

### Onbekend zaaktype breekt de zakenlijst

De zaaktypen van een machtigingsdienst worden als filter (`zaaktype__in`) naar de Zaken API gestuurd. Open Zaak valideert dat elk zaaktype bestaat. Bevat de configuratie een zaaktype dat niet (meer) bestaat, dan geeft Open Zaak een `400 does_not_exist` en faalt de hele zakenlijst voor die gebruiker, in plaats van alleen de zaken van dat type weg te laten. Controleer daarom na het verwijderen of vervangen van een zaaktype de machtigingsdienst configuratie.

### Acties worden niet aan de gemachtigde toegeschreven

De gemachtigde wordt alleen gebruikt om te tonen wie er handelt. Bij acties wordt nergens vastgelegd dat een gemachtigde ze heeft uitgevoerd:

* **Taak indienen.** De taak wordt `afgerond` en de ingediende gegevens worden opgeslagen in `portaalformulier.verzonden_data`, zonder vermelding van de indiener. De backend zet wel `correctedBy` op de aanvrager (bijvoorbeeld `BSN:999993847`), maar in de Objecten API is `correctedBy` een recordnummer dat de API zelf beheert, dus die waarde wordt genegeerd.
* **Bericht openen.** Het bericht wordt op `geopend` gezet voor de aanvrager. Opent de gemachtigde een bericht, dan ziet de aanvrager het daarna ook als gelezen, zonder te kunnen zien dat iemand anders het heeft gelezen.
* **Overige acties**, zoals documenten uploaden bij een taak (auteur is de aanvrager) en contactgegevens wijzigen in OpenKlant, gebeuren ook volledig op naam van de aanvrager.

Achteraf is dus niet te zien of de aanvrager zelf of een gemachtigde heeft gehandeld.
