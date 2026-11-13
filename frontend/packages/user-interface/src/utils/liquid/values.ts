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
import type { Liquid } from "liquidjs";

const START = "\uE000";
const END = "\uE001";
const TOKEN = /\uE000(\d+)\uE001/g;
const MARKERS = /[\uE000\uE001]/g;

const tables = new WeakMap<Liquid, string[]>();

const stringify = (value: unknown): string => {
  if (value === null || value === undefined) return "";
  if (Array.isArray(value)) return value.map(stringify).join("");
  return String(value);
};

/**
 * The `outputEscape` of a portal engine: stores the output [value] as text in the value table of [liquid] and
 * returns a placeholder token, so data never becomes template structure. The token markers are removed from the
 * value first, so data can't forge a token.
 */
export const storeValue = (liquid: Liquid, value: unknown): string => {
  let table = tables.get(liquid);
  if (!table) {
    table = [];
    tables.set(liquid, table);
  }
  table.push(stringify(value).replace(MARKERS, ""));
  return `${START}${table.length - 1}${END}`;
};

/** Replaces every placeholder token in [text] with its stored value. Text outside tokens is template text. */
export const resolveValues = (liquid: Liquid, text: string): string => {
  const table = tables.get(liquid) ?? [];
  return text.replace(TOKEN, (_, index: string) => table[Number(index)] ?? "");
};

const isPlainObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" &&
  value !== null &&
  Object.getPrototypeOf(value) === Object.prototype;

/** [value] with the tokens resolved in every string, recursively through arrays and plain objects. */
export const resolveValuesDeep = <T>(liquid: Liquid, value: T): T => {
  if (typeof value === "string") return resolveValues(liquid, value) as T;
  if (Array.isArray(value))
    return value.map((item) => resolveValuesDeep(liquid, item)) as T;
  if (isPlainObject(value))
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [
        key,
        resolveValuesDeep(liquid, item),
      ]),
    ) as T;
  return value;
};
