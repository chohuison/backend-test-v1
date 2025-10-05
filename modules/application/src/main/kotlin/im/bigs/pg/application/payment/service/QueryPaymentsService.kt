package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.*
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.payment.PaymentSummary
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

/**
 * 결제 이력 조회 유스케이스 구현체.
 * - 커서 토큰은 createdAt/id를 안전하게 인코딩해 전달/복원합니다.
 * - 통계는 조회 조건과 동일한 집합을 대상으로 계산됩니다.
 */
@Service
class QueryPaymentsService(
    private val paymentOutPort: PaymentOutPort
) : QueryPaymentsUseCase {
    /**
     * 필터를 기반으로 결제 내역을 조회합니다.
     *
     * @param filter 파트너/상태/기간/커서/페이지 크기
     * @return 조회 결과(목록/통계/커서)
     */
    override fun query(filter: QueryFilter): QueryResult {
        // 커서 디코딩
        val (cursorCreatedAt, cursorId) = decodeCursor(filter.cursor)

        // String status를 PaymentStatus enum으로 변환
        val paymentStatus = filter.status?.let { statusStr ->
            try {
                PaymentStatus.valueOf(statusStr.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        // 데이터 조회 (hasNext 판단을 위해 +1개 조회)
        val query = PaymentQuery(
            partnerId = filter.partnerId,
            status = paymentStatus,
            from = filter.from,
            to = filter.to,
            cursorCreatedAt = cursorCreatedAt,
            cursorId = cursorId,
            limit = filter.limit + 1
        )
        val page = paymentOutPort.findBy(query)

        // 다음 페이지 존재 여부 및 실제 반환할 데이터
        val hasNext = page.items.size > filter.limit
        val items = if (hasNext) page.items.take(filter.limit) else page.items

        // 다음 커서 생성
        val nextCursor = if (hasNext && items.isNotEmpty()) {
            val lastItem = items.last()
            encodeCursor(lastItem.createdAt?.atZone(ZoneOffset.UTC)?.toInstant(), lastItem.id)
        } else null

        // 통계 조회 (필터와 동일한 조건, 커서 무관)
        val summaryFilter = PaymentSummaryFilter(
            partnerId = filter.partnerId,
            status = paymentStatus,
            from = filter.from,
            to = filter.to
        )
        val summaryProjection = paymentOutPort.summary(summaryFilter)

        val summary = PaymentSummary(
            count = summaryProjection.count,
            totalAmount = summaryProjection.totalAmount,
            totalNetAmount = summaryProjection.totalNetAmount
        )

        return QueryResult(
            items = items,
            summary = summary,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    /** 다음 페이지 이동을 위한 커서 인코딩. */
    private fun encodeCursor(createdAt: Instant?, id: Long?): String? {
        if (createdAt == null || id == null) return null
        val raw = "${createdAt.toEpochMilli()}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /** 요청으로 전달된 커서 복원. 유효하지 않으면 null 커서로 간주합니다. */
    private fun decodeCursor(cursor: String?): Pair<LocalDateTime?, Long?> {
        if (cursor.isNullOrBlank()) return null to null
        return try {
            val raw = String(Base64.getUrlDecoder().decode(cursor))
            val parts = raw.split(":")
            val ts = parts[0].toLong()
            val id = parts[1].toLong()
            LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC) to id
        } catch (e: Exception) {
            null to null
        }
    }
}