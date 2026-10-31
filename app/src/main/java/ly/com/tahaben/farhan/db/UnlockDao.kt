package ly.com.tahaben.farhan.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate

@Dao
interface UnlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unlockEntity: UnlockEntity)

    @Query("SELECT * FROM UnlockEntity WHERE day = :day LIMIT 1")
    suspend fun getForDay(day: LocalDate): UnlockEntity?

    @Query("SELECT SUM(unlock_count) FROM UnlockEntity")
    suspend fun getTotalUnlocks(): Long?
}
