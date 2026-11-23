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
import { ComponentType } from "react";
import MenuEntryPlaceholder from "../components/MenuEntryPlaceholder";
import PortalPage from "../components/PortalPage";
import { MenuEntry } from "./menu-entries";

export type MenuEntryRendererProps = { entry: MenuEntry };

export type MenuEntryRenderers = Record<
  string,
  ComponentType<MenuEntryRendererProps>
>;

/**
 * The default renderer per menu entry `route.type`. Implementations can pass their own map to `AppProvider`
 * (`menuEntryRenderers`); it replaces this one, so spread these defaults to keep them:
 * `{ ...MENU_ENTRY_RENDERERS, foo: FooPage }`.
 */
export const MENU_ENTRY_RENDERERS: MenuEntryRenderers = {
  "portal-page": PortalPage,
  custom: MenuEntryPlaceholder,
};
