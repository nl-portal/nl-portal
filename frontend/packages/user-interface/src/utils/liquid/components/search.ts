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
import LiquidSearch from "../../../components/liquid/LiquidSearch";
import type { LiquidComponentDefinition } from "../components";

/**
 * A search field over a binding of a dynamic page, without end tag. Submitting sets `param` to the text,
 * an empty text removes the override. `binding`, `param` and `page-param` are names (quoted); `page-param`
 * is reset on submit. `title` is a message id for the label, shown as is when no message exists.
 *
 * @example
 * {% search binding: "berichten" param: "filter.q" page-param: "page" title: "zoeken" %}
 */
export const searchDefinition: LiquidComponentDefinition = {
  component: LiquidSearch,
  props: {
    binding: "binding",
    param: "param",
    title: "title",
    "page-param": "pageParam",
  },
};
