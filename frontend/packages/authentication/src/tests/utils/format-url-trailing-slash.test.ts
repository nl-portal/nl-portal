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
import { formatUrlTrailingSlash } from "../../utils/format-url-trailing-slash.ts";

describe("formatUrlTrailingSlash", () => {
  it("should remove trailing slash when slash is present but not wanted", () => {
    const formattedUrl = `${formatUrlTrailingSlash("http://www.example.com/api/", false)}`;
    const lastCharacter = formattedUrl[formattedUrl.length - 1];

    expect(lastCharacter).toBe("i");
  });

  it("should keep trailing slash when slash is present and wanted", () => {
    const formattedUrl = `${formatUrlTrailingSlash("http://www.example.com/api/", true)}`;
    const lastCharacter = formattedUrl[formattedUrl.length - 1];

    expect(lastCharacter).toBe("/");
  });

  it("should add trailing slash when slash is not present but is wanted", () => {
    const formattedUrl = `${formatUrlTrailingSlash("http://www.example.com/api", true)}`;
    const lastCharacter = formattedUrl[formattedUrl.length - 1];

    expect(lastCharacter).toBe("/");
  });

  it("should not add a trailing slash when slash is not present and not wanted", () => {
    const formattedUrl = `${formatUrlTrailingSlash("http://www.example.com/api", false)}`;
    const lastCharacter = formattedUrl[formattedUrl.length - 1];

    expect(lastCharacter).toBe("i");
  });
});
