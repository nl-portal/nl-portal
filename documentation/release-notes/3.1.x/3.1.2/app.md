# NL-Portal App 3.1.2

Deze release bevat NL-Portal Backend Libraries 3.1.2 en NL-Portal Frontend Libraries 3.1.2.

## Nieuwe Functionaliteit

Deze release bevat alleen beveiligingsupdates van afhankelijkheden, waaronder Spring Boot 3.5.16,
Netty 4.1.139.Final en React Router 7.18.2. Zie de release notes van
[NL-Portal Backend Libraries 3.1.2](backend-libraries.md) en
[NL-Portal Frontend Libraries 3.1.2](frontend-libraries.md).

## Bugfixes

Er zijn geen bugfixes.

## Breaking changes

Er zijn geen breaking changes.

## Deprecations

De legacy token exchange (v1) blijft in 3.x de standaard, maar is door Keycloak als verouderd
gemarkeerd en wordt in een toekomstige Keycloak versie verwijderd. Keycloak voegt geen ondersteuning
voor token exchange permissions toe aan Fine-Grained Admin Permissions v2, dus v1 blijft FGAP v1
vereisen.

## Bekende problemen

* Open source ondersteuning voor Spring Boot 3 is beëindigd. Een aantal kwetsbaarheden in Spring
  Framework, Spring Cloud en Spring Data blijft daarom in 3.x aanwezig. Zie de release notes van
  [NL-Portal Backend Libraries 3.1.2](backend-libraries.md).
* De `messaging`-module publiceert bij het aanmaken van een case een `CreateExternalCaseMessage`
  met de ruwe `submission`. Zie de release notes van
  [NL-Portal Backend Libraries 3.1.2](backend-libraries.md).
