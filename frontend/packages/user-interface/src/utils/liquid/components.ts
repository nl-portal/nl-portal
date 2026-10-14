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
import { createElement, type ElementType } from "react";
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
import { tableDefinition } from "./components/table";
import { descriptionListDefinition } from "./components/description-list";

export type LiquidComponentDefinition = {
  component: ElementType;
  props: Record<string, string>;
  body: { parse: (body: string) => Record<string, unknown> };
};

const LIQUID_COMPONENTS: Record<string, LiquidComponentDefinition> = {
  "description-list": descriptionListDefinition,
  table: tableDefinition,
};

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
          const body = (yield this.liquid.renderer.renderTemplates(
            this.templates,
            ctx,
          )) as string;
          const props = definition.body.parse(body);
          for (const [name, target] of Object.entries(definition.props)) {
            if (Object.prototype.hasOwnProperty.call(args, name))
              props[target] = args[name];
          }
          emitter.write(createElement(definition.component, props));
        }
      },
    );
  }
}
