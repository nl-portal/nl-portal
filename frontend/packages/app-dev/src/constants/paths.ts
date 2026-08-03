import { Paths } from "@nl-portal/nl-portal-user-interface";

export const paths: Paths = {
  noMatch: "/pagina-niet-gevonden",
  overview: "/",
  cases: "/zaken",
  case: (id = ":id") => `/zaken/zaak/${id}`,
  tasks: "/taken",
  task: (id = ":id") => `/taken/taak/${id}`,
  messages: "/berichten",
  message: (id = ":id") => `/berichten/bericht/${id}`,
  products: "/producten",
  themeOverview: (slug = ":slug") => `/${slug}`,
  themeList: (slug = ":slug", productTypeSlug = ":productTypeSlug") =>
    `/${slug}/${productTypeSlug}/lijst`,
  themeDetails: (
    slug = ":slug",
    productTypeSlug = ":productTypeSlug",
    id = ":id",
  ) => `/${slug}/${productTypeSlug}/${id}`,
  themeHistory: (
    slug = ":slug",
    productTypeSlug = ":productTypeSlug",
    id = ":id",
  ) => `/${slug}/${productTypeSlug}/${id}/geschiedenis`,
  themeMutate: (
    slug = ":slug",
    productTypeSlug = ":productTypeSlug",
    id = ":id",
  ) => `/${slug}/${productTypeSlug}/${id}/wijzigen`,
  account: "/account",
  changeContactInfo: (type = ":type") => `/account/wijzig/${type}`,
};
