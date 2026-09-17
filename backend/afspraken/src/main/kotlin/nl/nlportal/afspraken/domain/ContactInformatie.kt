package nl.nlportal.afspraken.domain

data class ContactInformatie(
    val naam: String,
    val emailadres: String,
    val telefoonnummer: String,
    val adres: Adres,
)
