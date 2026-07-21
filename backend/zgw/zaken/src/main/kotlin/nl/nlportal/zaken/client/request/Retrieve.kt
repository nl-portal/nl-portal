package nl.nlportal.zaken.client.request

interface Retrieve<T> {
    suspend fun retrieve(): T
}
