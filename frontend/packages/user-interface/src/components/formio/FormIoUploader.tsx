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
import { Components } from "@formio/js";
import { Root, createRoot } from "react-dom/client";
import { useEffect, useState } from "react";
import { get } from "lodash-es";
import { FormField } from "@gemeente-denhaag/form-field";
import { FormLabel } from "@gemeente-denhaag/form-label";
import { LocalizationProvider } from "@nl-portal/nl-portal-localization";
import { FileUpload } from "@gemeente-denhaag/file-upload";
import { File } from "@gemeente-denhaag/file";
import { Alert } from "@gemeente-denhaag/alert";
import { Paragraph } from "@gemeente-denhaag/typography";
import { FormattedMessage, useIntl } from "react-intl";

export interface UploadedFile {
  id?: string;
  url?: string;
  name: string;
  size: number;
  isUploaded?: boolean;
}

interface PortalFileUploadProps {
  id: string;
  label?: string;
  hideLabel?: boolean;
  context: object;
  multiple: boolean;
  onChange: (fileList: UploadedFile[]) => void;
  onUploadStart: (upload: Promise<void>) => void;
  onUploadEnd: (upload: Promise<void>) => void;
  informatieobjecttype?: string;
  getBeschrijving?: () => string | undefined;
  initialValue?: UploadedFile[];
  taakId?: string;
}

const PortalFileUpload = ({
  id,
  label,
  hideLabel,
  context,
  multiple,
  onChange,
  onUploadStart,
  onUploadEnd,
  informatieobjecttype,
  getBeschrijving,
  initialValue = [],
  taakId,
}: PortalFileUploadProps) => {
  const [errorMessageId, setErrorMessageId] = useState<string | null>(null);
  const [fileList, setFileList] = useState<UploadedFile[]>(initialValue);

  const intl = useIntl();

  const handleError = (
    tempId?: string,
    messageId = "formio.fileUpload.error",
  ) => {
    setErrorMessageId(messageId);

    if (tempId) {
      setFileList((prev) => prev.filter((item) => item.id !== tempId));
    }
  };

  const interpolateInformatieobjectUrl = (url: string) =>
    url.replace(
      /({{\s*(.*?)\s*}})/g,
      (input, _capturedTemplate, capturedPath) => {
        const value = get(context, capturedPath);

        return value ?? input;
      },
    );

  const uploadFile = (file: File) => {
    const restUri = sessionStorage.getItem("REST_URI");
    const uploadLink = `${restUri}/taak/${taakId}/document/content`;
    setErrorMessageId(null);
    const formData = new FormData();
    formData.append("file", file);

    if (informatieobjecttype) {
      formData.append(
        "informatieobjecttype",
        interpolateInformatieobjectUrl(informatieobjecttype),
      );
    }

    const beschrijving = getBeschrijving?.();
    if (beschrijving) {
      formData.append("beschrijving", beschrijving);
    }

    const tempId = crypto.randomUUID();
    const tempItem: UploadedFile = {
      id: tempId,
      name: file.name,
      size: file.size,
      isUploaded: false,
    };

    setFileList((prev) => (multiple ? [tempItem, ...prev] : [tempItem]));

    const upload = fetch(uploadLink, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${FormIoUploader.getOidcToken()}`,
      },
      body: formData,
    })
      .then(async (response) => {
        if (!response.ok) {
          const errorResponse = await response.json().catch(() => null);
          handleError(
            tempId,
            errorResponse?.error === "BESCHRIJVING_TOO_LONG"
              ? "formio.fileUpload.descriptionTooLong"
              : undefined,
          );
          return;
        }

        const jsonResponse = await response.json();
        const uploadedFile: UploadedFile = {
          url: jsonResponse?.url,
          name: file.name,
          size: file.size,
        };

        setErrorMessageId(null);

        setFileList((prev) =>
          prev.map((item) =>
            item.id === tempId
              ? { ...item, ...uploadedFile, isUploaded: true }
              : item,
          ),
        );
      })
      .catch(() => {
        handleError(tempId);
      });

    // Inform Form.io that a file upload is in progress.
    onUploadStart(upload);

    void upload.finally(() => {
      // Letting Form.io know that the file upload has completed.
      onUploadEnd(upload);
    });
  };

  useEffect(() => {
    onChange(fileList);
  }, [fileList, onChange]);

  return (
    <FormField invalid={errorMessageId !== null}>
      {!hideLabel && label && <FormLabel htmlFor={id}>{label}</FormLabel>}
      {errorMessageId && (
        <Alert
          close={() => {
            setErrorMessageId(null);
          }}
          text={
            <Paragraph>
              <FormattedMessage id={errorMessageId} />
            </Paragraph>
          }
          title=""
          variant="error"
        />
      )}
      {(multiple || fileList.length === 0) && (
        <FileUpload
          id={id}
          buttonLabel={intl.formatMessage({
            id: "formio.fileUpload.buttonLabel",
          })}
          text={intl.formatMessage({ id: "formio.fileUpload.text" })}
          onFilesSelected={(files) =>
            Array.from(files || []).forEach(uploadFile)
          }
          multiple={multiple}
        />
      )}
      {fileList.map((file) => (
        <File
          key={file.id}
          name={file.name}
          size={String(file.size)}
          onClick={() =>
            setFileList((prev) => prev.filter((item) => item.id !== file.id))
          }
          removable
          loading={!file.isUploaded}
        />
      ))}
    </FormField>
  );
};
const FieldComponent = Components.components.field;

class FormIoUploader extends FieldComponent {
  private reactRoot: Root | null;
  private pendingUploads = new Set<Promise<void>>();
  static globalOidcToken = "";

  constructor(component: any, options: any, data: any) {
    super(component, options, data);
    this.reactRoot = null;

    this.noField = true;

    if (this.component.multipleFiles === undefined) {
      this.component.multipleFiles = true;
    }

    this.component.multiple = true; // Must be true to force formio to accept arrays as valid input value for this field type
  }

  static get builderInfo() {
    return {
      title: "Portal File Upload",
      group: "basic",
      icon: "upload",
      schema: FormIoUploader.schema(),
    };
  }

  static schema() {
    return FieldComponent.schema({
      type: "portalFileUpload",
    });
  }

  static editForm = (...extend: any[]) =>
    FieldComponent.editForm([
      {
        key: "data",
        components: [
          {
            type: "textfield",
            key: "beschrijvingPath",
            label: "Beschrijving path",
            input: true,
            weight: 200,
            placeholder: "e.g. bestanden.beschrijving",
            tooltip:
              "Dotted path into the form data whose value is stored as the description (beschrijving) of each uploaded document. Inside a data grid the path is first resolved against the uploader's own row. Use a hidden field with a default value for a static description.",
          },
        ],
      },
      ...extend,
    ]);

  static register: () => void = () => {
    Components.addComponent("portalFileUpload", FormIoUploader);
  };

  get emptyValue(): UploadedFile[] {
    return [];
  }

  static setOidcToken = (oidcToken: string) => {
    FormIoUploader.globalOidcToken = oidcToken;
  };

  static getOidcToken = () => FormIoUploader.globalOidcToken;

  onChangeHandler = (files: UploadedFile[]) => {
    if (this.shouldConditionallyClear()) return;

    this.updateValue(files, undefined);
  };

  onUploadStartHandler = (upload: Promise<void>) => {
    this.pendingUploads.add(upload);
    this.emit("fileUploadingStart", upload);
  };

  onUploadEndHandler = (upload: Promise<void>) => {
    if (!this.pendingUploads.delete(upload)) return;

    this.emit("fileUploadingEnd", upload);
  };

  getBeschrijving = (): string | undefined => {
    const path = this.component.beschrijvingPath;
    if (!path) return undefined;

    const value = get(this.data, path) ?? get(this.rootValue, path);
    return typeof value === "string" && value.trim() ? value : undefined;
  };

  deleteValue() {
    super.deleteValue();
    this.pendingUploads?.forEach(this.onUploadEndHandler);
  }

  render() {
    return super.render(`<div ref="react"></div>`);
  }

  attach(element: Element) {
    const baseAttachResult = super.attach(element);

    this.loadRefs(element, { react: "single" });

    const mountEl = (this.refs as any)?.react as Element | undefined;
    if (!mountEl) return baseAttachResult;

    this.reactRoot?.unmount();
    this.reactRoot = createRoot(mountEl);

    this.reactRoot.render(
      <LocalizationProvider>
        <PortalFileUpload
          id={`${this.component.id}-${this.component.key}`}
          label={this.component.label}
          hideLabel={this.component.hideLabel}
          context={this.data}
          multiple={this.component.multipleFiles}
          onChange={this.onChangeHandler}
          onUploadStart={this.onUploadStartHandler}
          onUploadEnd={this.onUploadEndHandler}
          informatieobjecttype={this.component.informatieobjecttype || ""}
          getBeschrijving={this.getBeschrijving}
          initialValue={this.dataValue}
          taakId={this.options.taakId}
        />
      </LocalizationProvider>,
    );

    return baseAttachResult;
  }

  detach() {
    this.reactRoot?.unmount();
    this.reactRoot = null;
    return super.detach();
  }
}

export default FormIoUploader;
