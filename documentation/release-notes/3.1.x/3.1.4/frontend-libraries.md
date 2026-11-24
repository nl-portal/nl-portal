# NL-Portal Frontend Libraries 3.1.4

Deze versie bevat geen functionele wijzigingen in de frontend libraries en is uitsluitend uitgebracht
om het versienummer gelijk te houden met NL-Portal Backend Libraries 3.1.4 en NL-Portal App 3.1.4.

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

Er zijn geen bugfixes.

## Breaking changes

Er zijn geen breaking changes.

## Deprecations

* Ogone e-Commerce wordt niet meer ondersteund. Alle Ogone-code wordt verwijderd in 4.0.0, samen
  met de `payment`-module van de backend. Gebruik direct betalen (`payment-direct`). Zie de release
  notes van [NL-Portal Frontend Libraries 3.1.3](../3.1.3/frontend-libraries.md#deprecations).
* De hook `useUpdateProductVerbruiksObjectMutation` en de bijbehorende typen uit
  `@nl-portal/nl-portal-api` worden verwijderd in 4.0.0, samen met de onderliggende GraphQL-mutatie.
  Implementaties die verbruiksobjecten moeten kunnen wijzigen, dienen over te stappen op de
  `openproduct`-module van de backend. Zie de release notes van NL-Portal Frontend Libraries 3.0.5.

## Bekende problemen

* `@formio/js` gebruikt Quill 2.0.3, waarvoor een kwetsbaarheid met lage ernst bekend is
  (GHSA-v3m3-f69x-jf25). Er is nog geen versie van Quill met een oplossing.
