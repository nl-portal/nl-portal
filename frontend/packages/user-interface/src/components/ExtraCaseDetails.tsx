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
import { ReactNode } from "react";
import { DescriptionList } from "@gemeente-denhaag/descriptionlist";
import TableList from "./TableList";
import SectionHeader from "./SectionHeader";
import { useDateFormatter } from "@nl-portal/nl-portal-localization";

interface Props {
  data: Details[];
}

export interface ExtraZaakDetails {
  data: Details[];
  zaak: string;
}

export interface Table {
  headers: Content[];
  rows: Content[][];
}

export enum ContentTypes {
  BOLD = "bold",
  ITALIC = "italic",
  DATE = "date",
}

export enum DetailType {
  TABLE = "table",
  DESCRIPTION_LIST = "keywaardelijst",
}

export interface Details {
  key?: string;
  type: DetailType.TABLE | DetailType.DESCRIPTION_LIST;
  heading: string;
  description?: string;
  items: Content[] | Table;
  children?: Details[];
}

export interface Content {
  key: string;
  content: string;
  type?: ContentTypes.BOLD | ContentTypes.ITALIC | ContentTypes.DATE;
  description?: string;
}

//eslint-disable-next-line @typescript-eslint/no-explicit-any
export const isExtraCaseDetailsObject = (item: any) => {
  return (
    typeof item === "object" &&
    item !== null &&
    "type" in item &&
    typeof (item as Details).type === "string" &&
    "heading" in item &&
    typeof (item as Details).heading === "string" &&
    "items" in item &&
    typeof (item as Details).items === "object"
  );
};

export const ExtraCaseDetails = ({ data }: Props) => {
  const { formatDate } = useDateFormatter();

  const convertRichText = (
    value: string,
    type?: ContentTypes,
  ): string | ReactNode | ReactNode[] | null => {
    switch (type) {
      case ContentTypes.BOLD:
        return <b>{value}</b>;
      case ContentTypes.ITALIC:
        return <i>{value}</i>;
      case ContentTypes.DATE:
        return formatDate({ date: value });
      default:
        return value;
    }
  };

  const renderTableSection = (heading: string, table: Table) => (
    <section key={`section-${heading}`}>
      <TableList
        titleTranslationId={heading}
        headers={table.headers.map((head) => head.content)}
        rows={table.rows.map((row) =>
          row.map((cell) => ({
            children: convertRichText(cell.content, cell.type),
          })),
        )}
      />
    </section>
  );

  const renderDescriptionList = (
    heading: string,
    items: Content[],
    isChild: boolean,
  ) => (
    <section key={`section-${heading}`}>
      <SectionHeader title={heading} small={isChild} />
      <DescriptionList
        items={items.map((item) => ({
          title: item.key,
          detail: convertRichText(item.content, item.type),
        }))}
      />
    </section>
  );

  const renderDetails = (details: Details[], isChild = false): ReactNode[] =>
    details.map((detail) => {
      if (!isExtraCaseDetailsObject(detail)) {
        return null;
      }

      const { type, heading, items, children } = detail;
      switch (type) {
        case DetailType.TABLE:
          return (
            <>
              {renderTableSection(heading, items as Table)}
              {children && children.length > 0 && renderDetails(children, true)}
            </>
          );

        case DetailType.DESCRIPTION_LIST:
          return (
            <>
              {renderDescriptionList(heading, items as Content[], isChild)}
              {children && children.length > 0 && renderDetails(children, true)}
            </>
          );

        default:
          return null;
      }
    });

  return renderDetails(data);
};

export default ExtraCaseDetails;
