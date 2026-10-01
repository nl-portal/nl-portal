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
import { createElement, type ReactNode, type ElementType } from "react";
import DescriptionList from "../../components/DescriptionList";

type LiquidComponentDefinition = {
  component: ElementType;
  props: Record<string, "string" | "json">;
};

const LIQUID_COMPONENTS: Record<string, LiquidComponentDefinition> = {
  descriptionlist: {
    component: DescriptionList,
    props: {
      titleTranslationId: "string",
      items: "json",
    },
  },
};

export function parseComponents(html: string): ReactNode[] {
  const document = new DOMParser().parseFromString(html, "text/html");
  const parseNode = (node: Node, key: number): ReactNode => {
    if (node.nodeType === Node.TEXT_NODE) return node.textContent;
    if (node.nodeType !== Node.ELEMENT_NODE) return null;
    const element = node as Element;
    const name = element.tagName.toLowerCase();
    if (!Object.prototype.hasOwnProperty.call(LIQUID_COMPONENTS, name))
      return null;
    const definition = LIQUID_COMPONENTS[name];
    const props: Record<string, unknown> = { key };
    for (const attribute of Array.from(element.attributes)) {
      const name = attribute.name === "class" ? "classname" : attribute.name;
      // DOMParser lowercases attributes; use the original React prop name.
      const propName = Object.keys(definition.props).find(
        (prop) => prop.toLowerCase() === name,
      );
      if (!propName) continue;
      props[propName] =
        definition.props[propName] === "json"
          ? JSON.parse(attribute.value)
          : attribute.value;
    }
    // Template props are dynamic: the registry controls names and conversion,
    // but does not validate each component's required props or JSON structure.
    return createElement(
      definition.component,
      props,
      ...Array.from(element.childNodes).map(parseNode),
    );
  };
  return Array.from(document.body.childNodes).map(parseNode);
}
