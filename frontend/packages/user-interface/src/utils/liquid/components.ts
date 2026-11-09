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
import { decodeLiquidText } from "./decodeLiquidText";
import { tableDefinition } from "./components/table";
import { descriptionListDefinition } from "./components/description-list";
import { headingDefinition } from "./components/heading";
import { paragraphDefinition } from "./components/paragraph";
import { linkDefinition } from "./components/link";
import { imageDefinition } from "./components/image";
import { filterDefinition } from "./components/filter";
import { alertDefinition } from "./components/alert";
import { tabsDefinition } from "./components/tabs";
import { searchDefinition } from "./components/search";
import { linkListDefinition } from "./components/link-list";
import { unorderedListDefinition } from "./components/unordered-list";

/**
 * A template tag that renders [component].
 *
 * [props] maps tag arguments to component props. A tag with a [body] needs an end tag and
 * renders its body as children unless [body.parse] converts it to props. Without a [body],
 * the tag needs no end tag. [validate] may reject the final props by throwing.
 */
export type LiquidComponentDefinition = {
  component: ElementType;
  props: Record<string, string>;
  body?: { parse?: (body: string) => Record<string, unknown> };
  validate?: (props: Record<string, unknown>) => void;
};

export const LIQUID_COMPONENTS: Record<string, LiquidComponentDefinition> = {
  "description-list": descriptionListDefinition,
  table: tableDefinition,
  heading: headingDefinition,
  paragraph: paragraphDefinition,
  link: linkDefinition,
  image: imageDefinition,
  filter: filterDefinition,
  alert: alertDefinition,
  tabs: tabsDefinition,
  search: searchDefinition,
  "link-list": linkListDefinition,
  "unordered-list": unorderedListDefinition,
};

function toProps(
  definition: LiquidComponentDefinition,
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

export function registerComponents(liquid: Liquid) {
  for (const [name, definition] of Object.entries(LIQUID_COMPONENTS)) {
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
            bodyProps = definition.body.parse(text);
          } else if (definition.body) {
            const children: ReactNode[] = [];
            let text = "";
            const flushText = (last = false) => {
              if (!children.length) text = text.trimStart();
              if (last) text = text.trimEnd();
              decodeLiquidText(text)
                .split(/\r?\n/)
                .forEach((line, index) => {
                  if (index)
                    children.push(
                      createElement("br", { key: children.length }),
                    );
                  if (line) children.push(line);
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
