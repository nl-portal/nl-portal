/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.http.codec.DecoderHttpMessageReader
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.HttpMessageReader
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import java.io.File

/**
 * Guards that Jackson 2 on the runtime classpath (pulled in by jackson-jq) changes nothing:
 * no Jackson 2 ObjectMapper bean, only Jackson 3 codecs on the server and on WebClient,
 * and responses identical to snapshots taken before Jackson 2 was on the classpath.
 *
 * A missing snapshot is written on the first run.
 */
@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureWebTestClient(timeout = "30000")
@TestInstance(PER_CLASS)
@Tag("integration")
class JacksonCoexistenceTest {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var serverCodecConfigurer: ServerCodecConfigurer

    @Autowired
    lateinit var webClientBuilder: WebClient.Builder

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `no Jackson 2 ObjectMapper bean`() {
        val jackson2ObjectMapper = runCatching { Class.forName("com.fasterxml.jackson.databind.ObjectMapper") }.getOrNull()

        if (jackson2ObjectMapper != null) {
            assertEquals(emptyList<String>(), applicationContext.getBeanNamesForType(jackson2ObjectMapper).toList())
        }
    }

    @Test
    fun `server codecs are Jackson 3 only`() {
        assertJackson3Only(serverCodecConfigurer.readers, serverCodecConfigurer.writers)
    }

    @Test
    fun `WebClient codecs are Jackson 3 only`() {
        var readers: List<HttpMessageReader<*>> = emptyList()
        var writers: List<HttpMessageWriter<*>> = emptyList()
        webClientBuilder
            .clone()
            .codecs {
                readers = it.readers
                writers = it.writers
            }.build()

        assertJackson3Only(readers, writers)
    }

    @Test
    fun `GraphQL response matches snapshot`() {
        val body =
            webTestClient
                .post()
                .uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"query":"{ verificatieConfig { enabled typesNeedVerification } }"}""")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

        assertSnapshot("graphql-verificatie-config", body)
    }

    @Test
    fun `public frontend configuration matches snapshot`() {
        val body =
            webTestClient
                .get()
                .uri("/api/public/frontend")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

        assertSnapshot("public-frontend", body)
    }

    private fun assertJackson3Only(
        readers: List<HttpMessageReader<*>>,
        writers: List<HttpMessageWriter<*>>,
    ) {
        val codecs =
            readers.mapNotNull { (it as? DecoderHttpMessageReader<*>)?.decoder?.javaClass?.simpleName } +
                writers.mapNotNull { (it as? EncoderHttpMessageWriter<*>)?.encoder?.javaClass?.simpleName }

        assertTrue(codecs.contains("JacksonJsonDecoder"), "No JacksonJsonDecoder in $codecs")
        assertTrue(codecs.contains("JacksonJsonEncoder"), "No JacksonJsonEncoder in $codecs")
        assertFalse(codecs.any { it.startsWith("Jackson2") }, "Jackson 2 codec in $codecs")
    }

    private fun assertSnapshot(
        name: String,
        actual: String,
    ) {
        val file = File("src/test/resources/jackson-coexistence/$name.json")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(actual)
        }
        assertEquals(file.readText(), actual)
    }
}