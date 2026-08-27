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
