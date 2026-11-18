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
import { DoTaakPaymentDocument } from "@nl-portal/nl-portal-api";
import { useMutation } from "@apollo/client/react";
import { useContext, useState } from "react";
import { LocaleContext } from "@nl-portal/nl-portal-localization";

// -- Variables for the Ogone direct payment request:
// amount, required, The amount to be paid (float).
// identifier, required, The identifier of the merchant. Can be found in product eigenschappen or in the taak pspid.
// orderId, required, The unique order id. This id will be used to identify the payment in the Ogone system.
// reference, required, The reference of the payment. This reference will be shown on the bank statement of the payer.
// langId: The language id of the payment page. This id should be in the format of nl_NL.
// returnUrl: The url to redirect to after a successful payment.

interface PaymentRequestPayload {
  identifier: string;
  taakId: string;
}

const useTaakPayment = () => {
  const [loading, setLoading] = useState(false);
  const [mutateFunction] = useMutation(DoTaakPaymentDocument);
  const { currentLocale } = useContext(LocaleContext);
  const returnUrl = new URL(window.location.href);
  returnUrl.searchParams.set("type", "ogone");

  const startPayment = (paymentRequestPayload: PaymentRequestPayload) => {
    setLoading(true);
    returnUrl.searchParams.set("category", paymentRequestPayload.identifier);
    mutateFunction({
      variables: {
        identifier: paymentRequestPayload.identifier,
        langId: currentLocale.replace("-", "_"),
        taakId: paymentRequestPayload.taakId,
      },
      onCompleted: (data) => {
        window.location.href = data.doTaakPayment.redirectUrl;
      },
      onError: () => {
        setLoading(false);
      },
    });
  };

  return {
    startPayment,
    loading,
  };
};

export default useTaakPayment;
