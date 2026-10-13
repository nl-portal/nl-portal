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
import type { IntlShape } from "react-intl";
import { cloneElement, isValidElement, type ReactNode } from "react";
import { Context, toPromise, type Emitter } from "liquidjs";
import { createLiquidEngine } from "./engine";
import { decodeLiquidText } from "./decodeLiquidText";

export async function renderTemplate(
  template: string,
  data: Record<string, unknown>,
  intl: IntlShape,
): Promise<ReactNode[]> {
  const liquid = createLiquidEngine(intl);
  const nodes: ReactNode[] = [];
  let text = "";
  const flushText = () => {
    if (text.trim()) nodes.push(decodeLiquidText(text));
    text = "";
  };
  const emitter: Emitter = {
    buffer: "",
    write(value: unknown) {
      if (isValidElement(value)) {
        flushText();
        nodes.push(cloneElement(value, { key: nodes.length }));
      } else if (value !== null && value !== undefined) text += String(value);
    },
  };
  const ctx = new Context(data, liquid.options, {}, { liquid });
  await toPromise(
    liquid.renderer.renderTemplates(liquid.parse(template), ctx, emitter),
  );
  flushText();
  return nodes;
}
