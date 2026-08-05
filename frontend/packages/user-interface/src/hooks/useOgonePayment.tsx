import { DoDirectPaymentDocument } from "@nl-portal/nl-portal-api";
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
  amount: number;
  identifier: string;
  orderId: string;
  reference: string;
  title?: string;
  returnUrl?: string;
}

const useOgonePayment = () => {
  const [loading, setLoading] = useState(false);
  const [mutateFunction] = useMutation(DoDirectPaymentDocument);
  const { currentLocale } = useContext(LocaleContext);
  const returnUrl = new URL(window.location.href);
  returnUrl.searchParams.set("type", "ogone");

  const startPayment = (paymentRequestPayload: PaymentRequestPayload) => {
    setLoading(true);
    returnUrl.searchParams.set("category", paymentRequestPayload.identifier);
    mutateFunction({
      variables: {
        amount: paymentRequestPayload.amount,
        identifier: paymentRequestPayload.identifier,
        orderId: paymentRequestPayload.orderId,
        reference: paymentRequestPayload.reference,
        langId: currentLocale.replace("-", "_"),
        returnUrl: paymentRequestPayload.returnUrl || returnUrl.href,
      },
      onCompleted: (data) => {
        window.location.href = data.doDirectPayment.redirectUrl;
      },
      onError: () => {
        setLoading(false);
      },
    });
  };

  return {
    loading,
    startPayment,
  };
};

export default useOgonePayment;
