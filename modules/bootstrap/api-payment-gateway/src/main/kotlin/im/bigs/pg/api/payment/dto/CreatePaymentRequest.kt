package im.bigs.pg.api.payment.dto

import jakarta.validation.constraints.Min
import java.math.BigDecimal

data class CreatePaymentRequest(
    val partnerId: Long,
    @field:Min(1)
    val amount: BigDecimal,
    val cardBin: String? = null,
    val cardLast4: String? = null,
    val productName: String? = null,
) {
    override fun toString(): String {
        return "CreatePaymentRequest(" +
                "partnerId=$partnerId, " +
                "amount=$amount, " +
                "cardBin=${if (cardBin != null) "****" else "null"}, " +
                "cardLast4=${if (cardLast4 != null) "****" else "null"}, " +
                "productName=$productName)"
    }
}