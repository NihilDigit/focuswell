package dev.nihildigit.focuswell.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.defaultTags
import dev.nihildigit.focuswell.domain.defaultTrackers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomFocusWellStoreTest {
  private lateinit var database: FocusWellDatabase
  private lateinit var store: RoomFocusWellStore

  @Before
  fun setUp() {
    database =
      Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        FocusWellDatabase::class.java,
      )
        .allowMainThreadQueries()
        .build()
    store = RoomFocusWellStore(database.focusWellDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun persistsAndLoadsStateThroughRoom() = runTest {
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        tags = defaultTags,
        trackers = defaultTrackers,
        ledger =
          listOf(
            LedgerEntry(
              id = "daily-grant-2026-05-20",
              title = "Daily grant",
              deltaMinutes = 60.0,
              createdAt = Instant.parse("2026-05-20T04:00:00Z"),
            )
          ),
      )

    store.persistChange(previous = null, next = state)
    val loaded = store.loadState()

    assertNotNull(loaded)
    assertEquals("2026-05-20", loaded?.dailyDate)
    assertEquals(60.0, loaded?.reserveMinutes ?: 0.0, 0.0001)
    assertEquals(defaultTags.map { it.id }, loaded?.tags?.map { it.id })
    assertEquals(defaultTrackers.map { it.id }, loaded?.trackers?.map { it.id })
    assertEquals(listOf("daily-grant-2026-05-20"), loaded?.ledger?.map { it.id })
  }

  @Test
  fun persistsIncrementalDiffsAndDeletesRemovedRows() = runTest {
    val original =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        tags = defaultTags,
        trackers = defaultTrackers,
        ledger =
          listOf(
            LedgerEntry(
              id = "entry-1",
              title = "One",
              deltaMinutes = 1.0,
              createdAt = Instant.parse("2026-05-20T04:00:00Z"),
            )
          ),
      )
    val next = original.copy(ledger = emptyList())

    store.persistChange(previous = null, next = original)
    store.persistChange(previous = original, next = next)

    assertEquals(emptyList<String>(), store.loadState()?.ledger?.map { it.id })
  }
}
