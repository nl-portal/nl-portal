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
import { describe, it, expect } from "vitest";
import { ContextType } from "react";
import { render, waitFor } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { createMemoryRouter, RouterProvider } from "react-router";
import { GridIcon } from "@gemeente-denhaag/icons";
import PageMetaData from "../../components/PageMetaData";
import AppContext from "../../contexts/AppContext";
import RouterContext from "../../contexts/RouterContext";
import { MenuEntry } from "../../utils/menu-entries";

const hidden: MenuEntry = {
  name: "erfpacht-product",
  icon: "DocumentIcon",
  hidden: true,
  route: { type: "portal-page", page: "product", path: "/erfpacht/:id" },
};

const renderAt = (path: string, messages: Record<string, string>) => {
  const router = createMemoryRouter(
    [
      {
        path: "/",
        handle: { label: "breadcrumb.overview" },
        element: (
          <IntlProvider locale="nl" messages={messages} onError={() => {}}>
            <RouterContext.Provider
              value={
                {
                  navigationItems: [
                    [
                      {
                        titleTranslationKey: "overview",
                        path: "/",
                        icon: GridIcon,
                      },
                    ],
                  ],
                } as unknown as ContextType<typeof RouterContext>
              }
            >
              <AppContext.Provider
                value={
                  { menuEntries: [hidden] } as ContextType<typeof AppContext>
                }
              >
                <PageMetaData />
              </AppContext.Provider>
            </RouterContext.Provider>
          </IntlProvider>
        ),
        children: [{ path: "*", element: null }],
      },
    ],
    { initialEntries: [path] },
  );
  render(<RouterProvider router={router} />);
};

describe("PageMetaData for a hidden menu entry", () => {
  it("uses the menu entry message as page title", async () => {
    renderAt("/erfpacht/abc", {
      "app.appName": "NL Portal",
      "pageTitles.overview": "Overzicht",
      "menu-entries.erfpacht-product": "Erfpachtdetails",
    });

    await waitFor(() =>
      expect(document.title).toBe("Erfpachtdetails - NL Portal"),
    );
  });

  it("falls back to the app name without a message, never the raw id", async () => {
    renderAt("/erfpacht/abc", {
      "app.appName": "NL Portal",
      "pageTitles.overview": "Overzicht",
    });

    await waitFor(() => expect(document.title).toBe("NL Portal"));
  });
});
