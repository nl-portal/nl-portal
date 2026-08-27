/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.documentenapi.client

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentApisTest(
    @Autowired val documentApisConfig: DocumentApisConfig,
) {
    @Test
    fun `localhost documentapi should resolve to localhost config`() {
        assertEquals("https://localhost:8001/documenten/api/v1", documentApisConfig.properties.getConfig("openzaak").url)
        assertEquals("e09b8bc5-5831-4618-ab28-41411304309d", documentApisConfig.properties.getConfig("openzaak").secret)
    }

    @Test
    fun `exampleorg documentapi should resolve to localhost config`() {
        assertEquals("https://example.org/documenten/api/v1", documentApisConfig.properties.getConfig("example").url)
        assertEquals("e09b8bc5-5831-4618-ab28-111111111111", documentApisConfig.properties.getConfig("example").secret)
    }

    @Test
    fun `openzaak url should resolve to openzaak`() {
        assertEquals(
            "openzaak",
            documentApisConfig.properties.getConfigForDocumentUrl(
                "https://localhost:8001/documenten/api/v1/enkelvoudiginformatieobjecten/5f1e2695-8b68-448a-a62d-531321c744ec",
            ),
        )
    }

    @Test
    fun `exampleorg url should resolve to example`() {
        assertEquals(
            "example",
            documentApisConfig.properties.getConfigForDocumentUrl(
                "https://example.org/documenten/api/v1/adsf/api/v1/werwer/5f1e2695-8b68-448q",
            ),
        )
    }
}