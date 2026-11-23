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
  cloneElement,
  createElement,
  isValidElement,
  type ElementType,
  type ReactNode,
} from "react";
import {
  Hash,
  Tag,
  TypeGuards,
  type Liquid,
  type Context,
  type Emitter,
  type Parser,
  type TagToken,
  type TopLevelToken,
  type Template,
} from "liquidjs";
import { resolveValues, resolveValuesDeep } from "./values";
import { tableTag } from "./tags/table";
import { descriptionListTag } from "./tags/description-list";
import { headingTag } from "./tags/heading";
import { paragraphTag } from "./tags/paragraph";
import { linkTag } from "./tags/link";
import { backLinkTag } from "./tags/back-link";
import { imageTag } from "./tags/image";
import { filterTag } from "./tags/filter";
import { alertTag } from "./tags/alert";
import { tabsTag } from "./tags/tabs";
import { searchTag } from "./tags/search";
import { linkListTag } from "./tags/link-list";
import { unorderedListTag } from "./tags/unordered-list";
import { richTextTag } from "./tags/rich-text";
import { pageHeaderTag } from "./tags/page-header";
import { themeTasksTag } from "./tags/theme-tasks";
import { themeCasesTag } from "./tags/theme-cases";

/**
 * A template tag that renders [component].
 *
 * [props] maps tag arguments to component props. A tag with a [body] needs an end tag and
 * renders its body as children unless [body.parse] converts it to props. Without a [body],
 * the tag needs no end tag. [validate] may reject the final props by throwing.
 *
 * [body.parse] gets the body with value placeholders (see `values.ts`) and returns plain data; the placeholders
 * in its strings are resolved before [validate], so structure never comes from data. In children, a newline in
 * the template is a line break and a newline in a value is not.
 */
export type LiquidTagDefinition = {
  component: ElementType;
  props: Record<string, string>;
  body?: { parse?: (body: string) => Record<string, unknown> };
  validate?: (props: Record<string, unknown>) => void;
};

export const LIQUID_TAGS: Record<string, LiquidTagDefinition> = {
  "description-list": descriptionListTag,
  table: tableTag,
  heading: headingTag,
  paragraph: paragraphTag,
  link: linkTag,
  "back-link": backLinkTag,
  image: imageTag,
  filter: filterTag,
  alert: alertTag,
  tabs: tabsTag,
  search: searchTag,
  "link-list": linkListTag,
  "unordered-list": unorderedListTag,
  "rich-text": richTextTag,
  "page-header": pageHeaderTag,
  "theme-tasks": themeTasksTag,
  "theme-cases": themeCasesTag,
};

function toProps(
  definition: LiquidTagDefinition,
  args: Record<string, unknown>,
  bodyProps: Record<string, unknown>,
) {
  const props = { ...bodyProps };
  for (const [name, target] of Object.entries(definition.props)) {
    if (Object.prototype.hasOwnProperty.call(args, name))
      props[target] = args[name];
  }
  definition.validate?.(props);
  return props;
}

export function registerTags(liquid: Liquid) {
  for (const [name, definition] of Object.entries(LIQUID_TAGS)) {
    liquid.registerTag(
      name,
      class extends Tag {
        private templates: Template[] = [];
        private args: Hash;
        constructor(
          token: TagToken,
          tokens: TopLevelToken[],
          liquid: Liquid,
          parser: Parser,
        ) {
          super(token, tokens, liquid);
          this.args = new Hash(token.args);
          if (!definition.body) return;
          while (tokens.length) {
            const next = tokens.shift()!;
            if (TypeGuards.isTagToken(next) && next.name === `end${name}`)
              return;
            this.templates.push(parser.parseToken(next, tokens));
          }
          throw new Error(`Missing end${name} tag.`);
        }
        *render(
          ctx: Context,
          emitter: Emitter,
        ): Generator<unknown, void, unknown> {
          const args = (yield this.args.render(ctx)) as Record<string, unknown>;
          let bodyProps: Record<string, unknown> = {};
          if (definition.body?.parse) {
            const text = (yield this.liquid.renderer.renderTemplates(
              this.templates,
              ctx,
            )) as string;
            bodyProps = resolveValuesDeep(
              this.liquid,
              definition.body.parse(text),
            );
          } else if (definition.body) {
            const children: ReactNode[] = [];
            const liquid = this.liquid;
            let text = "";
            const flushText = (last = false) => {
              if (!children.length) text = text.trimStart();
              if (last) text = text.trimEnd();
              text.split(/\r?\n/).forEach((line, index) => {
                if (index)
                  children.push(createElement("br", { key: children.length }));
                const value = resolveValues(liquid, line);
                if (value) children.push(value);
              });
              text = "";
            };
            yield this.liquid.renderer.renderTemplates(this.templates, ctx, {
              buffer: "",
              write(value: unknown) {
                if (isValidElement(value)) {
                  flushText();
                  children.push(cloneElement(value, { key: children.length }));
                } else if (value != null) {
                  text += String(value);
                }
              },
            } satisfies Emitter);
            flushText(true);
            bodyProps = { children };
          }
          emitter.write(
            createElement(
              definition.component,
              toProps(definition, args, bodyProps),
            ),
          );
        }
      },
    );
  }
}
