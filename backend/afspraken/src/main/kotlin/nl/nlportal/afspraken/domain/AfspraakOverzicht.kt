package nl.nlportal.afspraken.domain

data class AfspraakOverzicht(
    val afspraakIdentificatie: String,
    val onderwerp: String,
    val geplandAanvangsmoment: String,
    val geplandEindmoment: String,
)
