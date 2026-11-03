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
import type { LiquidComponentDefinition } from "../components";
import { REGEX_PATTERNS } from "../../../constants/regex-patterns";

const LiquidImage = ({ src, alt = "" }: { src: string; alt?: string }) => (
  <img src={src} alt={alt} style={{ maxWidth: "100%" }} />
);

/**
 * An image without end tag. `src` is relative or uses https.
 *
 * @example
 * {% image src: "https://www.denhaag.nl/logo.svg" alt: "Logo" %}
 */
export const imageDefinition: LiquidComponentDefinition = {
  component: LiquidImage,
  props: { src: "src", alt: "alt" },
  validate: (props) => {
    const src = props.src;
    const relative =
      typeof src === "string" &&
      !REGEX_PATTERNS.urlProtocol.test(src) &&
      !src.startsWith("//");
    if (
      typeof src !== "string" ||
      !src ||
      !(relative || src.toLowerCase().startsWith("https:"))
    ) {
      throw new Error("Image src must be relative or use https.");
    }
  },
};
