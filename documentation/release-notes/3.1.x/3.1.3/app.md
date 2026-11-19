# NL-Portal App 3.1.3

Deze release bevat NL-Portal Backend Libraries 3.1.3 en NL-Portal Frontend Libraries 3.1.3.

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

* Betalingen zijn gekoppeld aan een open betaaltaak van de ingelogde gebruiker. Bedrag,
  betaalkenmerk en betaalprofiel komen uit de taak. Zie
  [GHSA-cvgw-7m2x-4g74](https://github.com/nl-portal/nl-portal/security/advisories/GHSA-cvgw-7m2x-4g74).
* De status van een betaling wordt alleen nog getoond voor een eigen taak. Zie
  [GHSA-5869-gw29-cgpq](https://github.com/nl-portal/nl-portal/security/advisories/GHSA-5869-gw29-cgpq).
* Bij direct betalen wordt een taak alleen afgerond na een geslaagde betaling van het bedrag van de
  taak.

Zie de release notes van [NL-Portal Backend Libraries 3.1.3](backend-libraries.md).

## Breaking changes

* **Betalen vereist de `taak`-module.** Zonder `nl-portal.config.taak.enabled=true` laden de
  betaalmodules niet. Zie "Migratie".
* **Alleen betalingen voor een taak.** De betaal-API ondersteunt alleen nog een open betaaltaak van
  de ingelogde gebruiker. Betalingen zonder taak vragen een eigen API- en servicelaag in de
  implementatie. Zie "Betalingen zonder taak" in de release notes van
  [NL-Portal Backend Libraries 3.1.3](backend-libraries.md#betalingen-zonder-taak).

## Migratie

Zet de `taak`-module aan als een betaalmodule aan staat:

```shell
NLPORTAL_CONFIG_TAAK_ENABLED=true
```

## Deprecations

* Ogone e-Commerce wordt niet meer ondersteund. Alle Ogone-code wordt verwijderd in 4.0.0: de
  `payment`-module van de backend, de configuratie onder `nl-portal.config.payment.ogone` en
  `USE_LEGACY_OGONE_PAYMENT`, en de Ogone-hooks van de frontend. Gebruik direct betalen
  (`payment-direct`). Zie de release notes van
  [NL-Portal Backend Libraries 3.1.3](backend-libraries.md) en
  [NL-Portal Frontend Libraries 3.1.3](frontend-libraries.md).
* De legacy token exchange (v1) blijft in 3.x de standaard, maar is door Keycloak als verouderd
  gemarkeerd en wordt in een toekomstige Keycloak versie verwijderd. Keycloak voegt geen
  ondersteuning voor token exchange permissions toe aan Fine-Grained Admin Permissions v2, dus v1
  blijft FGAP v1 vereisen.

## Bekende problemen

* Open source ondersteuning voor Spring Boot 3 is beëindigd. Een aantal kwetsbaarheden in Spring
  Framework, Spring Cloud en Spring Data blijft daarom in 3.x aanwezig. Zie de release notes van
  [NL-Portal Backend Libraries 3.1.2](../3.1.2/backend-libraries.md).
* De `messaging`-module publiceert bij het aanmaken van een case een `CreateExternalCaseMessage`
  met de ruwe `submission`. Zie de release notes van
  [NL-Portal Backend Libraries 3.1.2](../3.1.2/backend-libraries.md).
