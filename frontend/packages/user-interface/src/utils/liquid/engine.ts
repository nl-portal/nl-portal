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
import { Liquid } from "liquidjs";
import type { IntlShape } from "react-intl";
import { currencyFormat } from "../../constants/currency-format";
import { shortDateOptions } from "@gemeente-denhaag/utils";

export const LIQUID_FORMATTERS = {
  date: (intl: IntlShape, value: unknown) =>
    intl.formatDate(
      value as Parameters<IntlShape["formatDate"]>[0],
      shortDateOptions,
    ),
  currency: (
    intl: IntlShape,
    value: unknown,
    currency = currencyFormat.currency,
  ) => intl.formatNumber(value as number, { ...currencyFormat, currency }),
};

export function createLiquidEngine(intl: IntlShape) {
  const liquid = new Liquid({
    strictVariables: true,
    strictFilters: true,
  });

  for (const [name, formatter] of Object.entries(LIQUID_FORMATTERS)) {
    liquid.registerFilter(name, (value: unknown, ...args: string[]) =>
      formatter(intl, value, ...args),
    );
  }

  return liquid;
}
