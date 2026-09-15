# NL-Portal App 3.1.1

Deze release bevat NL-Portal Backend Libraries 3.1.1 en NL-Portal Frontend Libraries 3.1.1.

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

De volgende bugs zijn opgelost:

* De instelling `use-nnp-kvk-query-identificators` van de `zakenapi` module kon niet worden gezet.
  Zodra de instelling een waarde kreeg, faalde het binden van de configuratie en startte de
  applicatie niet op, in Kubernetes een crash loop. De schakelaar was daarmee in 3.0.x en 3.1.0 niet
  te gebruiken: aan zetten haalde de portal onderuit in plaats van het gedrag te wijzigen.

  Dit speelt vooral bij configuratie via het
  [Configuration Panel](../../../configuratie/configuration-panel.md), waar de wijziging pas bij de
  eerstvolgende herstart tot een falende start leidt. Dezelfde instelling via de environment
  variabele `NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_USENNPKVKQUERYIDENTIFICATORS` gaf hetzelfde
  resultaat.

  De instelling is nu gewoon te zetten. Een omgeving die de instelling niet zet verandert hier niet
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
