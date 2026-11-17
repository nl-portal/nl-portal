# NL-Portal App 3.1.2

Deze release bevat NL-Portal Backend Libraries 3.1.2 en NL-Portal Frontend Libraries 3.1.2.

## Nieuwe Functionaliteit

Deze release bevat een beveiligingsoplossing voor de betaalmodules en beveiligingsupdates van
afhankelijkheden, waaronder Spring Boot 3.5.16, Netty 4.1.139.Final en React Router 7.18.2. Zie de release notes van
[NL-Portal Backend Libraries 3.1.2](backend-libraries.md) en
[NL-Portal Frontend Libraries 3.1.2](frontend-libraries.md).

## Bugfixes

* De return URL van een betaling wordt alleen nog gebruikt wanneer hij dezelfde origin heeft als de
  URL in het betaalprofiel. Zie [GHSA-6rxj-f2rp-7mp3](https://github.com/nl-portal/nl-portal/security/advisories/GHSA-6rxj-f2rp-7mp3) en de release notes van
  [NL-Portal Backend Libraries 3.1.2](backend-libraries.md).

## Breaking changes

* **De URL's van betaalprofielen zijn verplicht.** Zonder absolute URL in het betaalprofiel komen
  gebruikers na het betalen niet terug in het portaal. Zie "Migratie".
* **Return URL's op een andere origin.** Een URL in het betaalprofiel op een andere origin dan het
  portaal wint nu altijd van de pagina waar de gebruiker de betaling startte. Zie de release notes van
  [NL-Portal Backend Libraries 3.1.2](backend-libraries.md).

## Migratie

Stel per betaalprofiel de URL van het portaal in, met de origin precies zoals de browser hem toont
(kleine letters, zonder `:443` of `:80`). Vervang `<PROFIEL>` door de naam van het profiel in
hoofdletters, bijvoorbeeld `BELASTINGZAKEN`.

```shell
# Direct betalen, als NLPORTAL_CONFIG_PAYMENT_DIRECT_ENABLED=true
NLPORTAL_CONFIG_PAYMENT_DIRECT_PROPERTIES_CONFIGURATIONS_<PROFIEL>_RETURNURL=https://portal.example.nl

# Ogone, als NLPORTAL_CONFIG_PAYMENT_OGONE_ENABLED=true
NLPORTAL_CONFIG_PAYMENT_OGONE_PROPERTIES_CONFIGURATIONS_<PROFIEL>_SUCCESSURL=https://portal.example.nl
NLPORTAL_CONFIG_PAYMENT_OGONE_PROPERTIES_CONFIGURATIONS_<PROFIEL>_FAILUREURL=https://portal.example.nl
```

Gebruik je de Helm chart, zie dan de release notes van `nl-portal-backend` chart 3.2.1 in de
[helm-charts repository](https://github.com/nl-portal/helm-charts).

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
