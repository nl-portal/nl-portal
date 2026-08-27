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
import classNames from "classnames";
import { AnchorHTMLAttributes } from "react";
import { Link } from "react-router";
import { Link as DHLink } from "@gemeente-denhaag/link";

// A generic wrapper around the React Router Link, which will possibly be used a lot in implementations to pass to components from design systems.
// For example in breadcrumbs, headers and footers.
//
// TODO: the external link determination a little bit hacky. Ideally, the React Router Link would automatically know if a href is external.
// This logic should be improved or extended in the future.
const PortalLink = ({
  href,
  className,
  ...props
}: AnchorHTMLAttributes<HTMLAnchorElement>) => {
  const isExternalLink =
    href && (href.startsWith("https://") || href.startsWith("tel:"));
  const linkClassNames = classNames("nl-link", className);

  if (href && !isExternalLink) {
    return (
      <Link to={href} className={linkClassNames} {...props}>
        {props.children}
      </Link>
    );
  }

  return (
    <DHLink href={href} className={className} {...props}>
      {props.children}
    </DHLink>
  );
};

export default PortalLink;
