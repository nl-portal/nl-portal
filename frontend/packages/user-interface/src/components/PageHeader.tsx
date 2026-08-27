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
import styles from "./PageHeader.module.scss";
import Skeleton from "./Skeleton";
import Heading from "./Heading";
import { useContext } from "react";
import NotificationContext from "../contexts/NotificationContext";
import { pageHeaderHeight, pageHeaderWidth } from "../constants/skeleton";
import Notification from "./Notification";

interface Props {
  loading?: boolean;
  title?: string | React.ReactNode;
  subTitle?: string | React.ReactNode;
  children?: React.ReactNode;
}

const PageHeader = ({ loading, title, subTitle, children }: Props) => {
  const { state: notifications } = useContext(NotificationContext);

  if (loading)
    return <Skeleton height={pageHeaderHeight} width={pageHeaderWidth} />;
  if (!title && !children) return null;

  return (
    <header className={styles["page-header"]}>
      {title && <Heading size="h2">{title}</Heading>}
      {subTitle && <Heading as="h3">{subTitle}</Heading>}
      {Object.entries(notifications).map(([id, props]) => (
        <Notification key={id} {...props} />
      ))}
      {children}
    </header>
  );
};

export default PageHeader;
