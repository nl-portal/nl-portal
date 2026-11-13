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
import React from "react";
import { matchRoutes, RouteObject } from "react-router";
import { NavigationItem } from "../interfaces/navigation-item";

export type MenuEntryRoute = {
  type: string;
  page: string;
  path: string;
};

export type MenuEntry = {
  name: string;
  icon: string;
  group?: number;
  ordering?: number;
  route: MenuEntryRoute;
};

export type SkippedMenuEntry = {
  entry: MenuEntry;
  reason: string;
};

export const DEFAULT_MENU_ENTRY_GROUP = 1;

export const loadMenuEntries = async (
  restUri: string,
  oidcToken: string,
): Promise<MenuEntry[]> => {
  const response = await fetch(`${restUri}/frontend/menu`, {
    headers: { Authorization: `Bearer ${oidcToken}` },
  });

  if (!response.ok) {
    throw new Error(`Menu entries request failed: ${response.status}`);
  }

  return (await response.json()) as MenuEntry[];
};

/**
 * Splits entries into the ones that can be shown and the ones that can't.
 * An entry is skipped when its route type has no renderer, or when its path is
 * matched by a static app route (anything other than a catch-all "*" route).
 */
export const getRenderableMenuEntries = (
  entries: MenuEntry[],
  routes: RouteObject[],
  renderers: Record<string, unknown>,
): { renderable: MenuEntry[]; skipped: SkippedMenuEntry[] } => {
  const renderable: MenuEntry[] = [];
  const skipped: SkippedMenuEntry[] = [];

  entries.forEach((entry) => {
    if (!renderers[entry.route.type]) {
      skipped.push({
        entry,
        reason: `unsupported type "${entry.route.type}"`,
      });
      return;
    }

    const matches = matchRoutes(routes, entry.route.path);
    const deepestRoute = matches?.[matches.length - 1]?.route;

    if (deepestRoute && deepestRoute.path !== "*") {
      skipped.push({
        entry,
        reason: `path "${entry.route.path}" is already used by an app route`,
      });
      return;
    }

    renderable.push(entry);
  });

  return { renderable, skipped };
};

/**
 * Returns a copy of the menu groups with the entries placed by `group` and `ordering`.
 * A group index at or beyond the number of groups goes to one new trailing group.
 * Each `ordering` becomes the final position in its group; missing or too large values append.
 */
export const withMenuEntries = (
  groups: NavigationItem[][],
  entries: MenuEntry[],
  resolveIcon: (name: string) => React.ElementType,
): NavigationItem[][] => {
  const result = groups.map((group) => [...group]);
  const entriesByGroup = new Map<number, MenuEntry[]>();

  entries.forEach((entry) => {
    const index = Math.min(
      entry.group ?? DEFAULT_MENU_ENTRY_GROUP,
      groups.length,
    );
    entriesByGroup.set(index, [...(entriesByGroup.get(index) ?? []), entry]);
  });

  entriesByGroup.forEach((groupEntries, index) => {
    if (index === result.length) result.push([]);
    const group = result[index];
    let lastInsertedIndex = -1;

    [...groupEntries]
      .sort((a, b) => getOrdering(a) - getOrdering(b))
      .forEach((entry) => {
        const position = Math.min(
          Math.max(getOrdering(entry), lastInsertedIndex + 1),
          group.length,
        );
        group.splice(position, 0, toNavigationItem(entry, resolveIcon));
        lastInsertedIndex = position;
      });
  });

  return result;
};

const getOrdering = (entry: MenuEntry) =>
  entry.ordering ?? Number.MAX_SAFE_INTEGER;

const toNavigationItem = (
  entry: MenuEntry,
  resolveIcon: (name: string) => React.ElementType,
): NavigationItem => ({
  titleTranslationKey: entry.name,
  titleTranslationId: `menu-entries.${entry.name}`,
  path: entry.route.path,
  icon: resolveIcon(entry.icon),
  matchSubPaths: entry.route.type === "custom" || undefined,
});
