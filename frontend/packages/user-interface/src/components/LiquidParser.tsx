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
import { useEffect, useState, type ReactNode } from "react";
import { renderTemplate } from "../utils/liquid/renderTemplate";
import { parseComponents } from "../utils/liquid/parseComponents";

export interface LiquidParserProps {
  loading?: boolean;
  template: string;
  data: Record<string, unknown>;
  onError?: (error: Error) => void;
}

const LiquidParser = ({ template, data, onError }: LiquidParserProps) => {
  const [result, setResult] = useState<{
    template: string;
    data: Record<string, unknown>;
    content: ReactNode;
  }>();

  useEffect(() => {
    let active = true;
    renderTemplate(template, data)
      .then((html) => {
        if (active)
          setResult({ template, data, content: parseComponents(html) });
      })
      .catch((error: unknown) => {
        if (!active) return;
        setResult(undefined);
        onError?.(error instanceof Error ? error : new Error(String(error)));
      });
    return () => {
      active = false;
    };
  }, [template, data, onError]);

  return result?.template === template && result.data === data
    ? result.content
    : null;
};

export default LiquidParser;
