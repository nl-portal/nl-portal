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
import { useContext, useEffect } from "react";
import type { ComponentProps } from "react";
import { Form } from "@formio/react";
import { OidcContext } from "@nl-portal/nl-portal-authentication";
import FormIoUploader from "./FormIoUploader";
import "./FormIoTemplates.scss";

export default function PortalForm(props: ComponentProps<typeof Form>) {
  const { oidcToken } = useContext(OidcContext);

  useEffect(() => {
    FormIoUploader.setOidcToken(oidcToken ?? "");
  }, [oidcToken]);

  return <Form {...props} />;
}
