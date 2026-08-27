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
import { QUERY_GET_PERSOON_V2 } from "@nl-portal/nl-portal-api";

export const getPersoon = {
  request: {
    query: QUERY_GET_PERSOON_V2,
    variables: {},
  },
  result: {
    data: {
      getPersoonV2: {
        burgerservicenummer: "999991954",
        geslacht: {
          code: "6030",
          omschrijving: "vrouw",
        },
        naam: {
          voornamen: "Sierra",
          officialLastName: "de Kooyman - van der Maassen",
          __typename: "PersoonNaam",
        },
        verblijfplaats: {
          type: "Adres",
          functieAdres: {
            code: "W",
            omschrijving: "woonadres",
          },
          verblijfadres: {
            officieleStraatnaam: "Leyweg 61e",
            postcode: "2545CC",
            woonplaats: "'s-Gravenhage",
          },
          datumVan: {
            type: "Datum",
            datum: "2018-07-01",
            langFormaat: "1 juli 2018",
          },
          adresseerbaarObjectIdentificatie: 226010000038820,
        },
        geboorte: {
          datum: {
            datum: "2003-03-03",
            jaar: 2003,
            maand: 3,
            dag: 3,
            __typename: "PersoonDatum",
          },
          land: {
            code: "6030",
            omschrijving: "Nederland",
            __typename: "PersoonGeboorteLand",
          },
          __typename: "PersoonGeboorte",
        },
        nationaliteiten: [
          {
            nationaliteit: {
              code: "0001",
              omschrijving: "Nederlandse",
              __typename: "PersoonNationaliteit",
            },
            __typename: "PersoonNationaliteiten",
          },
          {
            nationaliteit: {
              code: "0002",
              omschrijving: "Portugees",
              __typename: "PersoonNationaliteit",
            },
            __typename: "PersoonNationaliteiten",
          },
        ],
        __typename: "Persoon",
      },
    },
  },
};
