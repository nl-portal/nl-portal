/*
 * Copyright 2015-2025 Ritense BV, the Netherlands.
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
package nl.nlportal.product.graphql

import com.fasterxml.jackson.databind.JsonNode
import nl.nlportal.commonground.authentication.WithBurgerUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.graphql.test.tester.HttpGraphQlTester

@SpringBootTest(properties = ["nl-portal.config.product.verbruiks-object-modification-enabled=false"])
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProductMutationDisabledIT(
    @Autowired private val applicationContext: ApplicationContext,
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val graphqlUpdateProductVerbruiksObject: String,
) {
    @Test
    fun productMutationBeanIsNotRegistered() {
        assertTrue(applicationContext.getBeansOfType(ProductMutation::class.java).isEmpty())
    }

    @Test
    @WithBurgerUser("569312863")
    fun updateProductVerbruiksObjectIsNotExecutable() {
        httpGraphQlTester
            .document(graphqlUpdateProductVerbruiksObject)
            .execute()
            .errors()
            .satisfy { errors -> assertTrue(errors.isNotEmpty()) }
    }

    @Test
    @WithBurgerUser("569312863")
    fun updateProductVerbruiksObjectRemainsInSchemaAsDeprecated() {
        val mutationType =
            httpGraphQlTester
                .document(INTROSPECT_MUTATION_TYPE)
                .execute()
                .errors()
                .verify()
                .path("__type")
                .entity(JsonNode::class.java)
                .get()

        val field =
            mutationType
                .path("fields")
                .firstOrNull { it.path("name").asText() == "updateProductVerbruiksObject" }

        assertNotNull(field)
        assertEquals(true, field?.path("isDeprecated")?.asBoolean())
        assertTrue(field?.path("deprecationReason")?.asText()?.contains("4.0.0") == true)
    }

    private companion object {
        const val INTROSPECT_MUTATION_TYPE = """
            query {
                __type(name: "Mutation") {
                    fields(includeDeprecated: true) {
                        name
                        isDeprecated
                        deprecationReason
                    }
                }
            }
        """
    }
}