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
    IdeaEntity::class,
    LedgerEntryEntity::class,
  ],
  version = 11,
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .build()
            .also { instance = it }
      }

    private val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE daily_trackers ADD COLUMN rewardMinutes REAL NOT NULL DEFAULT 10.0")
        }
      }

    private val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE app_state ADD COLUMN dailyGrantMinutes REAL NOT NULL DEFAULT 60.0")
          db.execSQL("ALTER TABLE app_state ADD COLUMN dayBoundaryHour INTEGER NOT NULL DEFAULT 4")
          db.execSQL("ALTER TABLE app_state ADD COLUMN sleepProtectionStartHour INTEGER NOT NULL DEFAULT 1")
          db.execSQL("ALTER TABLE app_state ADD COLUMN sleepProtectionMultiplier REAL NOT NULL DEFAULT 2.0")
        }
      }

    private val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("UPDATE daily_trackers SET rewardMinutes = 15.0 WHERE id IN ('aerobic', 'wake', 'vocabulary', 'codewars') AND rewardMinutes = 10.0")
          db.execSQL("UPDATE daily_trackers SET rewardMinutes = 60.0 WHERE id IN ('math-3h', '408-3h') AND rewardMinutes = 10.0")
        }
      }

    private val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ideas` (
              `id` TEXT NOT NULL,
              `text` TEXT NOT NULL,
              `quadrant` TEXT NOT NULL,
              `checklistJson` TEXT NOT NULL DEFAULT '[]',
              `createdAt` TEXT NOT NULL,
              `updatedAt` TEXT NOT NULL,
              `archivedAt` TEXT,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
        }
      }

    private val MIGRATION_5_6 =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE ideas ADD COLUMN checklistJson TEXT NOT NULL DEFAULT '[]'")
        }
      }

    private val MIGRATION_6_7 =
      object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE app_state ADD COLUMN longSessionRemindersEnabled INTEGER NOT NULL DEFAULT 1")
        }
      }

    private val MIGRATION_7_8 =
      object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE app_state ADD COLUMN lastCheckInDailyDate TEXT")
          db.execSQL("ALTER TABLE app_state ADD COLUMN dailyGrantPausedUntilDate TEXT")
        }
      }

    private val MIGRATION_8_9 =
      object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE app_state ADD COLUMN wakeTargetHour INTEGER NOT NULL DEFAULT 9")
        }
      }

    private val MIGRATION_9_10 =
      object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("UPDATE app_state SET wakeTargetHour = 5 WHERE wakeTargetHour = 9")
          db.execSQL("UPDATE app_state SET sleepProtectionStartHour = 21 WHERE sleepProtectionStartHour = 1")
        }
      }

    private val MIGRATION_10_11 =
      object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE app_state ADD COLUMN sleepProtectionEndHour INTEGER NOT NULL DEFAULT 5")
          db.execSQL("UPDATE app_state SET dayBoundaryHour = 12 WHERE dayBoundaryHour = 4")
        }
      }
  }
}
