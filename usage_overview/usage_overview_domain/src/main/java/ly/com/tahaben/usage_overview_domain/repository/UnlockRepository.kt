package ly.com.tahaben.usage_overview_domain.repository

import java.time.LocalDate

/**
 * Daily unlock/pickup counter. Implemented by the app module against the
 * unified Room database; consumed by the usage overview UI and the
 * accessibility service.
 */
interface UnlockRepository {
    suspend fun getUnlocksForDay(day: LocalDate): Int
    suspend fun incrementUnlock(day: LocalDate)
}
