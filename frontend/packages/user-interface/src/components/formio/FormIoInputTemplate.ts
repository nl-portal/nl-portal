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
import { escape } from "lodash-es";
import {
  errorsBlock,
  serializeAttrs,
  wrapperOpen,
} from "./FormIoTemplateUtils";

const renderInputElement = (ctx: any) => {
  const key = escape(ctx.component.key || "textfield");
  const fallbackId = ctx.instance?.id || key;
  const inputId = escape(ctx.input?.id || fallbackId);

  const hasErrors = Array.isArray(ctx.errors) && ctx.errors.length > 0;
  const baseClass = `utrecht-textbox utrecht-textbox--html-input${hasErrors ? " utrecht-textbox--invalid" : ""}`;

  const attrObj =
    typeof ctx.input?.attr === "object" ? ctx.input.attr || {} : {};
  const combinedClass = [baseClass, attrObj.class].filter(Boolean).join(" ");
  if (attrObj.class) delete attrObj.class;

  const placeholder = ctx.t(ctx.component.placeholder || "");
  const errorId = `err-${inputId}`;
  const needsDescribedBy =
    hasErrors &&
    !(
      (typeof ctx.input?.attr === "string" &&
        ctx.input.attr.includes("aria-describedby")) ||
      (typeof ctx.input?.attr === "object" &&
        ctx.input.attr?.["aria-describedby"])
    );

  const extras = {
    id: inputId,
    name: ctx.input?.name || `data[${key}]`,
    type: ctx.input?.type || ctx.component.inputType || "text",
    dir: "auto",
    placeholder,
    required: !!ctx.component.validate?.required || undefined,
    disabled: !!(ctx.disabled || ctx.component.disabled) || undefined,
    class: combinedClass,
    "aria-describedby": needsDescribedBy ? errorId : undefined,
  };

  const inputAttributes = serializeAttrs(ctx.input?.attr, extras);
  const ref = ctx.input?.ref || "input";

  if (ctx.component?.type === "day") {
    return `<input ref="${ref}" ${inputAttributes} />`;
  }

  return `
    ${wrapperOpen(ctx, inputId, "text")}
      <label class="pra-textbox" for="${inputId}">
        <input ref="${ref}" ${inputAttributes} />
      </label>
      ${errorsBlock(ctx, inputId)}
    </div>
  `;
};

export const renderTextareaElement = (ctx: any) => {
  const key = escape(ctx.component.key || "textarea");
  const fallbackId = ctx.instance?.id || key;
  const inputId = escape(ctx.input?.id || fallbackId);
  const hasErrors = Array.isArray(ctx.errors) && ctx.errors.length > 0;
  const isDisabled = !!(ctx.disabled || ctx.component.disabled);
  const baseClass = `utrecht-textarea utrecht-textarea--html-textarea${hasErrors ? " utrecht-textarea--invalid" : ""}${isDisabled ? " utrecht-textarea--disabled" : ""}`;

  const attrObj =
    typeof ctx.input?.attr === "object" ? ctx.input.attr || {} : {};
  const combinedClass = [baseClass, attrObj.class].filter(Boolean).join(" ");
  if (attrObj.class) delete attrObj.class;

  // waarde uit attr (object of string) -> content van <textarea>
  const valueFromAttr =
    typeof ctx.input?.attr === "object"
      ? (ctx.input.attr?.value ?? "")
      : typeof ctx.input?.attr === "string"
        ? (ctx.input.attr.match(/\bvalue="([^"]*)"/)?.[1] ?? "")
        : "";

  const placeholder = ctx.t(ctx.component.placeholder || "");
  const errorId = `err-${inputId}`;
  const needsDescribedBy =
    hasErrors &&
    !(
      (typeof ctx.input?.attr === "string" &&
        ctx.input.attr.includes("aria-describedby")) ||
      (typeof ctx.input?.attr === "object" &&
        ctx.input.attr?.["aria-describedby"])
    );

  const extras = {
    id: inputId,
    name: ctx.input?.name || `data[${key}]`,
    dir: "auto",
    placeholder,
    required: !!ctx.component.validate?.required || undefined,
    disabled: isDisabled || undefined,
    class: combinedClass,
    "aria-describedby": needsDescribedBy ? errorId : undefined,
    rows: ctx.component.rows || undefined,
    cols: ctx.component.cols || undefined,
  };

  const textareaAttributes = serializeAttrs(ctx.input?.attr, extras, ["value"]);

  return `
    ${wrapperOpen(ctx, inputId, "text")}
      <label class="pra-textarea" for="${inputId}">
        <textarea ref="input" ${textareaAttributes}>${escape(String(valueFromAttr ?? ""))}</textarea>
      </label>
      ${errorsBlock(ctx, inputId)}
    </div>
  `;
};

export const nlPortalInput = {
  form: (ctx: any) => {
    return ctx.component?.type === "textarea"
      ? renderTextareaElement(ctx)
      : renderInputElement(ctx);
  },
};
