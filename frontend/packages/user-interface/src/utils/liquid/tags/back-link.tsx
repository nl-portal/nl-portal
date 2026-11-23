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
import BackLink from "../../../components/BackLink";

const APP_PATH = /^\/(?![/\\])/;

const LiquidBackLink = ({ href, title }: { href?: string; title?: string }) => {
  const intl = useIntl();
  return (
    <BackLink href={href}>
      {title && intl.formatMessage({ id: title, defaultMessage: title })}
    </BackLink>
  );
};

/**
 * The app's back link, without end tag, for the top of a detail page. With `href` it always links to that app path,
 * the right choice for a page with one parent; without it goes back to the previous page in the app and shows nothing
 * when there is none. `href` must be an app path (`/…`), without `//`, backslashes, surrounding whitespace or control
 * characters. `title` is a message id, shown as is when no message exists; without it the standard "Terug".
 *
 * @example
 * {% back-link href: "/mijn-producten" %}
 */
export const backLinkTag: LiquidTagDefinition = {
  component: LiquidBackLink,
  props: { href: "href", title: "title" },
  validate: (props) => {
    if (
      props.href !== undefined &&
      (typeof props.href !== "string" ||
        !isAllowedHref(props.href) ||
        !APP_PATH.test(props.href))
    ) {
      throw new Error("Back link href must be an app path.");
    }
  },
};
