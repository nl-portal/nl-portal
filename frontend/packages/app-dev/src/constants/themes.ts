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
