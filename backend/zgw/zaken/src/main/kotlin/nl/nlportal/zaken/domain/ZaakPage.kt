package nl.nlportal.zaken.domain

import nl.nlportal.graphql.Page

class ZaakPage(
    number: Int,
    size: Int,
    content: List<Zaak>,
    totalElements: Int,
) : Page<Zaak>(number, size, content, totalElements) {
    companion object {
        fun fromResultPage(
            pageNumber: Int,
            pageSize: Int,
            resultPage: ResultPage<Zaak>,
        ): ZaakPage {
            val zaken = resultPage.results.map { it }

            return ZaakPage(
                number = pageNumber,
                size = pageSize,
                content = zaken,
                totalElements = resultPage.count,
            )
        }
    }
}
