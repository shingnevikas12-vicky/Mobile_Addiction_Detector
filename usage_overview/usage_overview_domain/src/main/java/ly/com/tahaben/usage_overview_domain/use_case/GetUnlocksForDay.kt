package ly.com.tahaben.usage_overview_domain.use_case

import ly.com.tahaben.usage_overview_domain.repository.UnlockRepository
import java.time.LocalDate

class GetUnlocksForDay(private val unlockRepository: UnlockRepository) {
    suspend operator fun invoke(day: LocalDate): Int {
        return unlockRepository.getUnlocksForDay(day)
    }
}
