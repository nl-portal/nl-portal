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
import { errorsBlock, serializeAttrs } from "./FormIoTemplateUtils";
import { escape } from "lodash-es";

export const nlPortalSingleCheckbox = {
  form: (ctx: any) => {
    const { component } = ctx;

    const baseId = ctx.instance?.id || component.key || "checkbox";
    const inputId = escape(ctx.input?.id || `${baseId}-input`);

    const name = ctx.input?.name || `data[${component.key}]`;

    const hasErrors = Array.isArray(ctx.errors) && ctx.errors.length > 0;
    const errorId = `err-${inputId}`;

    const wrapperClass = `utrecht-form-field utrecht-form-field--checkbox${hasErrors ? " utrecht-form-field--invalid" : ""}`;
    const baseInputClass = `utrecht-checkbox utrecht-checkbox--html-input utrecht-checkbox--custom utrecht-form-field__input${hasErrors ? " utrecht-checkbox--invalid" : ""}`;

    const attrObj =
      typeof ctx.input?.attr === "object" ? ctx.input.attr || {} : {};
    const combinedClass = [baseInputClass, attrObj.class]
      .filter(Boolean)
      .join(" ");
    if (attrObj.class) delete attrObj.class;

    const attrHasChecked =
      (typeof ctx.input?.attr === "string" &&
        /\bchecked(\s|=|>)/.test(ctx.input.attr)) ||
      (typeof ctx.input?.attr === "object" && !!ctx.input.attr.checked);

    const extras = {
      id: inputId,
      name,
      type: "checkbox",
      required: !!component.validate?.required || undefined,
      disabled: !!ctx.disabled || undefined,
      class: combinedClass,
      "aria-describedby": hasErrors ? errorId : undefined,
      checked: !attrHasChecked && !!ctx.dataValue ? true : undefined,
      value:
        typeof component.value !== "undefined"
          ? escape(String(component.value))
          : undefined,
    };

    const inputAttributes = serializeAttrs(ctx.input?.attr, extras);

    const labelText = escape(ctx.t(component.label || ""));

    return `
      <div class="${wrapperClass}" ref="element">
        <p class="nl-paragraph utrecht-form-field__label utrecht-form-field__label--checkbox">
          <label class="utrecht-form-label utrecht-form-label--checkbox" ref="label">
            <input ref="input" ${inputAttributes} />
            ${labelText}
          </label>
        </p>
        ${errorsBlock(ctx, inputId)}
      </div>
    `;
  },
};
