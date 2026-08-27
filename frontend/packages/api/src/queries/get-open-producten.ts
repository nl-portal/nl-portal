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
import { gql } from "@apollo/client";
import { OPEN_PRODUCT_FIELDS } from "../fragments/open-product";

export const QUERY_GET_OPEN_PRODUCTEN = gql`
  query GetOpenProducten(
    $pageNumber: Int
    $pageSize: Int
    $status: String
    $productTypeCode: String
    $productTypeId: String
    $productTypeCodes: [String!]
    $productTypeIds: [String!]
  ) {
    getOpenProducten(
      pageNumber: $pageNumber
      pageSize: $pageSize
      status: $status
      productTypeCode: $productTypeCode
      productTypeId: $productTypeId
      productTypeCodes: $productTypeCodes
      productTypeIds: $productTypeIds
    ) {
      number
      size
      totalElements
      numberOfElements
      totalPages
      content {
        ...openProductFields
      }
    }
  }
  ${OPEN_PRODUCT_FIELDS}
`;
