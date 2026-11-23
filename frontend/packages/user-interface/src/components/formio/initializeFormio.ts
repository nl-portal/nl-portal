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
import { Formio } from "@formio/js";
import ProtectedEval from "@formio/protected-eval";
import { Templates } from "@formio/react";
import FormIoUploader from "./FormIoUploader";
import FormIoDatePicker from "./FormIoDatePicker";
import { nlPortalInput } from "./FormIoInputTemplate";
import { nlPortalSelect } from "./FormIoSelectTemplate";
import { nlPortalSingleCheckbox } from "./FormIoSingleCheckboxTemplate";
import { nlPortalButton } from "./FormIoButtonTemplate";
import { nlPortalDataGrid } from "./FormIoDataGridTemplate";
import { nlPortalRadioSelectBoxesWrapper } from "./FormIoRadioSelectBoxesWrapper";
import { nlPortalAddress } from "./FormIoAddressTemplate";
import { nlPortalWell } from "./FormioWellTemplate";
import { nlPortalFieldTemplates } from "./FormIoFieldTemplate";

type FormioPlugin = {
  evaluator?: unknown;
  default?: FormioPlugin;
};

let initialized = false;

export function initializeFormio() {
  if (initialized) return;

  const protectedEval = ProtectedEval as FormioPlugin;
  const protectedEvalPlugin = protectedEval.evaluator
    ? protectedEval
    : protectedEval.default;
  const registerFormioPlugin = Formio.use.bind(Formio);
  registerFormioPlugin(protectedEvalPlugin);

  FormIoUploader.register();
  FormIoDatePicker.register();

  const base = Templates.templates.bootstrap || Templates.current || {};
  Templates.templates["nl-portal"] = {
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

  initialized = true;
}
