package nl.nlportal.afspraken.domain

data class AfspraakDetail(
    val afspraakIdentificatie: String,
    val afspraakSoort: String,
    val onderwerp: String,
    val geplandAanvangsmoment: String,
    val geplandEindmoment: String,
    val aanmeldkenmerkAlfanumeriek: String?,
    val onlineAdres: String?,
    val afspraakLocatie: AfspraakLocatie?,
    val activiteiten: List<Activiteit>,
    val contactInformatie: ContactInformatie,
)
