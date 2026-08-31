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
import { Form } from "./Form";
import { FormField } from "@gemeente-denhaag/form-field";
import useInput from "../hooks/useInput";
import { FormLabel } from "@gemeente-denhaag/form-label";
import { FormFieldDescription } from "@gemeente-denhaag/form-field-description";
import { FormattedMessage } from "react-intl";
import { TextInput } from "@gemeente-denhaag/text-input";
import { FormFieldErrorMessage } from "@gemeente-denhaag/form-field-error-message";
import { useEffect, useState } from "react";
import { Alert, AlertProps } from "@gemeente-denhaag/alert";
import ValidationFormCountdown from "./ValidationFormCountdown";

interface ValidationFormProps {
  value: string;
  loading?: boolean;
  onSubmit?: (verificationCode: string) => void;
  onRefresh?: () => Promise<void>;
  error?: AlertProps | boolean;
  validationError?: boolean;
}

const ValidationForm = ({
  loading,
  onSubmit,
  onRefresh,
  error = false,
  validationError = false,
}: ValidationFormProps) => {
  const [showTimer, setShowTimer] = useState(true);
  const [timerComplete, setTimerComplete] = useState(false);

  useEffect(() => {
    if (!validationError) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setShowTimer(false);
    setTimerComplete(false);
  }, [validationError]);

  const {
    value,
    handleInputChange,
    handleInputBlur,
    hasError,
    errorTranslationId,
    resetValue,
  } = useInput("", [
    {
      validationFn: (value: string) => value !== "",
      errorTranslationId: "validationForm.error",
    },
  ]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    onSubmit?.(value);
  };

  const handleCancel = async () => {
    setShowTimer(false);
    setTimerComplete(false);
    await onRefresh?.();
    setShowTimer(true);
    resetValue();
  };

  return (
    <>
      {timerComplete && (
        <Alert
          variant="warning"
          title={<FormattedMessage id="validationForm.timeError.Title" />}
          text={<FormattedMessage id="validationForm.timeError.Text" />}
        />
      )}
      {validationError && (
        <Alert
          variant="warning"
          title={<FormattedMessage id="validationForm.validationError.Title" />}
          text={<FormattedMessage id="validationForm.validationError.Text" />}
        />
      )}
      <Form
        loading={!value || loading || timerComplete || validationError}
        onSubmit={handleSubmit}
        cancelTranslationId="validationForm.cancel"
        onCancel={handleCancel}
        error={error}
      >
        <FormField invalid={hasError}>
          <FormLabel htmlFor="validationForm">
            <FormattedMessage id={`validationForm.label`} />
          </FormLabel>
          <FormFieldDescription>
            {showTimer && (
              <ValidationFormCountdown
                onComplete={() => setTimerComplete(true)}
              />
            )}
          </FormFieldDescription>
          <TextInput
            id="validationForm"
            type="text"
            name="value"
            value={value}
            onChange={handleInputChange}
            onBlur={handleInputBlur}
            invalid={hasError}
            disabled={timerComplete || validationError}
          />
          {hasError && (
            <FormFieldErrorMessage>
              <FormattedMessage id={errorTranslationId} />
            </FormFieldErrorMessage>
          )}
        </FormField>
      </Form>
    </>
  );
};

export default ValidationForm;
