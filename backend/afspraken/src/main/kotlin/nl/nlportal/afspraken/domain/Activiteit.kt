package nl.nlportal.afspraken.domain

data class Activiteit(
    val naamActiviteitsoort: String,
    val aantal: Int,
    val aanduidingWatMeeTeNemen: String?,
)
