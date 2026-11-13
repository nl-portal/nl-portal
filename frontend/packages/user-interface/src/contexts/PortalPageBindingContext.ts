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
import { createContext } from "react";
import type { PortalPageBindingMeta } from "../utils/portal-pages";

/**
 * Lets template tags of a dynamic page read a binding's current params and refetch it with changed params.
 */
export type PortalPageBindingContextValue = {
  meta: (alias: string) => PortalPageBindingMeta | undefined;
  refetch: (alias: string, changes: Record<string, string>) => void;
};

const PortalPageBindingContext = createContext<
  PortalPageBindingContextValue | undefined
>(undefined);

export default PortalPageBindingContext;
