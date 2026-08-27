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
import { OidcContext } from "@nl-portal/nl-portal-authentication";
import { useContext, useState } from "react";

const useDownload = (href: string, filename?: string) => {
  const { oidcToken } = useContext(OidcContext);
  const [loading, setLoading] = useState(false);

  const onClick = async (event: React.MouseEvent<HTMLElement>) => {
    event.preventDefault();
    setLoading(true);

    const response = await fetch(href, {
      headers: { Authorization: `Bearer ${oidcToken}` },
    });

    setLoading(false);

    if (!response.ok) return console.error("Failed to download file");

    const responseHeader = response.headers.get("Content-Disposition");
    const headerFilename = responseHeader
      ?.split(";")
      ?.find((n) => n.includes("filename="))
      ?.replace("filename=", "")
      ?.replace(/"/g, "")
      .trim();
    const blob = await response.blob();
    const blobHref = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    const download = filename || headerFilename;

    if (!download) return console.error("Failed to download file, no filename");

    anchor.href = blobHref;
    anchor.download = download;
    document.body.appendChild(anchor);
    anchor.dispatchEvent(
      new MouseEvent("click", {
        bubbles: true,
        cancelable: true,
        view: window,
      }),
    );
    anchor.remove();
    window.URL.revokeObjectURL(anchor.href);
  };

  return { onClick, loading };
};

export default useDownload;
