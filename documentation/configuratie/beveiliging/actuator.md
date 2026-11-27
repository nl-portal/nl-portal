# Actuator

Actuator-toegang is opt-in. Standaard is alleen `/actuator/health` bereikbaar (zonder token, voor probes en monitoring);
alle andere actuator-endpoints worden geweigerd. Zet je een JWKS URI, dan zijn ze bereikbaar voor tokens uit die realm
(of tenant) met een van de ingestelde autoriteiten. Zo geef je een systeem, zoals het Configuration Panel, toegang tot
`refresh` en `restart` zonder dat personen die endpoints kunnen aanroepen.

De NL Portal app stelt `health`, `refresh` en `restart` beschikbaar. Stel je in een eigen implementatie meer endpoints
beschikbaar (`management.endpoints.web.exposure.include`, bijvoorbeeld `heapdump`), dan vallen die onder dezelfde
toegangsregels.

## Instellingen

| Env-variabele | Helm-waarde (`nl-portal-backend`) | Standaard | Verplicht |
|---|---|---|---|
| `NLPORTAL_SECURITY_ACTUATOR_JWKSETURI` | `settings.app.security.actuator.jwksURI` | geen: alleen health | om toegang aan te zetten |
| `NLPORTAL_SECURITY_ACTUATOR_ISSUERURI` | `settings.app.security.actuator.issuerURI` | geen: issuer niet gecontroleerd | nee |
| `NLPORTAL_SECURITY_ACTUATOR_AUTHORITIESCLAIMEXPRESSION` | `settings.app.security.actuator.authoritiesClaimExpression` | `['realm_access']['roles']` | nee |
| `NLPORTAL_SECURITY_ACTUATOR_AUTHORITIES` | `settings.app.security.actuator.authorities` | `ROLE_ACTUATOR` | nee |

- `NLPORTAL_SECURITY_ACTUATOR_JWKSETURI`: de JWKS URI van de realm (of tenant) die de tokens van de aanroepende client
  uitgeeft. Dit hoeft niet de realm van de inwoners (`JWKS_URI`) te zijn.
- `NLPORTAL_SECURITY_ACTUATOR_ISSUERURI`: de issuer van die realm (of tenant), precies zoals in de `iss`-claim van het
  token. Is deze gezet, dan worden alleen tokens van deze issuer geaccepteerd. Zet deze als meerdere issuers dezelfde
  signing keys delen, zoals tenants bij Entra ID.
- `NLPORTAL_SECURITY_ACTUATOR_AUTHORITIESCLAIMEXPRESSION`: SpEL-expressie die de autoriteiten uit het token haalt.
- `NLPORTAL_SECURITY_ACTUATOR_AUTHORITIES`: kommagescheiden; een van deze autoriteiten geeft toegang.

## Configureren

1. Registreer bij je identity provider een confidential client voor het systeem dat de actuator aanroept
   (client-credentials grant).
2. Geef die client de autoriteit uit `NLPORTAL_SECURITY_ACTUATOR_AUTHORITIES` (standaard de realm-rol `ROLE_ACTUATOR`
   op het service account). Geef deze rol nooit aan personen.
3. Zet `NLPORTAL_SECURITY_ACTUATOR_JWKSETURI` op de JWKS URI van die realm en, optioneel,
   `NLPORTAL_SECURITY_ACTUATOR_ISSUERURI` op de issuer. Herstart NL Portal.

## Voorbeelden

| Provider / stijl | `..._AUTHORITIESCLAIMEXPRESSION` | `..._AUTHORITIES` | Toekennen aan |
|---|---|---|---|
| Keycloak realm-rol (standaard) | `['realm_access']['roles']` | `ROLE_ACTUATOR` | service account van de client |
| Keycloak client-rol | `['resource_access']['<client>']['roles']` | `ROLE_ACTUATOR` | service account van de client |
| Entra ID app-rol | `['roles']` | `ROLE_ACTUATOR` | app registration van de client |
| OAuth2 scope (Keycloak, Okta, Auth0, Zitadel) | `['scope'].split(' ')` | `nl-portal-actuator` | de client, die de scope aanvraagt |

**Configuration Panel notify.** Het Configuration Panel roept na een wijziging `refresh` of `restart` aan. Wijs de JWKS
URI naar de realm waar de client van het Configuration Panel zijn token ophaalt, en geef die client de autoriteit. Raadpleeg voor de
instellingen aan de kant van het Configuration Panel de documentatie van het Configuration Panel voor de versie die je
gebruikt. Gebruik je de scope-variant, zet dan in het Configuration Panel
`CONFIG_NOTIFY_CLIENT_SCOPE=nl-portal-actuator`.

## Controleren

- `curl <backend>/actuator/health` geeft `200`.
- `curl -X POST <backend>/actuator/refresh` zonder token geeft `401`.
- Hetzelfde met het token van een persoon uit dezelfde realm geeft `403`; met een token uit een andere realm `401`.
- Met een token van de client met de autoriteit geeft het `200`.

## Problemen oplossen

| Symptoom | Oplossing |
|---|---|
| Log bij opstarten: `nl-portal.security.actuator.jwk-set-uri is not set: actuator endpoints other than health are denied` | Verwacht als je actuator-toegang niet gebruikt. Anders: zet `NLPORTAL_SECURITY_ACTUATOR_JWKSETURI`. |
| `401` met een token | Het token komt uit een andere realm dan de JWKS URI. Laat de client zijn token ophalen bij de realm van de JWKS URI. Met `NLPORTAL_SECURITY_ACTUATOR_ISSUERURI`: de `iss`-claim moet er precies mee overeenkomen, inclusief host en poort waarop de client zijn token ophaalt. |
| `403` met een token | Het token heeft de autoriteit niet. Controleer de rol of scope van de client en `..._AUTHORITIESCLAIMEXPRESSION`. |
