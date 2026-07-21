package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.domain.ResultPage

interface PagedRetrieve<O : PagedRetrieve<O, T>, T> : Retrieve<ResultPage<T>> {
    fun page(page: Int): O

    fun pageSize(pageSize: Int): O

    suspend fun retrieveAll(): List<T> {
        val results = mutableListOf<T>()
        do {
            val result = this.retrieve()
            val next = result.next
            results.addAll(result.results)
        } while (next != null)
        return results
    }
}
