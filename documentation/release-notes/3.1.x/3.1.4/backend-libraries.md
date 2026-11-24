# NL-Portal Backend Libraries 3.1.4

Deze versie bevat geen functionele wijzigingen in de backend libraries en is uitsluitend uitgebracht
om het versienummer gelijk te houden met NL-Portal Frontend Libraries 3.1.4 en NL-Portal App 3.1.4.

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

Er zijn geen bugfixes.

## Breaking changes

Er zijn geen breaking changes.

## Deprecations

* Ogone e-Commerce wordt niet meer ondersteund. Alle Ogone-code wordt verwijderd in 4.0.0. Gebruik
  de `payment-direct`-module. Zie de release notes van
  [NL-Portal Backend Libraries 3.1.3](../3.1.3/backend-libraries.md#deprecations).
* De legacy token exchange (v1) blijft in 3.x de standaard, maar is door Keycloak als verouderd
  gemarkeerd en wordt in een toekomstige Keycloak versie verwijderd. Keycloak voegt geen
  ondersteuning voor token exchange permissions toe aan Fine-Grained Admin Permissions v2, dus v1
  blijft FGAP v1 vereisen.

## Bekende problemen

* Open source ondersteuning voor Spring Boot 3 (Spring Framework 6.2) is beëindigd. Een aantal
  kwetsbaarheden is alleen opgelost in commerciële versies en blijft daarom in 3.x aanwezig. Zie de
  release notes van [NL-Portal Backend Libraries 3.1.2](../3.1.2/backend-libraries.md).
* De `messaging`-module publiceert bij het aanmaken van een case een `CreateExternalCaseMessage`
  met de ruwe `submission`. Zie de release notes van
  [NL-Portal Backend Libraries 3.1.2](../3.1.2/backend-libraries.md).
