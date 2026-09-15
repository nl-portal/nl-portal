# NL-Portal Backend Libraries 3.1.1

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

De volgende bugs zijn opgelost:

* De property `nl-portal.config.zakenapi.properties.use-nnp-kvk-query-identificators` kon niet
  worden gebonden. Zodra de property een waarde kreeg, faalde het binden van de configuratie van de
  `zakenapi` module en startte de applicatie niet op. De schakelaar was daarmee in 3.0.x en 3.1.0
  niet te gebruiken: aan zetten haalde de portal onderuit in plaats van het gedrag te wijzigen.

  De property is nu gewoon te zetten. Een omgeving die de property niet zet verandert hier niet
  door, want de standaardwaarde blijft `false`.

## Breaking changes

Er zijn geen breaking changes.

## Deprecations

De legacy token exchange (v1) blijft in 3.x de standaard, maar is door Keycloak als verouderd
gemarkeerd en wordt in een toekomstige Keycloak versie verwijderd. Keycloak voegt geen ondersteuning
voor token exchange permissions toe aan Fine-Grained Admin Permissions v2, dus v1 blijft FGAP v1
vereisen.

## Bekende problemen

Er zijn geen bekende problemen.
