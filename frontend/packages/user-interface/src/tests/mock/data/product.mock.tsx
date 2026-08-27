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
import { QUERY_GET_OPEN_PRODUCT_HOOFD_THEMAS_BY_PRODUCTEN } from "@nl-portal/nl-portal-api";

export const getProduct = {
  request: {
    query: QUERY_GET_OPEN_PRODUCT_HOOFD_THEMAS_BY_PRODUCTEN,
    variables: {},
  },
  result: {
    data: {
      getOpenProductHoofdThemas: [
        {
          uuid: "41f71c2e-9e0c-4a1b-8d39-709669b256c2",
          naam: "Belastingzaken",
          __typename: "OpenProductThema",
        },
        {
          uuid: "b95cfbf6-8578-410b-b108-a42fd20af843",
          naam: "Parkeren",
          __typename: "OpenProductThema",
        },
        {
          uuid: "1a25f58c-8e7b-425f-b466-7e6f8ca1268b",
          naam: "Hoofdthema",
          __typename: "OpenProductThema",
        },
      ],
    },
  },
};
