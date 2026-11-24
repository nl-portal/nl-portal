# NL-Portal App 3.1.4

Deze release bevat NL-Portal Backend Libraries 3.1.4 en NL-Portal Frontend Libraries 3.1.4.

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

* De container-images zijn bijgewerkt naar nieuwe base images met beveiligingsupdates. Het
  backend-image draait op `eclipse-temurin:21-jre-noble` (Ubuntu 24.04) en het frontend-image op
  `nginx/nginx-unprivileged:1.31.9-alpine-slim`.

## Breaking changes

* **Alleen als je een eigen image bouwt op basis van het backend-image:** dat is nu Ubuntu in plaats
  van Alpine. Gebruik `apt-get` in plaats van `apk`.

## Migratie

Er is geen migratie nodig.

## Deprecations

* Ogone e-Commerce wordt niet meer ondersteund. Alle Ogone-code wordt verwijderd in 4.0.0: de
  `payment`-module van de backend, de configuratie onder `nl-portal.config.payment.ogone` en
  `USE_LEGACY_OGONE_PAYMENT`, en de Ogone-hooks van de frontend. Gebruik direct betalen
  (`payment-direct`). Zie de release notes van
  [NL-Portal Backend Libraries 3.1.3](../3.1.3/backend-libraries.md) en
  [NL-Portal Frontend Libraries 3.1.3](../3.1.3/frontend-libraries.md).
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
