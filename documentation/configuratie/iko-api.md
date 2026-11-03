# IKO API

De backend-module `iko-api` is de API-module voor communicatie met IKO (Integraal Klant- en Objectbeeld). Deze eerste versie ondersteunt alleen het lezen van Aggregated Data Profiles (ADP's) voor de ingelogde burger of het ingelogde bedrijf.

Het is een interne API-module, net als `zgw:zaken-api`: er komt geen GraphQL-query of REST-endpoint bij. Feature-modules gebruiken de module om IKO-data op te halen. De eerste afnemer wordt templated pages (nl-portal-issues#170).

De module staat standaard uit en zit niet in de standaard app-images.

## Configuratie

| Property | Env var | Betekenis |
|----------|---------|-----------|
| `nl-portal.config.ikoapi.enabled` | `NLPORTAL_CONFIG_IKOAPI_ENABLED` | `true` om de module aan te zetten (standaard `false`) |
| `nl-portal.config.ikoapi.properties.url` | `NLPORTAL_CONFIG_IKOAPI_PROPERTIES_URL` | Basis-URL van IKO |
| `nl-portal.config.ikoapi.properties.token-url` | `NLPORTAL_CONFIG_IKOAPI_PROPERTIES_TOKENURL` | Token-endpoint van de Keycloak-realm van IKO |
| `nl-portal.config.ikoapi.properties.client-id` | `NLPORTAL_CONFIG_IKOAPI_PROPERTIES_CLIENTID` | Client van NL Portal in de IKO-realm |
| `nl-portal.config.ikoapi.properties.client-secret` | `NLPORTAL_CONFIG_IKOAPI_PROPERTIES_CLIENTSECRET` | Secret van die client |
| `nl-portal.config.ikoapi.properties.allowed-profiles` | `NLPORTAL_CONFIG_IKOAPI_PROPERTIES_ALLOWEDPROFILES` | Kommagescheiden namen van de ADP's die NL Portal mag opvragen. Standaard leeg: elk profiel wordt dan geweigerd |

Met de module aan zijn `url`, `token-url`, `client-id` en `client-secret` verplicht. Ontbreekt er een, dan start de applicatie niet en meldt `nl-portal.config.ikoapi.properties.<naam> is required`. Lege waarden worden bij het opstarten niet afgevangen.

`token-url` is het token-endpoint van de realm: `<keycloak basis-URL>/realms/<IKO-realm>/protocol/openid-connect/token`. De basis-URL bevat alleen `/auth` als Keycloak met dat relatieve pad draait.

```yaml
nl-portal:
    config:
        ikoapi:
            enabled: true
            properties:
                url: https://iko.example.nl
                token-url: https://keycloak.example.nl/realms/iko/protocol/openid-connect/token
                client-id: nl-portal
                client-secret: ${NLPORTAL_CONFIG_IKOAPI_PROPERTIES_CLIENTSECRET}
                allowed-profiles: personen
```

## Keycloak-voorwaarden (IKO-realm)

Dit gebeurt in de Keycloak-realm die IKO vertrouwt. Die realm heeft al de client `iko` van IKO zelf.

1. Maak een confidential client `nl-portal` aan: client authentication aan, **service accounts aan**, standard flow en direct access grants uit.
2. Maak op de client `iko` een client role aan (bijvoorbeeld `ROLE_NL_PORTAL`) en ken die toe aan de service account-gebruiker `service-account-nl-portal` (client `nl-portal`, tab *Service accounts roles*).
3. Voeg op `nl-portal` een **audience mapper** (`oidc-audience-mapper`) toe met included client audience `iko` en *Add to access token* aan. IKO accepteert alleen tokens met `aud` `iko`.
4. Zorg dat de client scope `roles` een **default** scope van `nl-portal` is, zodat `resource_access.iko.roles` in het token staat. IKO leest de autorisaties uit die claim.
5. Voeg de role toe aan de roles van elk ADP dat NL Portal mag lezen (IKO admin-UI, of de kolom `aggregated_data_profile.roles`).

Een token controleren:

```shell
curl -s -d grant_type=client_credentials -u nl-portal:<secret> <token-url> \
  | jq -r .access_token | jq -R 'split(".")[1] | @base64d | fromjson'
```

`aud` moet `iko` bevatten en `resource_access.iko.roles` de role uit stap 2. De claim `iss` moet gelijk zijn aan de issuer waarmee de resource server van IKO is ingesteld (`spring.security.oauth2.resourceserver.jwt.issuer-uri`). Keycloak bepaalt `iss` op basis van de hostnaam waarop het token is opgevraagd, dus `token-url` moet dezelfde hostnaam gebruiken als de issuer-instelling van IKO.

## Beveiligingsmodel

* NL Portal roept IKO aan als **service account**: een client-credentials-token uit de eigen Keycloak-realm van IKO. Alle gebruikers delen één token, dat in het geheugen wordt bewaard en 60 seconden voor het verloopt wordt vernieuwd. Het token van de burger gaat nooit naar IKO.
* Omdat IKO gebruikers niet van elkaar kan onderscheiden, komt de parameter `id` altijd uit de authenticatie (BSN voor een burger, KVK-nummer voor een bedrijf), nooit uit invoer van de aanroeper.
* `allowed-profiles` is de allowlist van NL Portal. Voor andere namen wordt IKO niet aangeroepen.
* De `roles` van elk ADP in IKO zijn de allowlist van IKO. NL Portal kan alleen ADP's lezen die de role uit stap 2 bevatten.

## Gebruik vanuit een feature-module

Voeg de module toe als afhankelijkheid:

```kotlin
dependencies {
    api(project(":iko-api"))
}
```

Injecteer `IkoApiService` en roep `getAggregatedDataProfile` aan:

```kotlin
class MijnFeatureService(
    private val ikoApiService: IkoApiService,
) {
    suspend fun personen(authentication: CommonGroundAuthentication): JsonNode? =
        ikoApiService.getAggregatedDataProfile(
            authentication,
            "personen",
            listOf(IkoContainerParam("zaken", PageRequest.of(0, 10))),
        )
}
```

* Het resultaat is de JSON van IKO, ongewijzigd, als Jackson `JsonNode`.
* `null` als de gebruiker geen BSN of KVK-nummer heeft, of als IKO 404 geeft.
* `IkoApiException` als de naam niet in `allowed-profiles` staat, of als IKO een andere fout geeft. De melding bevat de `correlationId` van IKO, nooit de foutmelding van IKO zelf.

Een feature die IKO gebruikt, gebruikt de beans alleen als `nl-portal.config.ikoapi.enabled` aan staat. Zonder die property bestaan `IkoApiService` en `IkoApiClient` niet.
