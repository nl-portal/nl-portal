package nl.nlportal.afspraken.domain

data class AfsprakenResultaat(
    val afspraken: List<AfspraakOverzicht>,
    val totalCount: Int,
)
