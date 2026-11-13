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
import { LinkProps } from "@gemeente-denhaag/link";
import LinkList from "../../../components/LinkList";
import type { LiquidTagDefinition } from "../tags";
import { isAllowedHref, pipeRows } from "../text";

type Item = { label: string; href: string };

const LiquidLinkList = ({ items }: { items: Item[] }) => (
  <LinkList
    titleTranslationId={null}
    links={items.map(({ label, href }): LinkProps => ({
      href,
      children: label,
    }))}
  />
);

/**
 * A list of links with `label | href` rows in the body. Every `href` is relative or uses https, http, tel or mailto.
 *
 * @example
 * {% link-list %}
 * {{ 'zaken' | i18n }} | /zaken
 * Gemeente Den Haag | https://www.denhaag.nl
 * {% endlink-list %}
 */
export const linkListTag: LiquidTagDefinition = {
  component: LiquidLinkList,
  props: {},
  body: {
    parse: (body) => ({
      items: pipeRows(body).map(({ left, right }) => {
        if (right === undefined)
          throw new Error("Link list row needs label | href.");
        return { label: left, href: right };
      }),
    }),
  },
  validate: (props) => {
    if (
      (props.items as Item[]).some(
        (item) => !item.href || !isAllowedHref(item.href),
      )
    ) {
      throw new Error(
        "Link list href must be relative or use an allowed protocol.",
      );
    }
  },
};
