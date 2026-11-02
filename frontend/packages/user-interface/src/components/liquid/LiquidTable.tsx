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
import { useContext, type ComponentProps } from "react";
import TableList from "../TableList";
import PageBindingContext from "../../contexts/PageBindingContext";

type Props = ComponentProps<typeof TableList> & {
  binding?: string;
  pageParam?: string;
  pageSize?: number | string;
  total?: number | string;
};

/**
 * TableList for templates. With a PageBindingContext, [binding], [pageParam], [pageSize] and [total]
 * it shows pagination that refetches the binding; otherwise it renders without interaction.
 */
const LiquidTable = ({
  binding,
  pageParam,
  pageSize,
  total,
  ...props
}: Props) => {
  const context = useContext(PageBindingContext);
  const size = Number(pageSize);
  const count = Number(total);

  if (
    !context ||
    !binding ||
    !pageParam ||
    !(size > 0) ||
    !Number.isFinite(count)
  ) {
    return <TableList {...props} />;
  }

  const index = Number(context.meta(binding)?.params[pageParam] ?? 0) || 0;
  return (
    <TableList
      {...props}
      index={index}
      indexLimit={Math.ceil(count / size) - 1}
      onChange={(page) =>
        context.refetch(binding, { [pageParam]: String(page) })
      }
    />
  );
};

export default LiquidTable;
