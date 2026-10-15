# Externe Taak

In zaakafhandeling kan het nodig zijn om (aanvullende) informatie op te vragen bij de klant of ketenpartner. Dit kan middels de externe taak. Een taak kan zijn het aanleveren van aanvullende gestructureerde informatie, het aanleveren van documenten (zaakinformatieobjecten) of het doen van een betaling.

Het formaat van een externe taak is gestandaardiseerd in een JSON formaat. De externe taak kan worden aangemaakt via een component naar keuze, in veel gevallen een zaakafhandelcomponent of taakapplicatie.

### Informatieobjecttype bij geüploade documenten

Elk document dat de klant in een taakformulier uploadt via het `portalFileUpload` component, krijgt in de Documenten API een informatieobjecttype. Per upload-component kan worden ingesteld welk informatieobjecttype dat is.

Hiervoor heeft het `portalFileUpload` component de eigenschap `informatieobjecttype`: de URL van een informatieobjecttype in de Catalogi API. Deze eigenschap staat niet in de form builder en wordt direct in de JSON van de formulierdefinitie gezet.

Werking:

* Is `informatieobjecttype` ingesteld, dan stuurt de portal de waarde bij elke upload mee.
* Is `informatieobjecttype` niet ingesteld of leeg, dan gebruikt de backend het standaard informatieobjecttype uit de configuratie van de Documenten API.
* De waarde mag placeholders bevatten in de vorm `{{pad}}`. Op het moment van uploaden vervangt de portal elke placeholder door de waarde op dat pad in de data van het component. Het pad begint zonder `data.`, en voor een component in een container is het relatief ten opzichte van die container. Een placeholder waarvoor geen waarde wordt gevonden, blijft ongewijzigd in de URL staan.
* Deze placeholders worden door de portal zelf als tekst vervangen, niet via de template-interpolatie van Form.io. Ze werken daarom ook binnen een strikte Content Security Policy.

Een vast informatieobjecttype:

```json
{
    "key": "bestanden.partnerID",
    "type": "portalFileUpload",
    "input": true,
    "label": "Een kopie van het identiteitsbewijs van uw partner",
    "informatieobjecttype": "http://localhost:8001/catalogi/api/v1/informatieobjecttypen/36e40d95-e411-4884-b08f-e5864ec8167f"
}
```

Een informatieobjecttype url dat door het zaakafhandelcomponent in de taakdata wordt meegegeven, bijvoorbeeld in het veld `informatieobjecttypeUrl`:

```json
{
    "key": "bestanden.partnerID",
    "type": "portalFileUpload",
    "input": true,
    "label": "Een kopie van het identiteitsbewijs van uw partner",
    "informatieobjecttype": "{{informatieobjecttypeUrl}}"
}
```

### Beschrijving bij geüploade documenten

Documenten die de klant in een taakformulier uploadt via het `portalFileUpload` component, worden opgeslagen als enkelvoudig informatieobject in de Documenten API. Per upload-component kan een beschrijving worden meegegeven die bij elk geüpload document wordt opgeslagen in het veld `beschrijving`.

Hiervoor heeft het `portalFileUpload` component de eigenschap `beschrijvingPath`. Dit is een pad met punten (bijvoorbeeld `bestanden.beschrijvingPartnerID`) naar een waarde in de formulierdata.

Werking:

* Op het moment van uploaden leest de portal de waarde op `beschrijvingPath` uit de formulierdata.
* Alleen als die waarde een niet-lege tekst is, wordt deze als `beschrijving` meegestuurd. Is `beschrijvingPath` niet ingesteld, of is de waarde leeg, alleen spaties of geen tekst, dan wordt er geen beschrijving opgeslagen.
* De beschrijving wordt niet via template-interpolatie (zoals `{{data.veld}}`) bepaald, zodat dit werkt binnen een strikte Content Security Policy. Gebruik ook niet de Form.io-eigenschap `description`; dat is de helptekst van het veld.

Voor een vaste beschrijving voeg je een verborgen veld (`hidden`) toe met een `defaultValue`, en laat je `beschrijvingPath` naar dat veld wijzen. Wil je de beschrijving vanuit het zaakafhandelcomponent (bijvoorbeeld GZAC) laten bepalen, vul dan dat verborgen veld via de taakdata.

```json
{
    "key": "bestanden.beschrijvingPartnerID",
    "type": "hidden",
    "input": true,
    "label": "Beschrijving identiteitsbewijs partner",
    "defaultValue": "Kopie identiteitsbewijs partner",
    "tableView": false
},
{
    "key": "bestanden.partnerID",
    "type": "portalFileUpload",
    "input": true,
    "label": "Een kopie van het identiteitsbewijs van uw partner",
    "beschrijvingPath": "bestanden.beschrijvingPartnerID",
    "multipleFiles": true
}
```

Het verborgen veld is een gewoon formulierveld en wordt dus ook meegestuurd bij het indienen van het formulier.

### Sequence diagram

Er is nog geen sequence diagram beschikbaar.&#x20;

