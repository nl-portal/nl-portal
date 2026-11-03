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
export type BindingMeta = {
  params: Record<string, string>;
};

export type BindingResponse = {
  alias: string;
  data: unknown;
  error: boolean;
  meta: BindingMeta;
};

/**
 * A page with its template and bindings. `messages` holds the template's messages per portal locale.
 * When `root` is set, the template comes from that binding and renders with its data as root;
 * `template` is then `null` when the binding supplied none.
 */
export type PageResponse = {
  type: "static" | "dynamic";
  template: string | null;
  messages: Record<string, Record<string, string>>;
  root: string | null;
  bindings: BindingResponse[];
};

/** The page doesn't exist or isn't visible to the user. */
export class PageNotFoundError extends Error {}

const request = async <T>(
  url: string,
  token: string,
  locale: string,
  init: RequestInit = {},
): Promise<T> => {
  const response = await fetch(url, {
    ...init,
    headers: {
      ...init.headers,
      Authorization: `Bearer ${token}`,
      "Accept-Language": locale,
    },
  });
  if (response.status === 404) {
    throw new PageNotFoundError(`Page request failed: ${response.status}`);
  }
  if (!response.ok) {
    throw new Error(`Page request failed: ${response.status}`);
  }
  return (await response.json()) as T;
};

const pageUrl = (restUri: string, page: string) =>
  `${restUri}/frontend/pages/${encodeURIComponent(page)}`;

/**
 * Loads a page, or with [id] a page whose bindings all resolve that object.
 *
 * @throws PageNotFoundError when the backend answers 404
 */
export const loadPage = (
  restUri: string,
  token: string,
  locale: string,
  page: string,
  id?: string,
) =>
  request<PageResponse>(
    id === undefined
      ? pageUrl(restUri, page)
      : `${pageUrl(restUri, page)}/${encodeURIComponent(id)}`,
    token,
    locale,
  );

/**
 * Resolves binding [alias] of a dynamic page again with [params] as overrides.
 */
export const refetchBinding = (
  restUri: string,
  token: string,
  locale: string,
  page: string,
  alias: string,
  params: Record<string, string>,
) =>
  request<BindingResponse>(
    `${pageUrl(restUri, page)}/bindings/${encodeURIComponent(alias)}`,
    token,
    locale,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ params }),
    },
  );
