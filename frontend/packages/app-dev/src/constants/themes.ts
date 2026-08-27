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
import { Themes } from "@nl-portal/nl-portal-user-interface";

export type ThemeSlug = "parkeren";
export type ProductTypeSlug = "vergunningen" | "bezoekersvergunningen";
export type ProductTypeCodes = "PARKEREN" | "BEZOEKERSVERGUNNING";

/**
 * Slug: for URL paths and translation keys
 * ProductType -> slug: for url paths and for translation keys
 * ProductType -> code: for mapping to product types in the API responses
 */

export const themes: Themes<ThemeSlug, ProductTypeSlug, ProductTypeCodes> = {
  parkeren: {
    slug: "parkeren",
    productTypes: {
      vergunningen: {
        slug: "vergunningen",
        code: "PARKEREN",
      },
      bezoekersvergunningen: {
        slug: "bezoekersvergunningen",
        code: "BEZOEKERSVERGUNNING",
      },
    },
  },
};
