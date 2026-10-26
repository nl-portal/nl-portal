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
import { describe, expect, it } from "vitest";
import { nlPortalInput } from "../../components/formio/FormIoInputTemplate";
import { nlPortalSelect } from "../../components/formio/FormIoSelectTemplate";
import { nlPortalRadioSelectBoxesWrapper } from "../../components/formio/FormIoRadioSelectBoxesWrapper";
import { nlPortalAddress } from "../../components/formio/FormIoAddressTemplate";

const LABEL = "Verwachte ingangsdatum huurcontract";

const makeCtx = (component: Record<string, unknown>) => ({
  component: { key: "field", label: LABEL, ...component },
  t: (s: string) => s,
  input: { id: "field-id", attr: {} },
  instance: { id: "field-id" },
});

const parse = (html: string) => {
  const element = document.createElement("div");
  element.innerHTML = html;
  return element;
};

describe("nlPortalInput", () => {
  it.each(["textfield", "datetime"])(
    "renders a visible label for %s",
    (type) => {
      const element = parse(nlPortalInput.form(makeCtx({ type })));

      expect(element.querySelectorAll("label.utrecht-form-label")).toHaveLength(
        1,
      );
      expect(element.querySelector("input")!.hasAttribute("aria-label")).toBe(
        false,
      );
    },
  );

  it.each(["textfield", "datetime"])(
    "hides the label and sets aria-label for %s with hideLabel",
    (type) => {
      const element = parse(
        nlPortalInput.form(makeCtx({ type, hideLabel: true })),
      );

      expect(element.querySelectorAll("label.utrecht-form-label")).toHaveLength(
        0,
      );
      expect(element.querySelector("input")!.getAttribute("aria-label")).toBe(
        LABEL,
      );
    },
  );

  it("hides the label and sets aria-label for a textarea with hideLabel", () => {
    const visible = parse(nlPortalInput.form(makeCtx({ type: "textarea" })));
    const hidden = parse(
      nlPortalInput.form(makeCtx({ type: "textarea", hideLabel: true })),
    );

    expect(visible.querySelectorAll("label.utrecht-form-label")).toHaveLength(
      1,
    );
    expect(hidden.querySelectorAll("label.utrecht-form-label")).toHaveLength(0);
    expect(hidden.querySelector("textarea")!.getAttribute("aria-label")).toBe(
      LABEL,
    );
  });
});

describe("nlPortalSelect", () => {
  it("renders a visible label", () => {
    const element = parse(nlPortalSelect.form(makeCtx({ type: "select" })));

    expect(element.querySelectorAll("label.utrecht-form-label")).toHaveLength(
      1,
    );
    expect(element.querySelector("select")!.hasAttribute("aria-label")).toBe(
      false,
    );
  });

  it("hides the label and sets aria-label with hideLabel", () => {
    const element = parse(
      nlPortalSelect.form(makeCtx({ type: "select", hideLabel: true })),
    );

    expect(element.querySelectorAll("label.utrecht-form-label")).toHaveLength(
      0,
    );
    expect(element.querySelector("select")!.getAttribute("aria-label")).toBe(
      LABEL,
    );
  });

  it("renders no label and no aria-label for day selects", () => {
    const element = parse(
      nlPortalSelect.form(makeCtx({ type: "day", hideLabel: true })),
    );

    expect(element.querySelectorAll("label")).toHaveLength(0);
    expect(element.querySelector("select")!.hasAttribute("aria-label")).toBe(
      false,
    );
  });
});

describe("nlPortalRadioSelectBoxesWrapper", () => {
  const values = [{ label: "Ja", value: "ja" }];

  it.each(["radio", "selectboxes"])(
    "renders a visible legend for %s",
    (type) => {
      const element = parse(
        nlPortalRadioSelectBoxesWrapper.form(makeCtx({ type, values })),
      );

      expect(element.querySelector("legend")!.textContent).toBe(LABEL);
      expect(
        element.querySelector("fieldset")!.hasAttribute("aria-label"),
      ).toBe(false);
    },
  );

  it.each(["radio", "selectboxes"])(
    "hides the legend and sets aria-label for %s with hideLabel",
    (type) => {
      const element = parse(
        nlPortalRadioSelectBoxesWrapper.form(
          makeCtx({ type, values, hideLabel: true }),
        ),
      );

      expect(element.querySelector("legend")).toBeNull();
      expect(
        element.querySelector("fieldset")!.getAttribute("aria-label"),
      ).toBe(LABEL);
    },
  );
});

describe("nlPortalAddress", () => {
  const addressCtx = (component: Record<string, unknown>) => ({
    ...makeCtx({ type: "address", ...component }),
    mode: { autocomplete: true },
  });

  it("uses the autocomplete aria-label when the label is visible", () => {
    const element = parse(nlPortalAddress.form(addressCtx({})));

    expect(element.querySelectorAll("label.utrecht-form-label")).toHaveLength(
      1,
    );
    expect(element.querySelector("input")!.getAttribute("aria-label")).toBe(
      "autocomplete",
    );
  });

  it("uses the component label as aria-label with hideLabel", () => {
    const element = parse(
      nlPortalAddress.form(addressCtx({ hideLabel: true })),
    );

    expect(element.querySelectorAll("label.utrecht-form-label")).toHaveLength(
      0,
    );
    expect(element.querySelector("input")!.getAttribute("aria-label")).toBe(
      LABEL,
    );
  });
});
