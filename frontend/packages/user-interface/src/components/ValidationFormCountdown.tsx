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
import { useEffect } from "react";
import { FormattedMessage, FormattedTime } from "react-intl";
import useCountdown from "../hooks/useCountdown";

interface ValidationFormCountdownProps {
  onComplete: () => void;
}

const ValidationFormCountdown = ({
  onComplete,
}: ValidationFormCountdownProps) => {
  const { timeLeft, start } = useCountdown((10 * 60 - 1) * 1000);

  useEffect(() => {
    start();
  }, []);

  useEffect(() => {
    if (timeLeft > 0) return;
    onComplete();
  }, [timeLeft]);

  if (timeLeft === 0) return null;

  return (
    <FormattedMessage
      id={`validationForm.labelDescription`}
      values={{
        time: (
          <FormattedTime value={timeLeft} minute="2-digit" second="2-digit" />
        ),
      }}
    />
  );
};

export default ValidationFormCountdown;
