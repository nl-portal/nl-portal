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
import type { LiquidTagDefinition } from "../tags";
import { isAllowedHref } from "../text";
import { Link } from "@gemeente-denhaag/link";
import PortalLink from "../../../components/PortalLink";

const TARGETS = ["_self", "_blank"] as const;

const LiquidLink = ({
  href,
  title,
  target,
}: {
  href: string;
  title?: string;
  target?: (typeof TARGETS)[number];
}) => {
  const intl = useIntl();
  const text = title
    ? intl.formatMessage({ id: title, defaultMessage: title })
    : href;
  return (
    <Link
      href={href}
      target={target}
      rel={target === "_blank" ? "noopener noreferrer" : undefined}
      Link={PortalLink}
    >
      {text}
    </Link>
  );
};

/**
 * A link without end tag. `title` is a message id, shown as is when no message exists.
 * `href` is relative or uses https, http, tel or mailto. `target` is `_self` (default) or `_blank`;
 * a relative link navigates inside the app.
 *
 * @example
 * {% link href: "/zaken" title: "Mijn zaken" target: "_blank" %}
 */
export const linkTag: LiquidTagDefinition = {
  component: LiquidLink,
  props: { href: "href", title: "title", target: "target" },
  validate: (props) => {
    if (
      typeof props.href !== "string" ||
      !props.href ||
      !isAllowedHref(props.href)
    ) {
      throw new Error("Link href must be relative or use an allowed protocol.");
    }
    if (
      props.target !== undefined &&
      !TARGETS.includes(props.target as (typeof TARGETS)[number])
    ) {
      throw new Error("Link target must be _self or _blank.");
    }
  },
};
