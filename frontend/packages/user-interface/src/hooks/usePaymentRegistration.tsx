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
  DirectPaymentStatusCategory,
  GetDirectPaymentStatusDocument,
} from "@nl-portal/nl-portal-api";
import { useLazyQuery } from "@apollo/client/react";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";

export enum PaymentStatus {
  IN_PROGRESS,
  SUCCESS,
  FAILURE,
}

const usePaymentRegistration = () => {
  const [getPaymentStatus] = useLazyQuery(GetDirectPaymentStatusDocument);
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatus>();
  const [orderId, setOrderId] = useState<string | undefined>();
  const [searchParams, setSearchParams] = useSearchParams();
  const type = searchParams.get("type");

  useEffect(() => {
    if (type !== "ogone") return;
    if (paymentStatus !== undefined) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPaymentStatus(PaymentStatus.IN_PROGRESS);

    const hostedCheckoutId = searchParams.get("hostedCheckoutId");
    const category = searchParams.get("category");

    if (!hostedCheckoutId || !category) {
      setPaymentStatus(PaymentStatus.FAILURE);
      return;
    }

    getPaymentStatus({
      variables: { identifier: category, hostedCheckoutId: hostedCheckoutId },
    })
      .then(({ data: paymentStatusData }) => {
        if (
          paymentStatusData?.getDirectPaymentStatus.status ===
          DirectPaymentStatusCategory.Successful
        ) {
          setPaymentStatus(PaymentStatus.SUCCESS);
          setOrderId(searchParams.get("orderID")?.toString());
        } else {
          setPaymentStatus(PaymentStatus.FAILURE);
        }
      })
      .catch(() => setPaymentStatus(PaymentStatus.FAILURE))
      .finally(() => {
        const newSearchParams = new URLSearchParams();
        setSearchParams(newSearchParams);
      });
  }, [type, paymentStatus]);

  return { paymentStatus, orderId };
};

export default usePaymentRegistration;
