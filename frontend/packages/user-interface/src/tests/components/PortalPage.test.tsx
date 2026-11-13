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
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ContextType, useState } from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { createMemoryRouter, Outlet, RouterProvider } from "react-router";
import { ApiContext } from "@nl-portal/nl-portal-api";
import { OidcContext } from "@nl-portal/nl-portal-authentication";
import { NotificationProvider } from "../../contexts/NotificationContext";
import PortalPage from "../../components/PortalPage";
import { MenuEntry } from "../../utils/menu-entries";
import { testPaths } from "../../providers/TestProvider";

const messages = {
  "portalPage.error": "Deze inhoud kan niet worden weergegeven.",
  "portalPage.refetchError": "Het ophalen van de gegevens is mislukt.",
  "portalPage.noDetails": "Er zijn geen details beschikbaar.",
  "menu-entries.erfpacht": "Erfpacht",
};

const entry = (page: string, path: string): MenuEntry => ({
  name: "erfpacht",
  icon: "DocumentIcon",
  route: { type: "portal-page", page, path },
});

type Responder = (url: string, init?: RequestInit) => Promise<Response>;

const json = (body: unknown, status = 200) =>
  Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );

let respond: Responder;
const fetchMock = vi.fn((url: string, init?: RequestInit) =>
  respond(url, init),
);

beforeEach(() => {
  fetchMock.mockClear();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const Shell = () => {
  const [locale, setLocale] = useState("nl-NL");
  return (
    <IntlProvider locale={locale} messages={messages} onError={() => {}}>
      <button onClick={() => setLocale("en-GB")}>en-GB</button>
      <ApiContext.Provider
        value={{ restUri: "http://api" } as ContextType<typeof ApiContext>}
      >
        <OidcContext.Provider
          value={{ oidcToken: "token" } as ContextType<typeof OidcContext>}
        >
          <NotificationProvider>
            <Outlet context={{ paths: testPaths }} />
          </NotificationProvider>
        </OidcContext.Provider>
      </ApiContext.Provider>
    </IntlProvider>
  );
};

const renderAt = (menuEntry: MenuEntry, path: string) => {
  const router = createMemoryRouter(
    [
      {
        element: <Shell />,
        children: [
          { path: testPaths.noMatch, element: <div>not found route</div> },
          {
            path: menuEntry.route.path,
            element: <PortalPage entry={menuEntry} />,
          },
        ],
      },
    ],
    { initialEntries: [path] },
  );
  render(<RouterProvider router={router} />);
};

const dynamicPage = {
  type: "dynamic",
  messages: {},
  root: null,
  template: `{% table binding: "producten" page-param: "page" page-size: 1 total: producten.total %}
Naam
{% for p in producten.items %}{{ p }}
{% endfor %}{% endtable %}
{% paragraph %}{{ info.tekst }}{% endparagraph %}`,
  bindings: [
    {
      alias: "info",
      data: { tekst: "Informatie" },
      error: false,
      meta: { params: {} },
    },
    {
      alias: "producten",
      data: { items: ["eerste"], total: 3 },
      error: false,
      meta: { params: { page: "0" } },
    },
  ],
};

const binding = (items: string[], page: string) => ({
  alias: "producten",
  data: { items, total: 3 },
  error: false,
  meta: { params: { page } },
});

describe("PortalPage", () => {
  it("renders a static page with its messages, without a default heading, and sends Accept-Language", async () => {
    respond = () =>
      json({
        type: "static",
        template:
          "{% heading level: 2 %}{{ 'titel' | i18n }}{% endheading %}{% description-list title: \"kop\" %}\n{{ 'veld' | i18n }} | x\n{% enddescription-list %}",
        messages: {
          "nl-NL": { titel: "Over ons", kop: "Gegevens", veld: "Veld" },
          "en-GB": { titel: "About us", kop: "Details", veld: "Field" },
        },
        root: null,
        bindings: [],
      });

    renderAt(entry("over-ons", "/over-ons"), "/over-ons");

    expect(
      await screen.findByRole("heading", { name: "Over ons" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Gegevens")).toBeInTheDocument();
    expect(screen.getByText("Veld")).toBeInTheDocument();
    expect(screen.queryByText("Erfpacht")).toBeNull();
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://api/frontend/portal-pages/over-ons");
    expect(new Headers(init?.headers).get("Accept-Language")).toBe("nl-NL");
    expect(new Headers(init?.headers).get("Authorization")).toBe(
      "Bearer token",
    );
  });

  it("switches page messages with the language without a request", async () => {
    respond = () =>
      json({
        type: "static",
        template:
          "{% heading level: 2 %}{{ 'titel' | i18n }}{% endheading %}{% paragraph %}{{ 'alleenNl' | i18n }}{% endparagraph %}",
        messages: {
          "nl-NL": { titel: "Over ons", alleenNl: "Alleen Nederlands" },
          "en-GB": { titel: "About us" },
        },
        root: null,
        bindings: [],
      });

    renderAt(entry("over-ons", "/over-ons"), "/over-ons");
    expect(
      await screen.findByRole("heading", { name: "Over ons" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Alleen Nederlands")).toBeInTheDocument();

    fireEvent.click(screen.getByText("en-GB"));

    expect(
      await screen.findByRole("heading", { name: "About us" }),
    ).toBeInTheDocument();
    expect(screen.queryByText("Alleen Nederlands")).toBeNull();
    expect(screen.getByText("alleenNl")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("renders a dynamic page with the bindings as context", async () => {
    respond = () => json(dynamicPage);

    renderAt(entry("erfpacht", "/erfpacht"), "/erfpacht");

    expect(await screen.findByText("eerste")).toBeInTheDocument();
    expect(screen.getByText("Informatie")).toBeInTheDocument();
  });

  it("pagination refetches and replaces only that binding", async () => {
    respond = (url, init) =>
      init?.method === "POST"
        ? json(binding(["tweede"], "1"))
        : json(dynamicPage);

    renderAt(entry("erfpacht", "/erfpacht"), "/erfpacht");
    fireEvent.click(
      await screen.findByRole("button", { name: /volgende|next/i }),
    );

    expect(await screen.findByText("tweede")).toBeInTheDocument();
    expect(screen.getByText("Informatie")).toBeInTheDocument();
    const [url, init] = fetchMock.mock.calls[1];
    expect(url).toBe(
      "http://api/frontend/portal-pages/erfpacht/bindings/producten",
    );
    expect(JSON.parse(init?.body as string)).toEqual({ params: { page: "1" } });
  });

  it("drops a stale refetch response", async () => {
    let resolveFirst: (value: Response) => void = () => {};
    let posts = 0;
    respond = (url, init) => {
      if (init?.method !== "POST") return json(dynamicPage);
      posts += 1;
      if (posts === 1)
        return new Promise<Response>((resolve) => (resolveFirst = resolve));
      return json(binding(["derde"], "2"));
    };

    renderAt(entry("erfpacht", "/erfpacht"), "/erfpacht");
    const next = await screen.findByRole("button", { name: /volgende|next/i });
    fireEvent.click(next);
    fireEvent.click(next);

    expect(await screen.findByText("derde")).toBeInTheDocument();
    resolveFirst(
      new Response(JSON.stringify(binding(["oud"], "1")), { status: 200 }),
    );
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(screen.queryByText("oud")).toBeNull();
    expect(screen.getByText("derde")).toBeInTheDocument();
  });

  it("keeps the data and shows a notification when a refetch fails", async () => {
    respond = (url, init) =>
      init?.method === "POST" ? json({}, 500) : json(dynamicPage);

    renderAt(entry("erfpacht", "/erfpacht"), "/erfpacht");
    fireEvent.click(
      await screen.findByRole("button", { name: /volgende|next/i }),
    );

    expect(
      await screen.findByText("Het ophalen van de gegevens is mislukt."),
    ).toBeInTheDocument();
    expect(screen.getByText("eerste")).toBeInTheDocument();
  });

  it("loads a page by id and renders the root binding's template with that binding's data only", async () => {
    respond = () =>
      json({
        type: "dynamic",
        template: "{% paragraph %}A: {{ naam }}{% endparagraph %}",
        messages: {},
        root: "a",
        bindings: [
          {
            alias: "a",
            data: { naam: "Eerste" },
            error: false,
            meta: { params: {} },
          },
          {
            alias: "b",
            data: { naam: "Tweede" },
            error: false,
            meta: { params: {} },
          },
        ],
      });

    renderAt(entry("product", "/erfpacht/:id"), "/erfpacht/abc");

    expect(await screen.findByText("A: Eerste")).toBeInTheDocument();
    expect(screen.queryByText(/Tweede/)).toBeNull();
    expect(fetchMock.mock.calls[0][0]).toBe(
      "http://api/frontend/portal-pages/product/abc",
    );
  });

  it("shows noDetails when the root binding supplied no template", async () => {
    respond = () =>
      json({
        type: "dynamic",
        template: null,
        messages: {},
        root: "a",
        bindings: [
          { alias: "a", data: {}, error: false, meta: { params: {} } },
        ],
      });

    renderAt(entry("product", "/erfpacht/:id"), "/erfpacht/abc");

    expect(
      await screen.findByText("Er zijn geen details beschikbaar."),
    ).toBeInTheDocument();
  });

  it("shows the error alert when the root binding failed", async () => {
    respond = () =>
      json({
        type: "dynamic",
        template: null,
        messages: {},
        root: "a",
        bindings: [
          { alias: "a", data: null, error: true, meta: { params: {} } },
        ],
      });

    renderAt(entry("product", "/erfpacht/:id"), "/erfpacht/abc");

    expect(
      await screen.findByText("Deze inhoud kan niet worden weergegeven."),
    ).toBeInTheDocument();
  });

  it("navigates to noMatch on a 404", async () => {
    respond = () => json({ message: "Page not found" }, 404);

    renderAt(entry("product", "/erfpacht/:id"), "/erfpacht/onbekend");

    expect(await screen.findByText("not found route")).toBeInTheDocument();
  });

  it("contains a template render error and keeps the notifications", async () => {
    respond = () =>
      json({
        type: "static",
        template: "{{ onbekend }}",
        messages: {},
        root: null,
        bindings: [],
      });

    renderAt(entry("over-ons", "/over-ons"), "/over-ons");

    await waitFor(() =>
      expect(
        screen.getByText("Deze inhoud kan niet worden weergegeven."),
      ).toBeInTheDocument(),
    );
  });
});
