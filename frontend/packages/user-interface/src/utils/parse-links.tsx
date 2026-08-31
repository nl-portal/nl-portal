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
import { Link } from "@gemeente-denhaag/link";
import PortalLink from "../components/PortalLink";

const urlRegex = /(https?:\/\/[^\s]+)/g;

// Remove possible trailing . or , from the url
const cleanupUrl = (url: string) => {
  let punctuation = "";

  if (url.endsWith(".") || url.endsWith(",")) {
    punctuation = url.slice(-1);
    url = url.slice(0, -1);
  }

  return { url, punctuation };
};

export const parseLinks = (text: string) => {
  return text.split(urlRegex).map((part, index) => {
    if (urlRegex.test(part)) {
      const { url, punctuation } = cleanupUrl(part);
      return (
        <>
          <Link Link={PortalLink} href={url} key={index} target="_blank">
            {url}
          </Link>
          {punctuation}
        </>
      );
    }

    return part;
  });
};
