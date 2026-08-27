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
import { useContext } from "react";
import prettyBytes from "pretty-bytes";
import { LocaleContext } from "@nl-portal/nl-portal-localization";
import { Document as PortalDocument } from "@nl-portal/nl-portal-api";
import { File } from "@gemeente-denhaag/file";
import useDownload from "../hooks/useDownload";

interface Props {
  document: PortalDocument;
  downloadLink: string;
}

const Document = ({ document: doc, downloadLink: downloadLink }: Props) => {
  const { onClick } = useDownload(downloadLink, doc.bestandsnaam || undefined);
  const { hrefLang } = useContext(LocaleContext);

  return (
    <File
      name={
        doc.bestandsnaam && doc.bestandsnaam?.lastIndexOf(".") >= 0
          ? doc.bestandsnaam?.substring(0, doc.bestandsnaam.lastIndexOf("."))
          : doc.bestandsnaam || ""
      }
      href={doc.bestandsnaam || ""}
      size={prettyBytes(doc.bestandsomvang || 0, { locale: hrefLang })}
      onClick={onClick}
    />
  );
};

export default Document;
