package ly.com.tahaben.farhan.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Daily unlock (pickup) counter. One row per day; the accessibility service
 * increments the row for today on each confirmed unlock.
 */
@Entity
data class UnlockEntity(
    @PrimaryKey
    val day: LocalDate,
    @ColumnInfo(name = "unlock_count")
    val unlockCount: Int
)
