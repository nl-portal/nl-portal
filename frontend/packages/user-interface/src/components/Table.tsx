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
import {
  Table as TableComp,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TableBody,
} from "@gemeente-denhaag/table";
import React from "react";
import PortalLink from "./PortalLink";
import classNames from "classnames";

export interface CellObject {
  className?: string;
  children?: React.ReactNode;
  href?: string;
  head?: boolean;
}

export type CellSingle = React.ReactNode;

export type Cell = CellSingle | CellObject;

interface Props {
  headers?: Cell[];
  rows?: Cell[][];
}

const Table = ({ headers, rows }: Props) => {
  const isObject = (cell: Cell): cell is CellObject => {
    return (
      typeof cell === "object" &&
      cell !== null &&
      ("className" in cell ||
        "children" in cell ||
        "href" in cell ||
        "head" in cell)
    );
  };

  const renderCell = (keyString: string, cell: Cell, head?: boolean) => {
    let classes = classNames("denhaag-table__cell--align-top");

    if (isObject(cell)) {
      classes = classNames(classes, cell.className);

      if (cell.head)
        return (
          <TableHeader
            key={keyString}
            className={classes}
            href={cell.href}
            Link={PortalLink}
          >
            {cell.children}
          </TableHeader>
        );

      return (
        <TableCell
          key={keyString}
          className={classes}
          href={cell.href}
          Link={PortalLink}
        >
          {cell.children}
        </TableCell>
      );
    }

    if (head)
      return (
        <TableHeader key={keyString} className={classes}>
          {cell}
        </TableHeader>
      );

    return (
      <TableCell key={keyString} className={classes}>
        {cell}
      </TableCell>
    );
  };

  return (
    <TableComp>
      {headers && (
        <TableHead>
          <TableRow>
            {headers.map((cell, index) =>
              renderCell(`header-${index}`, cell, true),
            )}
          </TableRow>
        </TableHead>
      )}
      {rows && (
        <TableBody>
          {rows.map((row, rowIndex) => (
            <TableRow key={rowIndex}>
              {row.map((cell, cellIndex) =>
                renderCell(`row-${rowIndex}-${cellIndex}`, cell),
              )}
            </TableRow>
          ))}
        </TableBody>
      )}
    </TableComp>
  );
};

export default Table;
