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
import { Alert, AlertProps } from "@gemeente-denhaag/alert";
import styles from "./Notification.module.scss";
import { useEffect, useState } from "react";
import classNames from "classnames";

export interface NotificationProps extends AlertProps {
  closable?: boolean;
}

const Notification = ({
  closable = true,
  title,
  text,
  variant,
  className,
  ...props
}: NotificationProps) => {
  const [showNotification, setShowNotification] = useState(true);
  const alertClassName = classNames(
    styles["nl-portal-notification"],
    className,
  );

  useEffect(() => {
    scrollTo(0, 0);
  }, []);

  const close = () => setShowNotification(false);

  if (!showNotification) return;

  return (
    <Alert
      {...props}
      variant={variant}
      className={alertClassName}
      title={title}
      text={text}
      close={closable ? close : undefined}
    />
  );
};

export default Notification;
