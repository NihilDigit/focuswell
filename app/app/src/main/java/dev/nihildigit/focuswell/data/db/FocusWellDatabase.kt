package dev.nihildigit.focuswell.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    AppStateEntity::class,
    TagEntity::class,
    DailyTrackerEntity::class,
    FocusRecordEntity::class,
    LeisureRecordEntity::class,
    LedgerEntryEntity::class,
  ],
  version = 1,
  exportSchema = true,
)
internal abstract class FocusWellDatabase : RoomDatabase() {
  abstract fun focusWellDao(): FocusWellDao

  companion object {
    @Volatile private var instance: FocusWellDatabase? = null

    fun get(context: Context): FocusWellDatabase =
      instance ?: synchronized(this) {
        instance
          ?: Room.databaseBuilder(
            context.applicationContext,
            FocusWellDatabase::class.java,
            "focuswell.db",
          )
            .build()
            .also { instance = it }
      }
  }
}
