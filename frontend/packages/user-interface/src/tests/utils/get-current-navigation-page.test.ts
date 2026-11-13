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
  getUnlabeledNavigationPage,
} from "../../utils/get-current-navigation-page";

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

  it("does not return a templated entry on a sub path", () => {
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
