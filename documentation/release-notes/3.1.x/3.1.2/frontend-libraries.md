# NL-Portal Frontend Libraries 3.1.2

Deze release bevat alleen beveiligingsupdates van afhankelijkheden.

## Nieuwe Functionaliteit

De volgende afhankelijkheden zijn bijgewerkt om bekende kwetsbaarheden te verhelpen:

* DOMPurify 3.4.9 naar 3.4.17 en Moment.js naar 2.31.0. Beide komen via `@formio/js` mee in
  `@nl-portal/nl-portal-user-interface`. De ranges van `@formio/js` staan deze versies toe, dus ook
  implementaties die de libraries afnemen kunnen ze installeren.
* Een groot aantal build-, test- en ontwikkeltools, waaronder Vite 7.3.7, Vitest 4.1.12 en
  wait-on 9.5.1, en hun transitieve afhankelijkheden. Deze komen niet in de gepubliceerde
  libraries terecht.

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

* `@formio/js` gebruikt Quill 2.0.3, waarvoor een kwetsbaarheid met lage ernst bekend is
  (GHSA-v3m3-f69x-jf25). Er is nog geen versie van Quill met een oplossing.
