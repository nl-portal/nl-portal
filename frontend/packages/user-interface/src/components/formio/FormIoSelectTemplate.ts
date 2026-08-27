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
  errorsBlock,
  serializeAttrs,
  wrapperOpen,
} from "./FormIoTemplateUtils";

export const nlPortalSelect = {
  form: (ctx: any) => {
    const { component } = ctx;
    const selectId =
      ctx.input?.id || ctx.instance?.id || component.key || "select";

    const hasErrors = Array.isArray(ctx.errors) && ctx.errors.length > 0;
    const errorId = `err-${selectId}`;

    const attrObj =
      typeof ctx.input?.attr === "object" ? ctx.input.attr || {} : {};
    const cssClass = [
      "utrecht-select",
      "utrecht-select--html-select",
      "denhaag-select",
      hasErrors && "utrecht-select--invalid",
      attrObj.class,
    ]
      .filter(Boolean)
      .join(" ");

    if (attrObj.class) delete attrObj.class;

    const needsDescribedBy =
      hasErrors &&
      !(
        attrObj["aria-describedby"] ||
        (typeof ctx.input?.attr === "string" &&
          ctx.input.attr.includes("aria-describedby"))
      );

    const hasNameInAttr =
      (typeof ctx.input?.attr === "string" &&
        /(?:^|\s)name=/.test(ctx.input.attr)) ||
      (typeof ctx.input?.attr === "object" && !!ctx.input.attr?.name);

    const extras: any = {
      class: cssClass,
      required: !!component.validate?.required || undefined,
      disabled: !!ctx.disabled || undefined,
      "aria-invalid": hasErrors ? "true" : undefined,
      "aria-describedby": needsDescribedBy ? errorId : undefined,
      multiple: component.multiple || undefined,
      name: hasNameInAttr ? undefined : `data[${component.key}]`,
    };

    if (!attrObj.id) {
      extras.id = selectId;
    }

    const selectAttributes = serializeAttrs(ctx.input?.attr, extras, [
      "type",
      "value",
    ]);

    const ref = ctx.input?.ref || "selectContainer";

    if (component.type === "day") {
      return `
        <select ref="${ref}" ${selectAttributes}>
          ${ctx.selectOptions || ""}
        </select>
      `;
    }

    return `
      ${wrapperOpen(ctx, selectId, "text")}
        <div>
          <select ref="${ref}" ${selectAttributes}>
           ${ctx.selectOptions || ""}
         </select>
        </div>
        ${errorsBlock(ctx, selectId)}
      </div>
    `;
  },
};
