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
import { filterEmptyParams } from "../../utils/filter-empty-params.ts";

describe("filterEmptyParams", () => {
  it("returns undefined when input is undefined", () => {
    expect(filterEmptyParams(undefined)).toBeUndefined();
  });

  it("removes undefined, null and empty string values", () => {
    const input = {
      a: "1",
      b: "",
      c: undefined as unknown as string,
      d: null as unknown as string,
      e: "ok",
    };
    expect(filterEmptyParams(input)).toEqual({ a: "1", e: "ok" });
  });

  it("returns undefined when all values are filtered out", () => {
    const input = { a: "", b: "" };
    expect(filterEmptyParams(input)).toBeUndefined();
  });

  it("keeps all non-empty values intact", () => {
    const input = { prompt: "login", kc_idp_hint: "digid" };
    expect(filterEmptyParams(input)).toEqual(input);
  });
});
