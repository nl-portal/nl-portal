# NL-Portal Backend Libraries 3.1.2

Deze release bevat alleen beveiligingsupdates van afhankelijkheden.

## Nieuwe Functionaliteit

De volgende afhankelijkheden zijn bijgewerkt om bekende kwetsbaarheden te verhelpen:

* Spring Boot 3.5.9 naar 3.5.16, met daarin onder andere Spring Framework 6.2.19 en Spring Security
  6.5.11.
* Jackson 2.19.4 naar 2.21.8.
* Netty 4.1.130.Final naar 4.1.139.Final.
* PostgreSQL JDBC driver 42.7.8 naar 42.7.14.
* Apache HttpClient 5.5.1 naar 5.6.5 en HttpCore 5.3.6 naar 5.4.4.
* Log4j API 2.24.3 naar 2.25.5.
* Apache Tika 3.1.0 naar 3.3.3.
* Spring Cloud Config 4.3.0 naar 4.3.4, Spring Cloud Bootstrap en Spring Cloud Stream 4.3.0 naar
  4.3.3.
* Apache Commons Lang 3.17.0 naar 3.20.0.

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

* Open source ondersteuning voor Spring Boot 3 (Spring Framework 6.2) is beëindigd. Een aantal
  kwetsbaarheden is alleen opgelost in commerciële versies en blijft daarom in 3.x aanwezig:
  * Spring Framework CVE-2026-47884, CVE-2026-47890, CVE-2026-47891, CVE-2026-47892, CVE-2026-59283
    en CVE-2026-59313. Deze vereisen `XsltView`, server-sent events, de Aalto XML processor,
    functional endpoints of de SpEL compiler. NL-Portal gebruikt geen van deze.
  * Spring Cloud Config CVE-2026-47836 en CVE-2026-47837. Deze zitten in Spring Cloud Config Server;
    NL-Portal gebruikt alleen de client.
  * Daarnaast een aantal kwetsbaarheden met een lagere score in Spring Framework, Spring Cloud en
    Spring Data. De volledige lijst staat in de pull request van deze release.

  Gebruik je deze onderdelen zelf in je implementatie, stap dan over op 4.x.
* Kotlin CVE-2026-53914 zit in de deserialisatie van build cache metadata tijdens het bouwen, niet
  in de Kotlin runtime libraries. De oplossing vereist Kotlin 2.4.20 en valt buiten
  deze patch release.
* De `messaging`-module publiceert bij het aanmaken van een case een `CreateExternalCaseMessage` (destination `create-external-case`) met de **ruwe** `submission` zoals de gebruiker die heeft ingediend, en niet de versie die tegen het JSON-schema van de case definition is gefilterd en gevalideerd. Dit is bewust: afnemers van de libraries kunnen use cases hebben die wij niet kunnen voorzien. Verwerk je deze berichten, bijvoorbeeld om een zaak aan te maken? Valideer de `submission` dan zelf tegen het schema van de case definition (`caseDefinitionId`) en negeer velden die niet in het schema staan. Vanaf 4.0.0 is de `messaging`-module verwijderd.
