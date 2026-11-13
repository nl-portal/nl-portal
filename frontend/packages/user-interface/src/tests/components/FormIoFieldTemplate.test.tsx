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
import { beforeAll, describe, expect, it } from "vitest";
import { Formio } from "@formio/js";
import { Templates } from "@formio/react";
import {
  nlPortalField,
  nlPortalFieldTemplates,
} from "../../components/formio/FormIoFieldTemplate";
import { nlPortalInput } from "../../components/formio/FormIoInputTemplate";
import { nlPortalSelect } from "../../components/formio/FormIoSelectTemplate";
import { nlPortalRadioSelectBoxesWrapper } from "../../components/formio/FormIoRadioSelectBoxesWrapper";
import { nlPortalSingleCheckbox } from "../../components/formio/FormIoSingleCheckboxTemplate";
import { nlPortalButton } from "../../components/formio/FormIoButtonTemplate";
import { nlPortalDataGrid } from "../../components/formio/FormIoDataGridTemplate";
import { nlPortalAddress } from "../../components/formio/FormIoAddressTemplate";
import { nlPortalWell } from "../../components/formio/FormioWellTemplate";
import FormIoDatePicker from "../../components/formio/FormIoDatePicker";

const TEMPLATE = "nl-portal-test";

const datetimeComponent = (extra: Record<string, unknown> = {}) => ({
  key: "verwachteIngangsdatumHuurcontract",
  type: "datetime",
  input: true,
  label: "Verwachte ingangsdatum huurcontract",
  format: "dd-MM-yyyy",
  enableTime: false,
  ...extra,
});

const renderForm = async (components: unknown[]) => {
  const element = document.createElement("div");
  document.body.appendChild(element);
  await Formio.createForm(
    element,
    { display: "form", components },
    { template: TEMPLATE, noAlerts: true },
  );
  return element;
};

describe("FormIoFieldTemplate", () => {
  beforeAll(() => {
    FormIoDatePicker.register();
    const base = Templates.templates.bootstrap || Templates.current || {};
    Templates.templates[TEMPLATE] = {
      ...base,
      ...nlPortalFieldTemplates,
      address: nlPortalAddress,
      input: nlPortalInput,
      well: nlPortalWell,
      select: nlPortalSelect,
      radio: nlPortalRadioSelectBoxesWrapper,
      checkbox: nlPortalSingleCheckbox,
      button: nlPortalButton,
      datagrid: nlPortalDataGrid,
    };
  });

  it("renders only the element", () => {
    expect(
      nlPortalField.form({
        element: "<div>x</div>",
        labelMarkup: "<label>y</label>",
      }),
    ).toBe("<div>x</div>");
  });

  it("registers field templates only for types that render their own label", () => {
    expect(nlPortalFieldTemplates["field-datetime"]).toBe(nlPortalField);
    expect(nlPortalFieldTemplates["field-textfield"]).toBe(nlPortalField);
    expect(nlPortalFieldTemplates["field-checkbox"]).toBeUndefined();
    expect(nlPortalFieldTemplates["field-day"]).toBeUndefined();
    expect(nlPortalFieldTemplates["field-datagrid"]).toBeUndefined();
  });

  it("renders a single label and description for a datetime component", async () => {
    const element = await renderForm([
      datetimeComponent({ description: "Datum waarop het contract ingaat" }),
    ]);
    const component = element.querySelector(".formio-component-datetime");

    expect(component).not.toBeNull();
    expect(
      component!.querySelectorAll("label.utrecht-form-label"),
    ).toHaveLength(1);
    expect(component!.querySelectorAll("label.col-form-label")).toHaveLength(0);
    expect(
      component!.querySelectorAll(".utrecht-form-field-description"),
    ).toHaveLength(1);
    expect(component!.querySelectorAll(".form-text.text-muted")).toHaveLength(
      0,
    );
  });

  it("hides the label and names the input for a datetime component with hideLabel", async () => {
    const element = await renderForm([datetimeComponent({ hideLabel: true })]);
    const component = element.querySelector(".formio-component-datetime");

    expect(
      component!.querySelectorAll("label.utrecht-form-label"),
    ).toHaveLength(0);
    expect(component!.querySelectorAll("label.col-form-label")).toHaveLength(0);
    expect(
      component!.querySelector("input[aria-label]")!.getAttribute("aria-label"),
    ).toBe("Verwachte ingangsdatum huurcontract");
  });

  it("keeps the stock label for a day component", async () => {
    const element = await renderForm([
      {
        key: "geboortedatum",
        type: "day",
        input: true,
        label: "Geboortedatum",
      },
    ]);
    const component = element.querySelector(".formio-component-day");

    expect(component!.querySelectorAll("label.col-form-label")).toHaveLength(1);
  });
});
