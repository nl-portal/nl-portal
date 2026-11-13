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
import { REGEX_PATTERNS } from "../../constants/regex-patterns";

/**
 * The non-blank rows of a tag body as `left | right`, split at the first unescaped `|` and trimmed.
 * `\\|` is a literal pipe. A row without `|` has no `right`. Values are still placeholders here.
 */
export const pipeRows = (body: string) =>
  body
    .split(/\r?\n/)
    .filter((line) => line.trim())
    .map((line) => {
      const separator = line.search(/(?<!\\)\|/);
      const text = (value: string) => value.trim().replace(/\\\|/g, "|");
      return separator === -1
        ? { left: text(line), right: undefined }
        : {
            left: text(line.slice(0, separator)),
            right: text(line.slice(separator + 1)),
          };
    });

/** The rendered body of a text tag, trimmed. */
export const bodyText = (body: string) => body.trim();

/**
 * Whether [href] is a relative link or uses https, http, tel or mailto. Rejects protocol-relative links (`//host`),
 * backslashes, surrounding whitespace and control characters, so data can't point a relative-looking link at another site.
 */
export const isAllowedHref = (href: string) =>
  href === href.trim() &&
  !href.startsWith("//") &&
  !href.includes("\\") &&
  ![...href].some(
    (char) => char.charCodeAt(0) < 32 || char.charCodeAt(0) === 127,
  ) &&
  (!REGEX_PATTERNS.urlProtocol.test(href) ||
    REGEX_PATTERNS.externalLink.test(href));
