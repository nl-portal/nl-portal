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
import {
  UnorderedList,
  UnorderedListItem,
} from "@gemeente-denhaag/unorderedlist";
import type { LiquidTagDefinition } from "../tags";
import { bodyText } from "../text";

const LiquidUnorderedList = ({ items }: { items: string[] }) => (
  <UnorderedList>
    {items
      .filter((item) => item.trim())
      .map((item, index) => (
        <UnorderedListItem key={index}>{item}</UnorderedListItem>
      ))}
  </UnorderedList>
);

/**
 * A bulleted list with one item per non-blank line of the body. A newline in a value doesn't start an item;
 * use the `lines` filter for one item per line of data.
 *
 * @example
 * {% unordered-list %}
 * Een geldig identiteitsbewijs
 * Het kenteken van uw auto
 * {% endunordered-list %}
 */
export const unorderedListTag: LiquidTagDefinition = {
  component: LiquidUnorderedList,
  props: {},
  body: {
    parse: (body) => ({
      items: body.split(/\r?\n/).map(bodyText).filter(Boolean),
    }),
  },
};
