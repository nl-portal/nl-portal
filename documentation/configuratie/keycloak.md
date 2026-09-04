# Keycloak configuratie

NL Portal gebruikt Keycloak voor de authenticatie van gebruikers. Deze pagina beschrijft alle vereisten om een Keycloak in te richten voor gebruik met de NL Portal: de token exchange, clients, gebruikersattributen en claims.

Deze documentatie beschrijft de **burger flow** (gebruiker met BSN, ingelogd via DigiD) en de **generieke gebruikersflow** (Keycloak gebruiker zonder BSN of KVK). Voor overige flows, zoals bedrijven (eHerkenning) en machtigingen, verwijzen we naar de broncode of [support](../support-en-resources/community-en-support.md). Zie [Overige flows](#overige-flows).

## Token exchange

Als je een api call doet naar de backend wordt de token onderschept en wordt er een call naar Keycloak gedaan en hier wordt een nieuwe token opgehaald waar de bsn/kvk wel in zit. Deze wordt nu gebruikt in de verdere applicatie.

De token van de browser bevat nooit een BSN of KVK.

### Twee varianten

Vanaf versie 3.1.0 ondersteunt de backend hiervoor twee varianten. Je kiest er één per omgeving; ze combineren niet.

| Variant | Keycloak versie | Configuratiepagina |
| ------- | --------------- | ------------------ |
| Standard token exchange (v2), aanbevolen | 26.2 of nieuwer | [Standard token exchange (v2)](keycloak-token-exchange-v2.md) |
| Legacy token exchange (v1), gedeprecieerd | alle versies | [Legacy token exchange (v1)](keycloak-token-exchange-v1.md) |

De backend staat standaard op v1, zodat bestaande omgevingen ongewijzigd blijven werken. Je schakelt over met de property `nl-portal.authentication.keycloak.token-exchange-version`.

**Kies v2 als je Keycloak 26.2 of nieuwer draait.** De legacy variant is door Keycloak als deprecated gemarkeerd en wordt in een toekomstige Keycloak versie verwijderd.

De belangrijkste reden om over te stappen is niet de deprecation zelf, maar de koppeling met fine-grained admin permissions. De legacy token exchange vereist de feature `admin-fine-grained-authz:v1`. Keycloak staat niet toe dat twee versies van dezelfde feature tegelijk aanstaan, en `KC_FEATURES` geldt voor de hele server. Eén realm die v1 nodig heeft, blokkeert daarmee Fine-Grained Admin Permissions v2 voor **alle** realms op die Keycloak. Keycloak heeft aangegeven geen ondersteuning voor token exchange permissions aan FGAP v2 toe te voegen, dus die situatie lost zichzelf niet op.

Zie de [officiële Keycloak documentatie](https://www.keycloak.org/securing-apps/token-exchange) voor de verschillen tussen beide varianten.

## Clients en backend configuratie

Welke clients je nodig hebt verschilt per variant: v2 heeft er twee, v1 heeft er drie. Dat staat op de betreffende configuratiepagina.

De koppeling met de backend verloopt in beide gevallen via dezelfde environment variabelen van de app image (zie ook de [Deployment guide](deployment-guide.md)):

| Variabele | Betekenis |
| --------- | --------- |
| `KEYCLOAK_CLIENT_ID` | Client id van de m2m (backend) client |
| `KEYCLOAK_CLIENT_SECRET` | Secret van de m2m (backend) client |
| `KEYCLOAK_TOKEN_EXCHANGE_VERSION` | `v1` of `v2`. Standaard `v1` |
| `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` | Bij v1 verplicht: de doelclient. Bij v2 optioneel, meestal leeg |

Optioneel, en aanbevolen bij v2: met `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES` laat je de backend controleren voor welke client een token is uitgegeven. Zonder die instelling accepteert de backend elke token die door hetzelfde realm is ondertekend. Zie [Audience validatie](keycloak-token-exchange-v2.md#audience-validatie-aanbevolen).

## Gebruikerstypen

De backend bepaalt het type gebruiker op basis van claims in de **geëxchangede** token:

1. Claim `aanvrager.bsn` aanwezig → de gebruiker is een **burger** (zie [Gebruikersattributen: burger flow](#gebruikersattributen-burger-flow)).
2. Claim `aanvrager.kvk` aanwezig → de gebruiker is een **bedrijf** (zie [Overige flows](#overige-flows)).
3. Geen van beide → de gebruiker is een **generieke gebruiker** (zie [Generieke gebruikers](#generieke-gebruikers-sub-flow)).

Deze indeling geldt voor beide token exchange varianten.

## Gebruikersattributen: burger flow

Voor een burger stel je het volgende in:

* De gebruiker in het portal realm heeft een **user attribute** `bsn` (bijvoorbeeld `999993847`).
* Er staat een protocol mapper (type *User Attribute*) die het user attribute `bsn` mapt naar de claim `aanvrager.bsn` in het access token. Waar die mapper hoort verschilt per variant: bij v2 in een client scope op de m2m client, bij v1 op de token-exchange client.
* Daarnaast heeft de gebruiker het user attribute `authenticationMethod` met waarde `digid`, dat via de `middel` claim de frontend features bepaalt (zie [De middel claim](#de-middel-claim)).

Een burger heeft daarmee toegang tot onder andere de Mijn Gegevens pagina (BRP gegevens via Haal Centraal) en ziet zaken waarop hij of zij als initiator met dat BSN geregistreerd staat.

## Generieke gebruikers (sub flow)

Een gebruiker zonder `aanvrager.bsn` of `aanvrager.kvk` claim wordt behandeld als generieke Keycloak gebruiker. De backend identificeert deze gebruiker met de eerste 13 karakters van de `sub` claim, met identificatietype `uid`.

Voor generieke gebruikers werkt een deel van de portal functionaliteit:

| Functionaliteit          | Werkt voor generieke gebruiker? | Toelichting                                                                                  |
| ------------------------ | ------------------------------- | -------------------------------------------------------------------------------------------- |
| Zaken                    | Ja                              | Zaken moeten een rol hebben met `betrokkeneIdentificatie.natuurlijkPersoon.anpIdentificatie` gelijk aan de uid (eerste 13 karakters van `sub`). |
| Taken                    | Ja                              | Taakobjecten met `identificatie.type` = `uid` en `identificatie.value` = de uid.             |
| Berichten                | Ja                              | Berichtobjecten met dezelfde uid-identificatie.                                              |
| Mijn Gegevens (BRP)      | Nee                             | Vereist een burger (BSN); de pagina toont geen gegevens.                                     |
| OpenKlant 2 (partijen)   | Nee                             | Vereist een burger of bedrijf; queries geven een foutmelding.                                |

## De middel claim

De frontend leest de claim `middel` uit het access token van de **frontend client** en bepaalt daarmee welke features actief zijn. De standaard app image is geconfigureerd met de volgende authenticatiemethoden (aanpasbaar in een fork, zie `frontend/src/App.tsx` in de NL Portal App repository):

| Categorie | Waarden van `middel`           | Gedrag                                                |
| --------- | ------------------------------ | ----------------------------------------------------- |
| person    | `digid`, `machtigen`           | Persoonsweergave: Mijn Gegevens toont BRP gegevens.   |
| company   | `eherkenning`, `bewindvoering` | Bedrijfsweergave (zie [Overige flows](#overige-flows)). |
| proxy     | `machtigen`, `bewindvoering`   | Machtigingsflow (zie [Overige flows](#overige-flows)). |

Voor de burger flow stel je dit in met:

* Het user attribute `authenticationMethod` met waarde `digid` op de gebruiker.
* Een protocol mapper (type *User Attribute*) op de **frontend client** die het user attribute `authenticationMethod` mapt naar de claim `middel` in het access token.

**Let op:** als de `middel` claim ontbreekt valt de frontend terug op de persoonsweergave. De Mijn Gegevens pagina toont dan alleen gegevens als de gebruiker ook daadwerkelijk een burger is (BSN attribuut én mapper correct ingesteld). Is dat niet het geval, dan toont de pagina een foutmelding.

## Externe identity providers

De attributen `bsn` en `authenticationMethod` zijn **user attributes op de Keycloak gebruiker** in het portal realm. Wanneer gebruikers via een externe identity provider inloggen (bijvoorbeeld Azure AD of DigiD via identity brokering), bestaan deze attributen niet vanzelf. Configureer in dat geval **identity provider mappers** in Keycloak die de attributen bij het inloggen op de gebruiker zetten. De protocol mappers op de clients (zie hierboven) mappen de attributen vervolgens naar de claims.

## Referentieconfiguratie

De docker-compose omgeving in deze repository bevat een volledig werkend voorbeeld van alle bovenstaande configuratie, ingericht volgens de v2 variant. Gebruik deze als referentie bij het inrichten van een eigen Keycloak:

* `docker-compose/docker-compose.yaml`, de Keycloak service. Let op dat er geen `KC_FEATURES` regel in staat: dat is precies wat de v2 variant oplevert.
* `docker-compose/imports/keycloak/nlportal-realm.json`, een volledig realm met de twee clients, de client scope `aanvrager` met de protocol mappers voor `aanvrager.bsn` en `aanvrager.kvk`, de audience mappers op beide clients, en testgebruikers (`burger` met BSN attribuut, `bedrijf` met KVK attribuut).

De bestandsnamen in `docker-compose/imports/keycloak/` eindigen op `-realm.json`. Dat is geen vormvoorkeur: Keycloak gebruikt die naamgeving om de importvolgorde te bepalen wanneer een importmap meerdere realms bevat, en zonder die naamgeving start Keycloak 26 niet op.

## Overige flows

Naast de burger flow en de generieke gebruikersflow ondersteunt de NL Portal ook bedrijven (KVK / eHerkenning) en machtigingsflows (`machtigen`, `bewindvoering`). Deze flows zijn niet in deze documentatie uitgewerkt. Raadpleeg hiervoor de broncode (de module `zgw/common-ground-authentication` in de Backend Libraries repository en de frontend van de NL Portal App repository) of neem contact op via [Community en support](../support-en-resources/community-en-support.md).
