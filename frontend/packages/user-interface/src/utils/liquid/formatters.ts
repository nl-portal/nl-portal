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
 * `i18n`: formats the message `id` from the page's messages with keyword arguments as ICU values,
 * like every portal message. An unknown id shows the id.
 *
 * @example
 * {{ 'aantal' | i18n: count: producten.total }}
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
 * `date`: without an argument a short date in the user's language; with an strftime argument the built-in
 * LiquidJS `date`.
 *
 * @example
 * {{ startDatum | date }} {{ startDatum | date: "%Y-%m-%d" }}
 */
const createDateFilter = (liquid: Liquid, intl: IntlShape) => {
  const builtInDate = handlerOf(liquid.filters.date);
  return function (this: unknown, value: unknown, ...args: unknown[]) {
    if (args.length && builtInDate) {
      return builtInDate.call(this, value, ...args);
    }
    return LIQUID_FORMATTERS.date(intl, value);
  };
};

/**
 * `currency`: an amount in the user's language, euro unless the currency code is given.
 *
 * @example
 * {{ prijs | currency }} {{ prijs | currency: "USD" }}
 */
const createCurrencyFilter =
  (intl: IntlShape) => (value: unknown, currency?: string) =>
    LIQUID_FORMATTERS.currency(intl, value, currency);

/**
 * `html_text`: the text of an HTML or HTML-encoded value: tags removed and every entity decoded, parsed in an
 * inert document. `null` gives `""`, other values are stringified. The result is still shown as text.
 *
 * @example
 * {{ omschrijving | html_text }}
 */
export const htmlText = (value: unknown): string => {
  if (value === null || value === undefined) return "";
  return (
    new DOMParser().parseFromString(String(value), "text/html").body
      .textContent ?? ""
  );
};

/**
 * `lines`: the lines of a value, split on `\n` or `\r\n` and trimmed. `null` gives no lines.
 *
 * @example
 * {% assign regels = adres | lines %}{% for regel in regels %}{{ regel }}
 * {% endfor %}
 */
export const lines = (value: unknown): string[] => {
  if (value === null || value === undefined) return [];
  return String(value)
    .split(/\r?\n/)
    .map((line) => line.trim());
};

const PORTAL_FILTER_FACTORIES = {
  date: createDateFilter,
  currency: (_: Liquid, intl: IntlShape) => createCurrencyFilter(intl),
  i18n: (_: Liquid, intl: IntlShape) => createI18nFilter(intl),
  html_text: () => htmlText,
  lines: () => lines,
} satisfies Record<
  string,
  (liquid: Liquid, intl: IntlShape) => (...args: never[]) => unknown
>;

/** The names of every filter the portal registers: its own and the LiquidJS built-ins it overrides. */
export const PORTAL_FILTERS = Object.keys(
  PORTAL_FILTER_FACTORIES,
) as (keyof typeof PORTAL_FILTER_FACTORIES)[];

/** Registers every filter in [PORTAL_FILTERS] on [liquid]; `i18n` formats messages of [intl]. */
export function registerFormatters(liquid: Liquid, intl: IntlShape) {
  for (const name of PORTAL_FILTERS) {
    liquid.registerFilter(
      name,
      PORTAL_FILTER_FACTORIES[name](liquid, intl) as FilterHandler,
    );
  }
}
