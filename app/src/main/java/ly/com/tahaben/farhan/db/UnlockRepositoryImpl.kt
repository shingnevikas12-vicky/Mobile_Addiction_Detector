package ly.com.tahaben.farhan.db

import ly.com.tahaben.usage_overview_domain.repository.UnlockRepository
import java.time.LocalDate

class UnlockRepositoryImpl(
    private val unlockDao: UnlockDao
) : UnlockRepository {

    override suspend fun getUnlocksForDay(day: LocalDate): Int {
        return unlockDao.getForDay(day)?.unlockCount ?: 0
    }

    override suspend fun incrementUnlock(day: LocalDate) {
        val current = unlockDao.getForDay(day)?.unlockCount ?: 0
        unlockDao.upsert(UnlockEntity(day = day, unlockCount = current + 1))
    }
}
