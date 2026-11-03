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
import type { FilterImplOptions, Liquid } from "liquidjs";
import { type IntlShape } from "react-intl";
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

type FilterHandler = (
  this: unknown,
  value: unknown,
  ...args: unknown[]
) => unknown;

const handlerOf = (filter: FilterImplOptions | undefined) =>
  (typeof filter === "function" ? filter : filter?.handler) as
    FilterHandler | undefined;

/**
 * The `i18n` filter: formats message `id` with keyword arguments as ICU values, like every portal message:
 * an unknown id shows the id.
 */
const createI18nFilter =
  (intl: IntlShape) =>
  (id: unknown, ...args: unknown[]) => {
    const values = Object.fromEntries(
      args.filter(
        (arg): arg is [string, unknown] =>
          Array.isArray(arg) && arg.length === 2,
      ),
    ) as Record<string, string | number | boolean | Date | null | undefined>;
    return intl.formatMessage({ id: String(id) }, values);
  };

/**
 * Registers the portal formatters. `date` without an argument formats as an intl short date;
 * with an strftime argument it delegates to the built-in Liquid `date` filter.
 * `i18n` formats a message of [intl], which includes the page's own messages.
 */
export function registerFormatters(liquid: Liquid, intl: IntlShape) {
  const builtInDate = handlerOf(liquid.filters.date);

  liquid.registerFilter(
    "date",
    function (this: unknown, value: unknown, ...args: unknown[]) {
      if (args.length && builtInDate) {
        return builtInDate.call(this, value, ...args);
      }
      return LIQUID_FORMATTERS.date(intl, value);
    },
  );
  liquid.registerFilter("currency", (value: unknown, currency?: string) =>
    LIQUID_FORMATTERS.currency(intl, value, currency),
  );
  liquid.registerFilter("i18n", createI18nFilter(intl));
}
