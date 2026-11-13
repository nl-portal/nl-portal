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
import { Alert } from "@gemeente-denhaag/alert";
import { useIntl } from "react-intl";
import type { LiquidTagDefinition } from "../tags";
import { bodyText } from "../text";

const VARIANTS = ["info", "success", "warning", "error"] as const;

type Variant = (typeof VARIANTS)[number];

const LiquidAlert = ({
  variant = "info",
  title = "",
  children = "",
}: {
  variant?: Variant;
  title?: string;
  children?: string;
}) => {
  const intl = useIntl();
  return (
    <Alert
      variant={variant}
      title={title && intl.formatMessage({ id: title, defaultMessage: title })}
      text={children}
    />
  );
};

/**
 * An alert with the body as text. `variant` is info (default), success, warning or error;
 * `title` is a message id, shown as is when no message exists.
 *
 * @example
 * {% alert variant: "warning" title: "onderhoud" %}Vanavond is de portal niet bereikbaar.{% endalert %}
 */
export const alertTag: LiquidTagDefinition = {
  component: LiquidAlert,
  props: { variant: "variant", title: "title" },
  body: { parse: (body) => ({ children: bodyText(body) }) },
  validate: (props) => {
    if (
      props.variant !== undefined &&
      !VARIANTS.includes(props.variant as Variant)
    ) {
      throw new Error("Alert variant must be info, success, warning or error.");
    }
  },
};
