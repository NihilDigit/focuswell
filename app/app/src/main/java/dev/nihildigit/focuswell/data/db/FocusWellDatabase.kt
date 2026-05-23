package dev.nihildigit.focuswell.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    AppStateEntity::class,
    TagEntity::class,
    DailyTrackerEntity::class,
    FocusRecordEntity::class,
    LeisureRecordEntity::class,
    LedgerEntryEntity::class,
  ],
  version = 2,
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
            .addMigrations(MIGRATION_1_2)
            .build()
            .also { instance = it }
      }

    private val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE daily_trackers ADD COLUMN rewardMinutes REAL NOT NULL DEFAULT 10.0")
        }
      }
  }
}
