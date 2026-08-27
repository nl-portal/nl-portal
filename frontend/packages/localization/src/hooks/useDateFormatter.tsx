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
import { useContext } from "react";
import LocaleContext from "../contexts/LocaleContext";
import {
  formatDate as format,
  FormatDateLabels,
  longDateOptions,
} from "@gemeente-denhaag/utils";
import useActionLabels from "./useActionLabels";

interface FormatDateProps {
  date: string;
  formatOptions?: Intl.DateTimeFormatOptions;
  relative?: boolean;
  namedDays?: boolean;
  labels?: FormatDateLabels;
}

const useDateFormatter = () => {
  const { currentLocale } = useContext(LocaleContext);
  const defaultLabels = useActionLabels();

  const formatDate = ({
    date,
    formatOptions = longDateOptions,
    relative,
    namedDays,
    labels = defaultLabels,
  }: FormatDateProps) => {
    return format({
      dateTime: date,
      locale: currentLocale,
      format: formatOptions,
      relative,
      namedDays,
      labels,
    })[0];
  };

  return { formatDate };
};

export default useDateFormatter;
