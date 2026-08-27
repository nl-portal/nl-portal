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
export const formIoUploaderEditForm = () => ({
  components: [
    { key: "type", type: "hidden" },
    {
      type: "textfield",
      input: true,
      key: "label",
      label: "Label",
      placeholder: "Label",
      validate: {
        required: true,
      },
    },
    {
      type: "textfield",
      input: true,
      key: "key",
      label: "Property Name",
      placeholder: "Property Name",
      tooltip: "The name of this field in the API endpoint.",
      validate: {
        required: true,
      },
    },
    {
      type: "checkbox",
      input: true,
      inputType: "checkbox",
      key: "multipleFiles",
      label: "Multiple",
      tooltip: "Allow multiple files to be uploaded.",
      defaultValue: true,
      validate: {
        required: false,
      },
    },
  ],
});
