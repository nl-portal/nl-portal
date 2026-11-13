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
import type { LiquidTagDefinition } from "../tags";
import { REGEX_PATTERNS } from "../../../constants/regex-patterns";
import { isAllowedHref } from "../text";

const LiquidImage = ({ src, alt = "" }: { src: string; alt?: string }) => (
  <img src={src} alt={alt} style={{ maxWidth: "100%" }} />
);

/**
 * An image without end tag. `src` is relative or uses https, without `//`, backslashes, surrounding whitespace or
 * control characters.
 *
 * @example
 * {% image src: "https://www.denhaag.nl/logo.svg" alt: "Logo" %}
 */
export const imageTag: LiquidTagDefinition = {
  component: LiquidImage,
  props: { src: "src", alt: "alt" },
  validate: (props) => {
    const src = props.src;
    if (
      typeof src !== "string" ||
      !src ||
      !isAllowedHref(src) ||
      (REGEX_PATTERNS.urlProtocol.test(src) &&
        !src.toLowerCase().startsWith("https:"))
    ) {
      throw new Error("Image src must be relative or use https.");
    }
  },
};
