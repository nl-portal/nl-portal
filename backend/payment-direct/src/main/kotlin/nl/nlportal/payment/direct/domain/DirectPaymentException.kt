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
package nl.nlportal.payment.direct.domain

/**
 * Reasons a Direct Payment is rejected. Messages contain no amounts, references or personal data.
 */
sealed class DirectPaymentException(
    message: String,
) : RuntimeException(message) {
    class InvalidSignature : DirectPaymentException("Webhook signature is not valid")

    class PaymentNotFinal(
        val status: Int,
    ) : DirectPaymentException("Payment status $status is not final")

    class MerchantMismatch : DirectPaymentException("Payment profile does not match the merchant")

    class AmountMismatch : DirectPaymentException("Paid amount does not match the expected amount")

    class AmountNotConvertible : DirectPaymentException("Amount can not be converted to cents")
}