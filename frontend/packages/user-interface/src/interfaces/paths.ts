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
export interface Paths {
  noMatch: string;
  overview: string;
  cases: string;
  case: (id?: string | number) => string;
  tasks: string;
  task: (id?: string | number) => string;
  messages: string;
  message: (id?: string | number) => string;
  products: string;
  themeOverview: (themeSlug: string) => string;
  themeList: (themeSlug: string, productTypeSlug?: string) => string;
  themeDetails: (
    themeSlug: string,
    productSlug?: string,
    id?: string | number,
  ) => string;
  themeHistory: (
    themeSlug: string,
    productSlug?: string,
    id?: string | number,
  ) => string;
  themeMutate: (
    themeSlug: string,
    productSlug?: string,
    id?: string | number,
  ) => string;
  account: string;
  changeContactInfo: (type?: string) => string;
  //eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: ((...args: any[]) => string) | string | undefined;
}
