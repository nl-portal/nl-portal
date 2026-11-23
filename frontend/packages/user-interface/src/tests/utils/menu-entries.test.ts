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
import { RouteObject } from "react-router";
import { NavigationItem } from "../../interfaces/navigation-item";
import {
  getRenderableMenuEntries,
  MenuEntry,
  withMenuEntries,
} from "../../utils/menu-entries";

const Icon = () => null;
const resolveIcon = () => Icon;

const overview: NavigationItem = {
  titleTranslationKey: "overview",
  path: "/",
  icon: GridIcon,
};
const tasks: NavigationItem = {
  titleTranslationKey: "tasks",
  path: "/taken",
  icon: ArchiveIcon,
};
const cases: NavigationItem = {
  titleTranslationKey: "cases",
  path: "/zaken",
  icon: ArchiveIcon,
};
const account: NavigationItem = {
  titleTranslationKey: "account",
  path: "/account",
  icon: ArchiveIcon,
};

const groups = (): NavigationItem[][] => [
  [overview],
  [tasks, cases],
  [account],
];

const entry = (
  name: string,
  overrides: Partial<MenuEntry> = {},
  type = "templated",
): MenuEntry => ({
  name,
  icon: "GridIcon",
  route: { type, page: "page", path: `/${name}` },
  ...overrides,
});

const paths = (navigationItems: NavigationItem[][]) =>
  navigationItems.map((group) => group.map((item) => item.path));

describe("withMenuEntries", () => {
  describe("group placement", () => {
    it("places an entry without group in group 1", () => {
      const result = withMenuEntries(groups(), [entry("a")], resolveIcon);

      expect(paths(result)).toEqual([
        ["/"],
        ["/taken", "/zaken", "/a"],
        ["/account"],
      ]);
    });

    it("places an entry without group in a new trailing group when there is a single group", () => {
      const result = withMenuEntries([[overview]], [entry("a")], resolveIcon);

      expect(paths(result)).toEqual([["/"], ["/a"]]);
    });

    it("places an entry in its configured group", () => {
      const result = withMenuEntries(
        groups(),
        [entry("a", { group: 2 })],
        resolveIcon,
      );

      expect(paths(result)).toEqual([
        ["/"],
        ["/taken", "/zaken"],
        ["/account", "/a"],
      ]);
    });

    it("places all out of range entries in one new trailing group", () => {
      const result = withMenuEntries(
        groups(),
        [entry("a", { group: 4 }), entry("b", { group: 9 })],
        resolveIcon,
      );

      expect(paths(result)).toEqual([
        ["/"],
        ["/taken", "/zaken"],
        ["/account"],
        ["/a", "/b"],
      ]);
    });
  });

  describe("ordering", () => {
    it("places ordering 0 at the front", () => {
      const result = withMenuEntries(
        groups(),
        [entry("a", { ordering: 0 })],
        resolveIcon,
      );

      expect(paths(result)[1]).toEqual(["/a", "/taken", "/zaken"]);
    });

    it("keeps config order for equal ordering", () => {
      const result = withMenuEntries(
        groups(),
        [entry("a", { ordering: 0 }), entry("b", { ordering: 0 })],
        resolveIcon,
      );

      expect(paths(result)[1]).toEqual(["/a", "/b", "/taken", "/zaken"]);
    });

    it("places entries at their final positions", () => {
      const result = withMenuEntries(
        groups(),
        [entry("b", { ordering: 2 }), entry("a", { ordering: 0 })],
        resolveIcon,
      );

      expect(paths(result)[1]).toEqual(["/a", "/taken", "/b", "/zaken"]);
    });

    it("appends missing and too large ordering", () => {
      const result = withMenuEntries(
        groups(),
        [entry("a"), entry("b", { ordering: 99 }), entry("c", { ordering: 0 })],
        resolveIcon,
      );

      expect(paths(result)[1]).toEqual(["/c", "/taken", "/zaken", "/b", "/a"]);
    });
  });

  describe("item shape", () => {
    it("uses the menu-entries title id and resolved icon", () => {
      const result = withMenuEntries(groups(), [entry("a")], resolveIcon);

      expect(result[1][2]).toEqual({
        titleTranslationKey: "a",
        titleTranslationId: "menu-entries.a",
        path: "/a",
        icon: Icon,
        matchSubPaths: undefined,
      });
    });

    it("sets matchSubPaths for custom entries only", () => {
      const result = withMenuEntries(
        groups(),
        [entry("a"), entry("b", {}, "custom")],
        resolveIcon,
      );

      expect(result[1][2].matchSubPaths).toBeUndefined();
      expect(result[1][3].matchSubPaths).toBe(true);
    });
  });

  it("never removes standard items", () => {
    const result = withMenuEntries(
      groups(),
      [entry("a", { group: 0 }), entry("b", { group: 1, ordering: 0 })],
      resolveIcon,
    );

    expect(result.flat()).toEqual(expect.arrayContaining(groups().flat()));
  });

  it("does not mutate the input", () => {
    const input = groups();
    const snapshot = paths(input);

    withMenuEntries(input, [entry("a"), entry("b", { group: 9 })], resolveIcon);

    expect(paths(input)).toEqual(snapshot);
  });
});

describe("getRenderableMenuEntries", () => {
  const Renderer = () => null;
  const renderers = { templated: Renderer, custom: Renderer };
  const routes: RouteObject[] = [
    { path: "/taken", element: null },
    { path: "/zaken/:id", element: null },
    { path: "*", element: null },
  ];

  const withPath = (name: string, path: string, type = "templated") =>
    entry(name, { route: { type, page: "page", path } });

  it("keeps templated and custom entries on a new path", () => {
    const entries = [
      withPath("a", "/nieuw"),
      withPath("b", "/extern", "custom"),
    ];

    const { renderable, skipped } = getRenderableMenuEntries(
      entries,
      routes,
      renderers,
    );

    expect(renderable).toEqual(entries);
    expect(skipped).toEqual([]);
  });

  it("skips an unknown type", () => {
    const unknown = withPath("a", "/nieuw", "default");

    const { renderable, skipped } = getRenderableMenuEntries(
      [unknown],
      routes,
      renderers,
    );

    expect(renderable).toEqual([]);
    expect(skipped).toEqual([
      { entry: unknown, reason: 'unsupported type "default"' },
    ]);
  });

  it.each(["/taken", "/TAKEN", "/zaken/extra"])(
    "skips path %s used by an app route",
    (path) => {
      const colliding = withPath("a", path);

      const { renderable, skipped } = getRenderableMenuEntries(
        [colliding],
        routes,
        renderers,
      );

      expect(renderable).toEqual([]);
      expect(skipped).toEqual([
        {
          entry: colliding,
          reason: `path "${path}" is already used by an app route`,
        },
      ]);
    },
  );

  it("does not treat a path only matched by * as a collision", () => {
    const { renderable } = getRenderableMenuEntries(
      [withPath("a", "/zaken")],
      routes,
      renderers,
    );

    expect(renderable).toHaveLength(1);
  });

  it("keeps config order", () => {
    const entries = [
      withPath("c", "/c"),
      withPath("x", "/taken"),
      withPath("a", "/a"),
      withPath("b", "/b"),
    ];

    const { renderable } = getRenderableMenuEntries(entries, routes, renderers);

    expect(renderable.map((e) => e.name)).toEqual(["c", "a", "b"]);
  });
});
