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

/**
 * Form.io `field` template for components whose nl-portal template renders its own label
 * and description. Replaces the stock bootstrap `field` template, which would add a second label.
 */
export const nlPortalField = {
  form: (ctx: any) => ctx.element,
};

/**
 * Component types whose nl-portal template (input, select, address, radio) renders the label.
 */
export const OWN_LABEL_COMPONENT_TYPES = [
  "textfield",
  "email",
  "password",
  "number",
  "phoneNumber",
  "url",
  "currency",
  "time",
  "textarea",
  "datetime",
  "select",
  "address",
  "radio",
  "selectboxes",
];

/**
 * `field-<type>` template entries, resolved by Form.io before the generic `field` template.
 */
export const nlPortalFieldTemplates = Object.fromEntries(
  OWN_LABEL_COMPONENT_TYPES.map((type) => [`field-${type}`, nlPortalField]),
);
