# NL-Portal Backend Libraries 3.1.3

Deze release bevat beveiligingsoplossingen voor de betaalmodules.

## Nieuwe Functionaliteit

Er is geen nieuwe functionaliteit.

## Bugfixes

* Een betaling via `doDirectPayment` (`payment-direct`) of `generateOgonePayment` (`payment`) was
  niet gekoppeld aan een taak. Bedrag, betaalkenmerk en order-id kwamen ongecontroleerd uit het
  verzoek van de client. Daarmee kon een taak met een lager bedrag worden betaald, of een betaling
  worden gestart voor de taak van een andere gebruiker. Een betaling is nu alleen mogelijk voor een
  open betaaltaak van de ingelogde gebruiker: het `orderId` moet het id van die taak zijn en het
  betaalprofiel (`identifier` of `pspId`) moet bij de `pspid` van de taak horen. Bedrag,
  betaalkenmerk en order-id komen uit de taak, bij Ogone ook de titel. Zie
  [GHSA-cvgw-7m2x-4g74](https://github.com/nl-portal/nl-portal/security/advisories/GHSA-cvgw-7m2x-4g74).
* `getDirectPaymentStatus` gaf de status van elke hosted checkout terug, ook van andere gebruikers.
  De status wordt nu alleen teruggegeven wanneer de checkout bij een taak van de ingelogde gebruiker
  hoort en de `pspid` van die taak bij het betaalprofiel past. Zie
  [GHSA-5869-gw29-cgpq](https://github.com/nl-portal/nl-portal/security/advisories/GHSA-5869-gw29-cgpq).
* De webhook van `payment-direct` rondt een taak alleen nog af na een geslaagde betaling van het
  bedrag van de taak: bij status 9 (betaald), wanneer de `merchantId` bij het betaalprofiel van de
  taak hoort en het bedrag in centen gelijk is aan het bedrag dat de checkout voor de taak rekent.
  Statussen 91, 92 en 93 ronden de taak niet meer af; Worldline stuurt een nieuwe webhook zodra de
  betaling status 9 krijgt.

## Breaking changes

* **De betaal-API ondersteunt alleen betaaltaken.** `doDirectPayment` en `generateOgonePayment`
  geven een foutmelding wanneer het `orderId` geen open betaaltaak van de ingelogde gebruiker is.
  Zie "Betalingen zonder taak".
* **`payment-direct` en `payment` vereisen de `taak`-module.** Beide modules laden alleen nog met
  `nl-portal.config.taak.enabled=true`. Zie "Migratie".
* **Waarden van de client worden genegeerd.** `amount`, `reference` en (Ogone) `title` uit het
  verzoek worden niet meer gebruikt.
* **`getDirectPaymentStatus` vereist een taak.** Een checkout zonder betaalpoging, of een checkout
  waarvan de `merchantReference` geen taak van de gebruiker is, geeft een foutmelding.
* **Afronden bij `payment-direct` gebeurt later.** Een taak blijft open totdat de betaling status 9
  heeft. Was een betaling eerst nog in behandeling (91), dan wordt de taak pas afgerond bij de
  webhook met status 9.

## Migratie

Zet de `taak`-module aan als `payment-direct` of `payment` aan staat:

```yaml
nl-portal:
  config:
    taak:
      enabled: true
```

Of als environment variable:

```shell
NLPORTAL_CONFIG_TAAK_ENABLED=true
```

### Betalingen zonder taak

De API van `payment-direct` en `payment` (de GraphQL-velden, de webhook en de postsale) ondersteunt
alleen betaaltaken. Dat is de enige betaalfunctionaliteit die NL-Portal standaard levert. Voor andere
betalingen bouwt een implementatie per use case een eigen API- en servicelaag op de interne
servicelaag van de betaalmodules (`DirectPaymentService`, `OgonePaymentService`). Die eigen laag:

* **controleert eigenaarschap**: de betaling hoort bij de ingelogde gebruiker of bij de partij
  namens wie die handelt. Dat geldt ook voor het opvragen van de betaalstatus;
* **valideert de betaling**: bedrag, betaalkenmerk, order-id en betaalprofiel komen uit de eigen
  gegevens van de use case, niet uit het verzoek van de client;
* **verwerkt de callbacks**: de webhook of postsale rondt de betaling alleen af bij een geslaagde,
  definitieve status, een bedrag gelijk aan het verwachte bedrag en het juiste betaalprofiel.

De interne servicelaag doet geen van deze controles. Een eigen API die waarden van de client
ongecontroleerd doorgeeft, is kwetsbaar voor
[GHSA-cvgw-7m2x-4g74](https://github.com/nl-portal/nl-portal/security/advisories/GHSA-cvgw-7m2x-4g74)
en [GHSA-5869-gw29-cgpq](https://github.com/nl-portal/nl-portal/security/advisories/GHSA-5869-gw29-cgpq).

Daarbij geldt:

* De bestaande GraphQL-velden kunnen niet worden vervangen. Geef de eigen API eigen namen.
* De webhook en de postsale ronden alleen taken af. Wie ook andere betalingen via deze callbacks
  ontvangt, vervangt `DirectPaymentService` of `OgonePaymentService` (of de bijbehorende controller)
  met een eigen bean, en laat betaaltaken via de bestaande afhandeling lopen.
* De services bestaan alleen met `nl-portal.config.taak.enabled=true`. Zonder de `taak`-module maakt
  de implementatie de beans zelf aan.

De standaard frontend stuurt al het id van de taak als `orderId` mee en hoeft niet te worden
aangepast.

Controleer na de update of betaaltaken na een geslaagde betaling worden afgerond. Blijft een taak
open, zoek dan in de log naar `Merchant does not match the task` of
`Amount is not the same as the amount in the task`.

## Deprecations

* Ogone e-Commerce wordt niet meer ondersteund. Alle Ogone-code wordt verwijderd in 4.0.0. Gebruik
  de `payment-direct`-module. Het gaat om:
  * de volledige `payment`-module (`nl.nl-portal:payment`), met alle klassen in
    `nl.nlportal.payment`. Deze zijn in 3.1.3 gemarkeerd met `@Deprecated`;
  * de GraphQL-mutatie `generateOgonePayment` en de typen `OgonePayment`, `OgonePaymentRequestInput`
    en `PaymentField`;
  * het postsale-endpoint `/api/public/payment/ogone/postsale`;
  * de configuratie onder `nl-portal.config.payment.ogone`.

  Het veld `ogonebetaling` van een taak en het taaksoort `ogonebetaling` blijven bestaan. Die
  worden ook door `payment-direct` gebruikt.
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
