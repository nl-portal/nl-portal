# NL-Portal Frontend Libraries 3.1.0

Deze versie bevat geen functionele wijzigingen in de frontend libraries en is uitsluitend uitgebracht
om het versienummer gelijk te houden met NL-Portal Backend Libraries 3.1.0 en NL-Portal App 3.1.0.

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

Er zijn geen bugfixes.

## Breaking changes

Er zijn geen breaking changes.

## Deprecations

* De hook `useUpdateProductVerbruiksObjectMutation` en de bijbehorende typen uit
  `@nl-portal/nl-portal-api` worden verwijderd in 4.0.0, samen met de onderliggende GraphQL-mutatie.
  Implementaties die verbruiksobjecten moeten kunnen wijzigen, dienen over te stappen op de
  `openproduct`-module van de backend. Zie de release notes van NL-Portal Frontend Libraries 3.0.5.

## Bekende problemen

Er zijn geen bekende problemen.
