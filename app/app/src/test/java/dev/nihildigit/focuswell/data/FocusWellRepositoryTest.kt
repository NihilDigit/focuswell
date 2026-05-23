package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.defaultTags
import dev.nihildigit.focuswell.domain.defaultTrackers
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class FocusWellRepositoryTest {
  private val previousTimeZone = TimeZone.getDefault()
  private val clock = MutableClock(Instant.parse("2026-05-20T05:00:00Z"))

  @Before
  fun setUp() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  @After
  fun tearDown() {
    TimeZone.setDefault(previousTimeZone)
  }

  @Test
  fun init_backfillsMissingDailyGrantsAcrossBusinessDays() {
    clock.instant = Instant.parse("2026-05-23T05:00:00Z")
    val store =
      InMemoryFocusWellStore(
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
      )

    val repo = FocusWellRepository(store, clock::now)

    assertEquals(240.0, repo.state.value.reserveMinutes, 0.0001)
    assertEquals(
      listOf(
        "daily-grant-2026-05-23",
        "daily-grant-2026-05-22",
        "daily-grant-2026-05-21",
        "daily-grant-2026-05-20",
      ),
      repo.state.value.ledger.map { it.id },
    )
  }

  @Test
  fun updateAndDeleteFocusRecord_writeAuditableLedgerAdjustments() {
    val focus = focusRecord(earnedMinutes = 30.0, activeMinutes = 60.0)
    val repo =
      FocusWellRepository(
        InMemoryFocusWellStore(
          baseState(
            focusRecords = listOf(focus),
            ledger =
              listOf(
                ledger(id = "ledger-focus-1", delta = 30.0, sourceId = focus.id),
                ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0),
              ),
          )
        ),
        clock::now,
      )

    clock.instant = Instant.parse("2026-05-20T06:00:00Z")
    repo.updateFocusRecord(focus.id, result = "Edited", activeMinutes = 120.0)
    repo.deleteFocusRecord(focus.id)

    val state = repo.state.value
    assertEquals(60.0, state.reserveMinutes, 0.0001)
    assertEquals(60.0, state.focusRecords.first { it.id == focus.id }.earnedMinutes, 0.0001)
    assertEquals(clock.instant, state.focusRecords.first { it.id == focus.id }.deletedAt)
    assertEquals(listOf(-60.0, 30.0, 30.0, 60.0), state.ledger.map { it.deltaMinutes })
    assertEquals(listOf("Deleted focus", "Edited focus", "Focus", "Daily grant"), state.ledger.map { it.title })
  }

  @Test
  fun init_settlesCompletedDailyTrackerRewardsAtDayBoundary() {
    clock.instant = Instant.parse("2026-05-21T04:00:00Z")
    val tracker =
      DailyTracker(
        id = "vocabulary",
        label = "Vocabulary",
        completed = true,
        rewardMinutes = 15.0,
      )
    val repo =
      FocusWellRepository(
        InMemoryFocusWellStore(
          FocusWellUiState(
            dailyDate = "2026-05-20",
            tags = defaultTags,
            trackers = listOf(tracker),
            ledger = listOf(ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0)),
          )
        ),
        clock::now,
      )

    val state = repo.state.value
    assertEquals("2026-05-21", state.dailyDate)
    assertEquals(135.0, state.reserveMinutes, 0.0001)
    assertEquals(
      listOf("daily-grant-2026-05-21", "tracker-reward-2026-05-20-vocabulary", "daily-grant-2026-05-20"),
      state.ledger.map { it.id },
    )
    assertEquals(false, state.trackers.first().completed)
    assertEquals(15.0, state.trackers.first().rewardMinutes, 0.0001)
  }

  @Test
  fun init_usesConfiguredDailyGrantAndBoundaryForNewLedgerEntries() {
    clock.instant = Instant.parse("2026-05-21T07:00:00Z")
    val rules = FocusWellRules(dailyGrantMinutes = 45.0, dayBoundaryHour = 7, sleepProtectionStartHour = 2)

    val repo =
      FocusWellRepository(
        InMemoryFocusWellStore(
          FocusWellUiState(
            dailyDate = "2026-05-20",
            rules = rules,
            tags = defaultTags,
            trackers = defaultTrackers,
            ledger = emptyList(),
          )
        ),
        clock::now,
      )

    val grant = repo.state.value.ledger.first { it.id == "daily-grant-2026-05-21" }
    assertEquals(45.0, grant.deltaMinutes, 0.0001)
    assertEquals(Instant.parse("2026-05-21T07:00:00Z"), grant.createdAt)
    assertEquals("2026-05-21", repo.state.value.dailyDate)
  }

  @Test
  fun deleteLeisureRecord_restoresReserveWithLedgerAdjustment() {
    val leisure =
      LeisureRecord(
        id = "leisure-1",
        startedAt = Instant.parse("2026-05-20T05:00:00Z"),
        endedAt = Instant.parse("2026-05-20T05:20:00Z"),
        elapsedMinutes = 20.0,
        costMinutes = 20.0,
        dailyDate = "2026-05-20",
      )
    val repo =
      FocusWellRepository(
        InMemoryFocusWellStore(
          baseState(
            leisureRecords = listOf(leisure),
            ledger =
              listOf(
                ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0),
                ledger(id = "ledger-leisure-1", title = "Leisure", delta = -20.0, sourceId = leisure.id),
              ),
          )
        ),
        clock::now,
      )

    clock.instant = Instant.parse("2026-05-20T06:00:00Z")
    repo.deleteLeisureRecord(leisure.id)

    val state = repo.state.value
    assertEquals(60.0, state.reserveMinutes, 0.0001)
    assertEquals(clock.instant, state.leisureRecords.first { it.id == leisure.id }.deletedAt)
    assertEquals(20.0, state.ledger.first().deltaMinutes, 0.0001)
    assertEquals("Deleted leisure", state.ledger.first().title)
  }

  @Test
  fun endLeisure_entersDepletedStateAtReserveExhaustionAndCanExit() {
    val repo =
      FocusWellRepository(
        InMemoryFocusWellStore(
          baseState(
            activeMode =
              ActiveMode.Leisure(
                startedAt = Instant.parse("2026-05-20T05:00:00Z"),
                reminderSessionId = "leisure-session",
              ),
            ledger = listOf(ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 10.0)),
          )
        ),
        clock::now,
      )

    clock.instant = Instant.parse("2026-05-20T05:20:00Z")
    val sessionId = repo.endLeisure()

    assertEquals("leisure-session", sessionId)
    assertEquals(0.0, repo.state.value.reserveMinutes, 0.0001)
    assertTrue(repo.state.value.activeMode is ActiveMode.Depleted)
    assertEquals(10.0, repo.state.value.leisureRecords.first().costMinutes, 0.0001)
    assertEquals(Instant.parse("2026-05-20T05:10:00Z"), repo.state.value.leisureRecords.first().endedAt)

    repo.endDepleted()

    assertEquals(ActiveMode.None, repo.state.value.activeMode)
  }

  @Test
  fun importExport_roundTripsStateAndRejectsBadTimestamps() {
    val repo =
      FocusWellRepository(
        InMemoryFocusWellStore(
          baseState(
            focusRecords = listOf(focusRecord()),
            ledger = listOf(ledger(id = "ledger-focus-1", delta = 30.0, sourceId = "focus-1")),
          )
        ),
        clock::now,
      )
    val exported = repo.exportJson()
    val imported = FocusWellRepository(InMemoryFocusWellStore(), clock::now)

    assertTrue(imported.importJson(exported))
    val roundTrip = JSONObject(imported.exportJson())
    assertEquals(JSONObject(exported).getString("dailyDate"), roundTrip.getString("dailyDate"))
    assertEquals(JSONObject(exported).getJSONArray("focusRecords").length(), roundTrip.getJSONArray("focusRecords").length())
    assertEquals(JSONObject(exported).getJSONArray("ledger").length(), roundTrip.getJSONArray("ledger").length())
    assertEquals(
      JSONObject(exported).getJSONArray("focusRecords").getJSONObject(0).getString("id"),
      roundTrip.getJSONArray("focusRecords").getJSONObject(0).getString("id"),
    )

    val invalid =
      JSONObject(exported)
        .apply {
          getJSONArray("ledger").getJSONObject(0).put("createdAt", "not-an-instant")
        }
        .toString()

    assertEquals(false, imported.importJson(invalid))
  }

  private fun baseState(
    activeMode: ActiveMode = ActiveMode.None,
    focusRecords: List<FocusRecord> = emptyList(),
    leisureRecords: List<LeisureRecord> = emptyList(),
    ledger: List<LedgerEntry> = emptyList(),
  ): FocusWellUiState =
    FocusWellUiState(
      dailyDate = "2026-05-20",
      activeMode = activeMode,
      tags = defaultTags,
      trackers = defaultTrackers,
      focusRecords = focusRecords,
      leisureRecords = leisureRecords,
      ledger = ledger,
    )

  private fun focusRecord(
    earnedMinutes: Double = 30.0,
    activeMinutes: Double = 60.0,
  ): FocusRecord =
    FocusRecord(
      id = "focus-1",
      task = "Math",
      result = "Done",
      type = SessionType.Input,
      tagName = "math",
      tagMultiplier = 1.0,
      typeRate = 0.5,
      startedAt = Instant.parse("2026-05-20T05:00:00Z"),
      endedAt = Instant.parse("2026-05-20T06:00:00Z"),
      activeDurationMinutes = activeMinutes,
      earnedMinutes = earnedMinutes,
      dailyDate = "2026-05-20",
    )

  private fun ledger(
    id: String,
    title: String = "Focus",
    delta: Double,
    sourceId: String? = null,
  ): LedgerEntry =
    LedgerEntry(
      id = id,
      title = title,
      deltaMinutes = delta,
      createdAt = Instant.parse("2026-05-20T05:00:00Z"),
      sourceId = sourceId,
    )
}

private class InMemoryFocusWellStore(
  initial: FocusWellUiState? = null,
) : FocusWellStore {
  var current: FocusWellUiState? = initial

  override fun loadState(): FocusWellUiState? = current

  override fun persistChange(previous: FocusWellUiState?, next: FocusWellUiState) {
    current = next
  }

  override fun clear() {
    current = null
  }
}

private class MutableClock(
  var instant: Instant,
) {
  fun now(): Instant = instant
}
