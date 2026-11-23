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
import { ArchiveIcon, GridIcon } from "@gemeente-denhaag/icons";
import { UIMatch } from "react-router";
import { NavigationItem } from "../../interfaces/navigation-item";
import {
  getCurrentNavigationPage,
  getHiddenMenuEntryPage,
  getUnlabeledNavigationPage,
} from "../../utils/get-current-navigation-page";
import { MenuEntry } from "../../utils/menu-entries";

const entry: NavigationItem = {
  titleTranslationKey: "thema-concept",
  titleTranslationId: "menu-entries.thema-concept",
  path: "/thema-concept",
  icon: GridIcon,
};
const customEntry: NavigationItem = {
  titleTranslationKey: "extern-portaal",
  titleTranslationId: "menu-entries.extern-portaal",
  path: "/extern",
  icon: GridIcon,
  matchSubPaths: true,
};
const overview: NavigationItem = {
  titleTranslationKey: "overview",
  path: "/",
  icon: GridIcon,
};
const cases: NavigationItem = {
  titleTranslationKey: "cases",
  path: "/zaken",
  icon: ArchiveIcon,
};

const navigationItems = [[entry, overview], [cases], [customEntry]];

const match = (
  pathname: string,
  label?: string,
): UIMatch<unknown, { label?: string } | undefined> => ({
  id: pathname,
  pathname,
  params: {},
  data: undefined,
  loaderData: undefined,
  handle: label ? { label } : undefined,
});

const root = match("/", "breadcrumb.overview");

describe("getCurrentNavigationPage", () => {
  it("returns an entry before overview on its own path", () => {
    expect(
      getCurrentNavigationPage(
        [root, match("/thema-concept")],
        navigationItems,
      ),
    ).toBe(entry);
  });

  it("returns a custom entry on a sub path", () => {
    expect(
      getCurrentNavigationPage([root, match("/extern/a/b")], navigationItems),
    ).toBe(customEntry);
  });

  it("does not return a portal-page entry on a sub path", () => {
    expect(
      getCurrentNavigationPage(
        [root, match("/thema-concept/a")],
        navigationItems,
      ),
    ).toBe(overview);
  });

  it("returns the standard item on its static child route", () => {
    expect(
      getCurrentNavigationPage(
        [
          root,
          match("/zaken", "breadcrumb.cases"),
          match("/zaken/zaak/1", "breadcrumb.case"),
        ],
        navigationItems,
      ),
    ).toBe(cases);
  });

  describe("with menu entries", () => {
    const hiddenEntry = (path: string): MenuEntry => ({
      name: "detail",
      icon: "DocumentIcon",
      hidden: true,
      route: { type: "portal-page", page: "detail", path },
    });
    const themaDetail = hiddenEntry("/thema-concept/:id");

    it("returns the visible entry above a hidden entry route", () => {
      expect(
        getCurrentNavigationPage(
          [root, match("/thema-concept/abc")],
          navigationItems,
          [themaDetail],
        ),
      ).toBe(entry);
    });

    it("returns the visible entry with the longest path above a hidden entry route", () => {
      const archive: NavigationItem = {
        titleTranslationKey: "thema-archief",
        path: "/thema-concept/archief",
        icon: GridIcon,
      };

      expect(
        getCurrentNavigationPage(
          [root, match("/thema-concept/archief/abc")],
          [[entry, overview], [archive]],
          [hiddenEntry("/thema-concept/archief/:id")],
        ),
      ).toBe(archive);
    });

    it("returns overview for a hidden entry without a visible entry above it", () => {
      expect(
        getCurrentNavigationPage(
          [root, match("/erfpacht/abc")],
          navigationItems,
          [hiddenEntry("/erfpacht/:id")],
        ),
      ).toBe(overview);
    });

    it("returns overview for an unknown sub path that is no hidden entry route", () => {
      expect(
        getCurrentNavigationPage(
          [root, match("/thema-concept/a/b")],
          navigationItems,
          [themaDetail],
        ),
      ).toBe(overview);
    });

    it("leaves custom sub paths and app child routes as they are", () => {
      expect(
        getCurrentNavigationPage(
          [root, match("/extern/a/b")],
          navigationItems,
          [themaDetail],
        ),
      ).toBe(customEntry);
      expect(
        getCurrentNavigationPage(
          [
            root,
            match("/zaken", "breadcrumb.cases"),
            match("/zaken/zaak/1", "breadcrumb.case"),
          ],
          navigationItems,
          [hiddenEntry("/zaken/:id")],
        ),
      ).toBe(cases);
    });

    it("returns overview on a hidden entry route without menu entries", () => {
      expect(
        getCurrentNavigationPage(
          [root, match("/thema-concept/abc")],
          navigationItems,
        ),
      ).toBe(overview);
    });
  });
});

describe("getUnlabeledNavigationPage", () => {
  it("returns the entry for an unlabeled match", () => {
    expect(
      getUnlabeledNavigationPage(
        [root, match("/thema-concept")],
        navigationItems,
      ),
    ).toBe(entry);
  });

  it("returns a custom entry for an unlabeled sub path", () => {
    expect(
      getUnlabeledNavigationPage([root, match("/extern/a")], navigationItems),
    ).toBe(customEntry);
  });

  it("returns nothing for a labelled match", () => {
    expect(
      getUnlabeledNavigationPage(
        [root, match("/zaken", "breadcrumb.cases")],
        navigationItems,
      ),
    ).toBeUndefined();
  });

  it("returns nothing for an unknown path", () => {
    expect(
      getUnlabeledNavigationPage(
        [root, match("/bestaat-niet")],
        navigationItems,
      ),
    ).toBeUndefined();
  });
});

describe("getHiddenMenuEntryPage", () => {
  const hidden: MenuEntry = {
    name: "erfpacht-product",
    icon: "DocumentIcon",
    hidden: true,
    route: { type: "portal-page", page: "product", path: "/erfpacht/:id" },
  };
  const visibleWithParam: MenuEntry = {
    name: "thema-detail",
    icon: "DocumentIcon",
    route: { type: "portal-page", page: "detail", path: "/thema-concept/:id" },
  };

  it("returns the hidden entry whose route path matches the deepest match", () => {
    expect(
      getHiddenMenuEntryPage([root, match("/erfpacht/abc")], navigationItems, [
        hidden,
      ]),
    ).toBe(hidden);
  });

  it("returns nothing for a labeled match or a path without hidden entry", () => {
    expect(
      getHiddenMenuEntryPage(
        [root, match("/erfpacht/abc", "breadcrumb.x")],
        navigationItems,
        [hidden],
      ),
    ).toBeUndefined();
    expect(
      getHiddenMenuEntryPage([root, match("/erfpacht")], navigationItems, [
        hidden,
      ]),
    ).toBeUndefined();
    expect(
      getHiddenMenuEntryPage(
        [root, match("/thema-concept/abc")],
        navigationItems,
        [visibleWithParam],
      ),
    ).toBeUndefined();
  });

  it("lets an entry with a menu item win over a hidden entry", () => {
    const hiddenOnEntryPath: MenuEntry = {
      ...hidden,
      route: { ...hidden.route, path: "/thema-concept" },
    };

    expect(
      getHiddenMenuEntryPage([root, match("/thema-concept")], navigationItems, [
        hiddenOnEntryPath,
      ]),
    ).toBeUndefined();
  });
});
