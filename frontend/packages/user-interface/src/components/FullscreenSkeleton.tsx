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
import Skeleton from "./Skeleton";
import style from "./FullscreenSkeleton.module.scss";
import classNames from "classnames";
import { ResponsiveContent } from "@gemeente-denhaag/responsive-content";

const Section = () => {
  return (
    <div className={style["section"]}>
      <Skeleton width={150} height={32} />
      <Skeleton width={110} height={24} style={{ marginBottom: "16px" }} />
      <Skeleton width={"100%"} height={64} />
      <Skeleton width={"100%"} height={64} />
      <Skeleton width={"100%"} height={64} />
    </div>
  );
};

const SidebarItem = () => {
  return (
    <div className={style["sidebar-item"]}>
      <Skeleton width={24} height={24} />
      <Skeleton width={180} height={24} />
    </div>
  );
};

const FullscreenSkeleton = () => {
  return (
    <div className={style["fullscreen-skeleton"]}>
      <div className={style["header"]}>
        <ResponsiveContent>
          <Skeleton width={130} height={40} />
        </ResponsiveContent>
      </div>
      <div className={style["breadcrumb"]} />
      <ResponsiveContent
        className={classNames(
          style["page"],
          "denhaag-page-content",
          "denhaag-responsive-content--sidebar",
        )}
      >
        <div className={style["sidebar"]}>
          <SidebarItem />
          <div className={style["sidebar-group"]}>
            <SidebarItem />
            <SidebarItem />
            <SidebarItem />
          </div>
          <div className={style["sidebar-group"]}>
            <SidebarItem />
            <SidebarItem />
            <SidebarItem />
          </div>
        </div>
        <div
          className={classNames(style["main"], "denhaag-page-content__main")}
        >
          <Skeleton width={370} height={48} style={{ maxWidth: "100%" }} />
          <Section />
          <Section />
          <Section />
        </div>
      </ResponsiveContent>
    </div>
  );
};

export default FullscreenSkeleton;
