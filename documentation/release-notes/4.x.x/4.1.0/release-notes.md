# NL-Portal 4.1.0

## Bugfixes

* Form.io-componenten in taakformulieren tonen het label en de beschrijving niet meer dubbel. Dit gold onder andere voor `datetime`, `phoneNumber`, `url`, `currency`, `time`, `address` en `radio`.
* De instelling `hideLabel` (Hide label) werkt nu voor tekstvelden, tekstvakken, datum/tijd, select, adres, radio en selectboxes. Het label wordt verborgen en het veld krijgt een `aria-label`, zodat het voor schermlezers benoemd blijft. Formulieren die `hideLabel` al aan hadden staan, tonen dit label vanaf deze versie dus niet meer.
* Fix voor nullpointer bij uploaden van document als documentType url en/of RSIN in de document API configuratie is null. De beide configuratie properties zijn nu verplichte geworden
* Support voor uploaden naar een document api die niet de default is.
