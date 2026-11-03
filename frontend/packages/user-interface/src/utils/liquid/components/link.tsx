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
import { useIntl } from "react-intl";
import PortalLink from "../../../components/PortalLink";
import type { LiquidComponentDefinition } from "../components";
import { isAllowedHref } from "../text";

const LiquidLink = ({ href, title }: { href: string; title?: string }) => {
  const intl = useIntl();
  const text = title
    ? intl.formatMessage({ id: title, defaultMessage: title })
    : href;
  return <PortalLink href={href}>{text}</PortalLink>;
};

/**
 * A link without end tag. `title` is a message id, shown as is when no message exists.
 * `href` is relative or uses https, http, tel or mailto.
 *
 * @example
 * {% link href: "/zaken" title: "Mijn zaken" %}
 */
export const linkDefinition: LiquidComponentDefinition = {
  component: LiquidLink,
  props: { href: "href", title: "title" },
  validate: (props) => {
    if (
      typeof props.href !== "string" ||
      !props.href ||
      !isAllowedHref(props.href)
    ) {
      throw new Error("Link href must be relative or use an allowed protocol.");
    }
  },
};
