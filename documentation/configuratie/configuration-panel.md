# Configuration Panel

Het Configuration Panel is een beheerapplicatie waarmee je de NL Portal backend en theming kunt configureren via een gebruiksvriendelijke interface — zonder code aan te passen of eigen images te bouwen.

Zie de [nl-portal-configuration-panel repository](https://github.com/nl-portal/nl-portal-configuration-panel) voor installatie-instructies, environment variabelen en versiecompatibiliteit.

## Wat kun je configureren

### Backend modules

Via het Configuration Panel kun je alle backend modules in- en uitschakelen en configureren:

| Module | Beschrijving |
|--------|--------------|
| Zaken API | Koppeling met Open Zaak voor zaken |
| Catalogi API | Zaaktypen en informatieobjecttypen |
| Documenten APIs | Documentopslag (meerdere configuraties mogelijk) |
| Besluiten API | Besluiten bij zaken |
| Open Object | Objectregistratie voor taken en berichten |
| OpenKlant 2 | Klantgegevens en contactmomenten |
| HaalCentraal BRP v2 | Persoonsgegevens uit de BRP |
| HaalCentraal HR | Bedrijfsgegevens uit het Handelsregister |
| Taak | Externe taken via Open Object |
| Berichten | Berichtenfunctionaliteit |
| Product | Productweergave |
| OpenProduct | Koppeling met OpenProduct API |
| DMN | Decision Model and Notation |
| Prefill | Voorinvullen van formulieren |
| Payment (Ogone/Direct) | Betalingsintegraties |
| ClamAV | Virusscanning van uploads |

### Theming

Het Configuration Panel biedt ook theming-opties:

* **Logo** — upload een eigen logo voor de header
* **Design tokens** — pas kleuren, typografie en andere stijlen aan via een CSS-editor

Zie [Eigen vormgeving](eigen-vormgeving.md) voor meer informatie over design tokens.

## Uitproberen via de demo-omgeving

Het Configuration Panel heeft een eigen docker-compose omgeving in de [Configuration Panel repository](https://github.com/nl-portal/nl-portal-configuration-panel). Start die eerst. Haal daarna in `docker-compose/imports/backend.env` van de monorepo het commentaar weg bij de regels onder **Configuration Panel** (en voor notify bij `NLPORTAL_SECURITY_ACTUATOR_JWKSETURI`), en start de docker-compose omgeving van de monorepo met `NETWORK=configuration-panel`, zodat NL Portal bij het opstarten zijn configuratie uit het Configuration Panel haalt. Zie "Side by side with the Configuration Panel" in [`docker-compose/README.md`](https://github.com/nl-portal/nl-portal/blob/main/docker-compose/README.md#side-by-side-with-the-configuration-panel) voor de stappen.

Het Configuration Panel is daarna bereikbaar op `http://localhost:3001` met gebruikersnaam **admin** en wachtwoord **admin**.

## NL Portal aansluiten op het Configuration Panel

De NL Portal backend libraries hebben ingebouwde ondersteuning voor het Configuration Panel.

**Beveiliging:** het Configuration Panel bevat gevoelige configuratie zoals API-tokens en secrets. Maak het Configuration Panel nooit publiek toegankelijk. Zowel de webapp als de config server endpoints mogen alleen bereikbaar zijn via interne netwerken (bijv. Kubernetes cluster-netwerk, localhost) of via een beveiligde gateway met authenticatie.

Configureer de volgende environment variabelen op de **NL Portal backend**:

| Variabele | Beschrijving |
|-----------|--------------|
| `CONFIGURATION_PANEL_ENABLED` | Zet op `true` om de integratie te activeren (default: `false`) |
| `CONFIGURATION_PANEL_URI` | URL van het Configuration Panel, bijv. `http://config-panel:8090/configuration` |
| `CONFIGURATION_PANEL_TOKEN` | Token voor authenticatie (moet overeenkomen met `CONFIG_SERVER_TOKEN` op het Configuration Panel) |
| `CONFIGURATION_PANEL_APPLICATION_NAME` | Naam van de applicatie, bijv. `nl-portal-app` |

Voorbeeld:

```shell
CONFIGURATION_PANEL_ENABLED=true
CONFIGURATION_PANEL_URI=http://configuration-panel:8090/configuration
CONFIGURATION_PANEL_TOKEN=VerySecretToken
CONFIGURATION_PANEL_APPLICATION_NAME=nl-portal-app
CONFIGURATION_PANEL_ENCRYPTION_KEY=<CONFIG_ENCRYPTION_KEY van het Configuration Panel>
```

De NL Portal backend haalt bij opstarten de configuratie op uit het Configuration Panel. Waarden uit het Configuration Panel hebben voorrang op environment variabelen.

* [Versleuteling](configuration-panel-versleuteling.md): het Configuration Panel levert waarden versleuteld aan; NL Portal heeft dezelfde sleutel nodig (`CONFIGURATION_PANEL_ENCRYPTION_KEY`).
* Notify: de backend laadt de configuratie altijd bij het opstarten. Wil je dat wijzigingen direct doorkomen, dan roept het Configuration Panel na een wijziging `refresh` of `restart` aan. Daarvoor moet [actuator-toegang](beveiliging/actuator.md) aan staan voor de client van het Configuration Panel. Zonder actuator-toegang werkt het Configuration Panel ook; wijzigingen gelden dan vanaf de volgende herstart.
* De instellingen van het Configuration Panel zelf staan in de documentatie van het Configuration Panel voor de versie die je gebruikt.

## Helm charts

Voor Kubernetes-omgevingen zijn er Helm charts beschikbaar (`nl-portal-configpanel-backend` en `nl-portal-configpanel-frontend`). Zie de [helm-charts repository](https://github.com/nl-portal/helm-charts) voor installatie-instructies.
