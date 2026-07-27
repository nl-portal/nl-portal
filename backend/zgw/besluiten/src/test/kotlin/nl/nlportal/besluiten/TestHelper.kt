/*
 * Copyright (c) 2024 Ritense BV, the Netherlands.
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
package nl.nlportal.besluiten

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.mockwebserver.MockResponse
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.function.Consumer

object TestHelper {
    val ERRORS_JSON_PATH = "$.errors"
    val EXTENSIONS_JSON_PATH = "$.extensions"
    private val logger = KotlinLogging.logger {}

    fun WebTestClient.ResponseSpec.verifyOnlyDataExists(basePath: String): WebTestClient.BodyContentSpec =
        this
            .expectBody()
            .consumeWith(Consumer { t -> logger.info { t } })
            .jsonPath(basePath)
            .exists()
            .jsonPath(ERRORS_JSON_PATH)
            .doesNotExist()
            .jsonPath(EXTENSIONS_JSON_PATH)
            .doesNotExist()

    fun mockResponse(body: String): MockResponse =
        MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody(body)

    val handleBesluitRequest =
        """
        {
          "url": "http://localhost:8001/besluiten/api/v1/besluiten/8863ab83-3496-4f40-9cad-f9d9526597c8",
          "identificatie": "klantportaal",
          "verantwoordelijkeOrganisatie": "klantportaal",
          "besluittype": "http://localhost:8000/catalogi/api/v1/besluittypen/496f51fd-ccdb-406e-805a-e7602ae78a2b",
          "zaak": "http://localhost:8001/zaken/api/v1/zaken/5d479908-fbb7-49c2-98c9-9afecf8de79a",
          "datum": "2019-08-24",
          "toelichting": "toelichting",
          "bestuursorgaan": "klant",
          "ingangsdatum": "2019-08-24",
          "vervaldatum": "2019-08-24",
          "vervalreden": "tijdelijk",
          "vervalredenWeergave": "string",
          "publicatiedatum": "2019-08-24",
          "verzenddatum": "2019-08-24",
          "uiterlijkeReactiedatum": "2019-08-24"
        }
        """.trimIndent()

    val handleBesluitRequestUnauthorizedZaak =
        """
        {
          "url": "http://localhost:8001/besluiten/api/v1/besluiten/7721129b-7bb9-49d0-9a84-0eb34b18320e",
          "identificatie": "klantportaal",
          "verantwoordelijkeOrganisatie": "klantportaal",
          "besluittype": "http://localhost:8000/catalogi/api/v1/besluittypen/496f51fd-ccdb-406e-805a-e7602ae78a2b",
          "zaak": "http://localhost:8001/zaken/api/v1/zaken/953ea2c4-09c4-47a1-a7c8-7a7b2783f852",
          "datum": "2019-08-24",
          "toelichting": "toelichting",
          "bestuursorgaan": "klant",
          "ingangsdatum": "2019-08-24",
          "vervaldatum": "2019-08-24",
          "vervalreden": "tijdelijk",
          "vervalredenWeergave": "string",
          "publicatiedatum": "2019-08-24",
          "verzenddatum": "2019-08-24",
          "uiterlijkeReactiedatum": "2019-08-24"
        }
        """.trimIndent()

    val handleBesluitDocumentRequest =
        """
          {
            "url": "http://localhost:10001/enkelvoudiginformatieobjecten/095be615-a8ad-4c33-8e9c-c7612fbf6c9f",
            "informatieobject": "http://localhost:10001/enkelvoudiginformatieobjecten/095be615-a8ad-4c33-8e9c-c7612fbf6c9f",
            "besluit": "http://localhost:8001/besluiten/api/v1/besluiten/8863ab83-3496-4f40-9cad-f9d9526597c8"
          }    
        """.trimIndent()

    val handleBesluitDocumentUnrelatedRequest =
        """
          {
            "url": "http://localhost:10001/enkelvoudiginformatieobjecten/00000000-0000-0000-0000-0000000000ff",
            "informatieobject": "http://localhost:10001/enkelvoudiginformatieobjecten/00000000-0000-0000-0000-0000000000ff",
            "besluit": "http://localhost:8001/besluiten/api/v1/besluiten/2a27bc3c-6a4c-432a-a9cb-5c31004e7769"
          }    
        """.trimIndent()

    val handleDocumentResponse =
        """
        {
           "url": "http://some.domain.com/enkelvoudiginformatieobjecten/095be615-a8ad-4c33-8e9c-c7612fbf6c9f",
           "identificatie": "string",
           "bronorganisatie": "string",
           "creatiedatum": "2021-10-14",
           "titel": "Een titel",
           "vertrouwelijkheidaanduiding": "openbaar",
           "auteur": "string",
           "status": "definitief",
           "formaat": ".pdf",
           "taal": "str",
           "versie": 0,
           "beginRegistratie": "2021-10-14T12:27:43Z",
           "bestandsnaam": "string",
           "inhoud": "http://example.com",
           "bestandsomvang": 0,
           "link": "http://example.com",
           "beschrijving": "string",
           "ontvangstdatum": "2021-10-14",
           "verzenddatum": "2021-10-14",
           "indicatieGebruiksrecht": true,
           "ondertekening": {
             "soort": "analoog",
             "datum": "2021-10-14"
           },
           "integriteit": {
             "algoritme": "crc_16",
             "waarde": "string",
             "datum": "2021-10-14"
           },
           "informatieobjecttype": "http://example.com",
           "locked": true,
           "bestandsdelen": [
             {
               "url": "http://example.com",
               "volgnummer": 0,
               "omvang": 0,
               "inhoud": "http://example.com",
               "voltooid": true,
               "lock": "string"
             }
           ]
         }
        """.trimIndent()

    val handleZaakResponse =
        """
            {
                "url": "http://localhost:8001/zaken/api/v1/zaken/5d479908-fbb7-49c2-98c9-9afecf8de79a",
                "uuid": "5d479908-fbb7-49c2-98c9-9afecf8de79a",
                "identificatie": "ZAAK-2021-0000000003",
                "bronorganisatie": "051845623",
                "omschrijving": "Voorbeeld afgesloten zaak 1",
                "toelichting": "",
                "zaaktype": "http://localhost:8000/catalogi/api/v1/zaaktypen/496f51fd-ccdb-406e-805a-e7602ae78a2b",
                "registratiedatum": "2021-09-16",
                "verantwoordelijkeOrganisatie": "051845623",
                "startdatum": "2021-09-16",
                "einddatum": null,
                "einddatumGepland": null,
                "uiterlijkeEinddatumAfdoening": null,
                "publicatiedatum": null,
                "communicatiekanaal": "",
                "productenOfDiensten": [],
                "vertrouwelijkheidaanduiding": "zaakvertrouwelijk",
                "betalingsindicatie": "",
                "betalingsindicatieWeergave": "",
                "laatsteBetaaldatum": null,
                "zaakgeometrie": null,
                "verlenging": {
                    "reden": "",
                    "duur": null
                },
                "opschorting": {
                    "indicatie": false,
                    "reden": ""
                },
                "selectielijstklasse": "",
                "hoofdzaak": null,
                "deelzaken": [],
                "relevanteAndereZaken": [],
                "eigenschappen": [],
                "status": "http://localhost:8000/zaken/api/v1/statussen/0c019c8a-2274-4a7b-b381-2f35908500a6",
                "kenmerken": [],
                "archiefnominatie": null,
                "archiefstatus": "nog_te_archiveren",
                "archiefactiedatum": null,
                "resultaat": "http://localhost:8001/zaken/api/v1/resultaten/095be615-a8ad-4c33-8e9c-c7612fbf6c9f"
            }
            """.trimIndent()

    val handleZaakRollenResponse =
        """
        {
               "count": 1,
               "next": null,
               "previous": null,
               "results": [
                 {
                   "url": "http://example.com",
                   "uuid": "095be615-a8ad-4c33-8e9c-c7612fbf6c9f",
                   "zaak": "http://example.com",
                   "betrokkene": "http://example.com",
                   "betrokkeneType": "natuurlijk_persoon",
                   "roltype": "http://example.com",
                   "omschrijving": "string",
                   "omschrijvingGeneriek": "string",
                   "roltoelichting": "string",
                   "registratiedatum": "2019-08-24T14:15:22Z",
                   "indicatieMachtiging": "gemachtigde"
                 }
               ]
             }    
        """.trimIndent()

    val handleZaakRollenResponseEmpty =
        """
        {
               "count": 1,
               "next": null,
               "previous": null,
               "results": []
             }    
        """.trimIndent()


}
