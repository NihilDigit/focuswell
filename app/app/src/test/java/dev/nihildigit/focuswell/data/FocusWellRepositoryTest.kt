package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.defaultTags
import dev.nihildigit.focuswell.domain.defaultTrackers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun init_backfillsMissingDailyGrantsAcrossBusinessDays() = runTest {
    clock.instant = Instant.parse("2026-05-23T13:00:00Z")
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
                createdAt = Instant.parse("2026-05-20T00:00:00Z"),
              )
            ),
        )
      )

    val repo = newRepository(store, clock::now)

    assertEquals(249.4575, repo.state.value.reserveMinutes, 0.0001)
    assertEquals(
      listOf(
        "daily-grant-2026-05-23",
        "daily-grant-2026-05-22",
        "daily-grant-2026-05-21",
        "daily-interest-2026-05-23",
        "daily-interest-2026-05-22",
        "daily-interest-2026-05-21",
        "daily-grant-2026-05-20",
      ),
      repo.state.value.ledger.map { it.id },
    )
  }

  @Test
  fun updateAndDeleteFocusRecord_writeAuditableLedgerAdjustments() = runTest {
    val focus = focusRecord(earnedMinutes = 30.0, activeMinutes = 60.0)
    val repo =
      newRepository(
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
  fun init_settlesCompletedDailyTrackerRewardsAtDayBoundary() = runTest {
    clock.instant = Instant.parse("2026-05-21T04:00:00Z")
    val tracker =
      DailyTracker(
        id = "vocabulary",
        label = "Vocabulary",
        completed = true,
        rewardMinutes = 15.0,
      )
    val repo =
      newRepository(
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
    assertEquals(138.75, state.reserveMinutes, 0.0001)
    assertEquals(
      listOf("daily-grant-2026-05-21", "daily-interest-2026-05-21", "tracker-reward-2026-05-20-vocabulary", "daily-grant-2026-05-20"),
      state.ledger.map { it.id },
    )
    assertEquals(false, state.trackers.first().completed)
    assertEquals(15.0, state.trackers.first().rewardMinutes, 0.0001)
  }

  @Test
  fun init_usesConfiguredDailyGrantAndBoundaryForNewLedgerEntries() = runTest {
    clock.instant = Instant.parse("2026-05-21T07:00:00Z")
    val rules = FocusWellRules(dailyGrantMinutes = 45.0, dayBoundaryHour = 7, sleepProtectionStartHour = 2)

    val repo =
      newRepository(
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
  fun completeMorningCheckIn_awardsWakeBonusFromRulesWindow() = runTest {
    val repo =
      newRepository(
        InMemoryFocusWellStore(
          baseState(
            rules = FocusWellRules(wakeTargetHour = 9, wakeTargetMinute = 0),
            ledger = listOf(ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0)),
          )
        ),
        clock::now,
      )

    repo.completeMorningCheckIn(
      checkInStartedAt = Instant.parse("2026-05-20T08:00:00Z"),
      phoneCostMinutes = 0.0,
      reviewedSegmentCount = 0,
      settledUntil = Instant.parse("2026-05-20T04:00:00Z"),
    )

    val wakeBonus = repo.state.value.ledger.first { it.id == "wake-bonus-2026-05-20" }
    assertEquals("Wake bonus", wakeBonus.title)
    assertEquals(30.0, wakeBonus.deltaMinutes, 0.0001)
  }

  @Test
  fun completeMorningCheckIn_skipsWakeBonusOutsideRulesWindow() = runTest {
    val repo =
      newRepository(
        InMemoryFocusWellStore(
          baseState(
            rules = FocusWellRules(wakeTargetHour = 9, wakeTargetMinute = 0),
            ledger = listOf(ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0)),
          )
        ),
        clock::now,
      )

    repo.completeMorningCheckIn(
      checkInStartedAt = Instant.parse("2026-05-20T07:59:00Z"),
      phoneCostMinutes = 0.0,
      reviewedSegmentCount = 0,
      settledUntil = Instant.parse("2026-05-20T04:00:00Z"),
    )

    assertTrue(repo.state.value.ledger.none { it.id.startsWith("wake-bonus-") })
  }

  @Test
  fun completePhoneUsageSettlement_deductsAndAdvancesSettlementCursor() = runTest {
    val repo =
      newRepository(
        InMemoryFocusWellStore(
          baseState(
            ledger = listOf(ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0)),
          )
        ),
        clock::now,
      )

    repo.completePhoneUsageSettlement(
      settlementStartedAt = Instant.parse("2026-05-20T09:00:00Z"),
      phoneCostMinutes = 18.0,
      reviewedSegmentCount = 2,
      settledUntil = Instant.parse("2026-05-20T08:55:00Z"),
    )

    val state = repo.state.value
    assertEquals(42.0, state.reserveMinutes, 0.0001)
    assertEquals(Instant.parse("2026-05-20T08:55:00Z"), state.lastPhoneUsageSettlementAt)
    assertEquals("Phone usage", state.ledger.first().title)
    assertEquals(-18.0, state.ledger.first().deltaMinutes, 0.0001)
  }

  @Test
  fun addConfigItems_usesInjectedClockForGeneratedIds() = runTest {
    clock.instant = Instant.parse("2026-05-20T05:00:00Z")
    val repo = newRepository(InMemoryFocusWellStore(baseState()), clock::now)

    repo.addTag("Deep Work", 1.2)
    repo.addBooleanTracker("Read", 15.0)
    repo.addRuleTracker("Math quota", "math", 180.0, 60.0)

    val state = repo.state.value
    assertEquals("tag-1779253200000", state.tags.first { it.name == "Deep Work" }.id)
    assertEquals("tracker-1779253200000", state.trackers.first { it.label == "Read" }.id)
    assertEquals("rule-1779253200000", state.trackers.first { it.label == "Math quota" }.id)
  }

  @Test
  fun addManualFocusRecord_appliesFocusFormulaAndUpdatesRuleTrackerProgress() = runTest {
    val repo = newRepository(InMemoryFocusWellStore(baseState()), clock::now)

    repo.addManualFocusRecord("Catch-up", 60.0, "offline block", SessionType.Input, tagId = "408")

    val state = repo.state.value
    val record = state.focusRecords.first()
    assertEquals("408", record.tagName)
    assertEquals(1.5, record.tagMultiplier, 0.0001)
    assertEquals(60.0, record.activeDurationMinutes, 0.0001)
    assertEquals(45.0, record.earnedMinutes, 0.0001)
    assertEquals(record.id, state.ledger.first().sourceId)
    assertEquals(45.0, state.ledger.first().deltaMinutes, 0.0001)
    assertEquals("1h 0m / 3h", state.trackers.first { it.id == "408-3h" }.progressLabel)
  }

  @Test
  fun addManualFocusRecord_usesDefaultInputRateBeforeMathTagMultiplier() = runTest {
    val repo = newRepository(InMemoryFocusWellStore(baseState()), clock::now)

    repo.addManualFocusRecord("Math", 60.0, null, SessionType.Input, tagId = "math")

    val state = repo.state.value
    val record = state.focusRecords.first()
    assertEquals("math", record.tagName)
    assertEquals(60.0, record.activeDurationMinutes, 0.0001)
    assertEquals(60.0, record.earnedMinutes, 0.0001)
    assertEquals(60.0, state.ledger.first().deltaMinutes, 0.0001)
    assertEquals("1h 0m / 3h", state.trackers.first { it.id == "math-3h" }.progressLabel)
  }

  @Test
  fun deleteLeisureRecord_restoresReserveWithLedgerAdjustment() = runTest {
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
      newRepository(
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
  fun endFocus_subtractsCorrectionBeforeOutcomeMultiplier() = runTest {
    val repo =
      newRepository(
        InMemoryFocusWellStore(baseState(ledger = listOf(ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0)))),
        clock::now,
      )

    repo.startFocus("Math", SessionType.Input, tagId = null)
    clock.instant = Instant.parse("2026-05-20T06:00:00Z")
    repo.endFocus("Drifted", correctionMinutes = 20.0)

    val record = repo.state.value.focusRecords.first()
    assertEquals(40.0, record.activeDurationMinutes, 0.0001)
    assertEquals(6.0, record.earnedMinutes, 0.0001)
    assertEquals(6.0, repo.state.value.ledger.first().deltaMinutes, 0.0001)
  }

  @Test
  fun ideas_moveArchiveAndRoundTripThroughJson() = runTest {
    val repo =
      newRepository(
        InMemoryFocusWellStore(baseState()),
        clock::now,
      )

    repo.addIdea("Try a visual proof")
    val ideaId = repo.state.value.ideas.first().id
    repo.moveIdea(ideaId, IdeaQuadrant.Explore)
    repo.updateIdea(
      ideaId,
      "Try a visual proof",
      listOf(
        IdeaChecklistItem(id = "task-1", text = "Sketch matrix", checked = true),
        IdeaChecklistItem(id = "task-2", text = "Check edge cases"),
      ),
    )
    repo.archiveIdea(ideaId)

    val idea = repo.state.value.ideas.first()
    assertEquals("Try a visual proof", idea.text)
    assertEquals(IdeaQuadrant.Explore, idea.quadrant)
    assertEquals(listOf("Sketch matrix", "Check edge cases"), idea.checklist.map { it.text })
    assertEquals(listOf(true, false), idea.checklist.map { it.checked })
    assertEquals(clock.instant, idea.archivedAt)

    val imported = newRepository(InMemoryFocusWellStore(), clock::now)
    assertTrue(imported.importJson(repo.exportJson()))
    assertEquals(IdeaQuadrant.Explore, imported.state.value.ideas.first().quadrant)
    assertEquals(listOf("Sketch matrix", "Check edge cases"), imported.state.value.ideas.first().checklist.map { it.text })
    assertEquals(listOf(true, false), imported.state.value.ideas.first().checklist.map { it.checked })
    assertEquals(clock.instant, imported.state.value.ideas.first().archivedAt)
  }

  @Test
  fun endLeisure_entersDepletedStateAtReserveExhaustionAndCanExit() = runTest {
    val repo =
      newRepository(
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
    assertEquals("2026-05-20", repo.state.value.dailyGrantPausedUntilDate)
    assertEquals(10.0, repo.state.value.leisureRecords.first().costMinutes, 0.0001)
    assertEquals(Instant.parse("2026-05-20T05:05:00Z"), repo.state.value.leisureRecords.first().endedAt)

    repo.endDepleted()

    assertEquals(ActiveMode.None, repo.state.value.activeMode)
  }

  @Test
  fun importExport_roundTripsStateAndRejectsBadTimestamps() = runTest {
    val repo =
      newRepository(
        InMemoryFocusWellStore(
          baseState(
            rules =
              FocusWellRules(
                longSessionRemindersEnabled = false,
                phoneUsageChargeFreePackages = setOf("app.words"),
              ),
            focusRecords = listOf(focusRecord()),
            ledger = listOf(ledger(id = "ledger-focus-1", delta = 30.0, sourceId = "focus-1")),
          )
        ),
        clock::now,
      )
    val exported = repo.exportJson()
    val imported = newRepository(InMemoryFocusWellStore(), clock::now)

    assertTrue(imported.importJson(exported))
    val exportedJson = jsonObject(exported)
    val roundTrip = jsonObject(imported.exportJson())
    assertEquals(exportedJson["dailyDate"]?.jsonPrimitive?.contentOrNull, roundTrip["dailyDate"]?.jsonPrimitive?.contentOrNull)
    assertEquals(exportedJson["focusRecords"]?.jsonArray?.size, roundTrip["focusRecords"]?.jsonArray?.size)
    assertEquals(exportedJson["ledger"]?.jsonArray?.size, roundTrip["ledger"]?.jsonArray?.size)
    assertEquals(
      exportedJson["focusRecords"]?.jsonArray?.get(0)?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull,
      roundTrip["focusRecords"]?.jsonArray?.get(0)?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull,
    )
    assertEquals(false, roundTrip["rules"]?.jsonObject?.get("longSessionRemindersEnabled")?.jsonPrimitive?.boolean)
    assertEquals(
      "app.words",
      roundTrip["rules"]?.jsonObject?.get("phoneUsageChargeFreePackages")?.jsonArray?.first()?.jsonPrimitive?.contentOrNull,
    )

    val invalid =
      exportedJson
        .withArrayItemObject("ledger", 0) { entry -> entry.withProperty("createdAt", JsonPrimitive("not-an-instant")) }
        .toString()

    assertEquals(false, imported.importJson(invalid))
  }

  @Test
  fun importJson_canPreserveCloudUpdateTimestamp() = runTest {
    val repo =
      newRepository(
        InMemoryFocusWellStore(
          baseState(
            ledger = listOf(ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0))
          )
        ),
        clock::now,
      )
    val cloudUpdatedAt = "2026-05-18T01:02:03Z"
    val exported =
      jsonObject(repo.exportJson())
        .withProperty("stateUpdatedAtUtc", JsonPrimitive(cloudUpdatedAt))
        .toString()

    clock.instant = Instant.parse("2026-05-20T08:00:00Z")
    val manualImport = newRepository(InMemoryFocusWellStore(), clock::now)
    assertTrue(manualImport.importJson(exported))
    assertEquals(clock.instant, manualImport.state.value.stateUpdatedAt)

    val cloudRestore = newRepository(InMemoryFocusWellStore(), clock::now)
    assertTrue(cloudRestore.importJson(exported, touchUpdatedAt = false))
    assertEquals(Instant.parse(cloudUpdatedAt), cloudRestore.state.value.stateUpdatedAt)
  }

  @Test
  fun importExport_preservesPausedFocusReminderMetadata() = runTest {
    val pausedAt = Instant.parse("2026-05-20T05:30:00Z")
    val repo =
      newRepository(
        InMemoryFocusWellStore(
          baseState(
            activeMode =
              ActiveMode.Focus(
                task = "Math",
                type = SessionType.Input,
                tag = defaultTags.first(),
                startedAt = Instant.parse("2026-05-20T05:00:00Z"),
                reminderSessionId = "focus-session",
                revision = 3,
                paused = true,
                pausedAt = pausedAt,
                pausedDurationMillis = 120_000L,
              )
          )
        ),
        clock::now,
      )

    val imported = newRepository(InMemoryFocusWellStore(), clock::now)
    assertTrue(imported.importJson(repo.exportJson()))

    val activeFocus = imported.state.value.activeMode as ActiveMode.Focus
    assertEquals("focus-session", activeFocus.reminderSessionId)
    assertEquals(3, activeFocus.revision)
    assertEquals(true, activeFocus.paused)
    assertEquals(pausedAt, activeFocus.pausedAt)
    assertEquals(120_000L, activeFocus.pausedDurationMillis)
  }

  @Test
  fun importJson_treatsLegacyWindDownModeAsInactive() = runTest {
    val exported =
      jsonObject(newRepository(InMemoryFocusWellStore(baseState()), clock::now).exportJson())
        .withProperty("activeMode", buildJsonObject { put("kind", "windDown") })
        .toString()

    val imported = newRepository(InMemoryFocusWellStore(), clock::now)

    assertTrue(imported.importJson(exported))
    assertEquals(ActiveMode.None, imported.state.value.activeMode)
  }

  @Test
  fun importJson_migratesLegacyTaggedManualLedgerIntoFocusRecord() = runTest {
    val legacy =
      jsonObject(newRepository(InMemoryFocusWellStore(baseState()), clock::now).exportJson())
        .withProperty(
          "ledger",
          buildJsonArray {
            add(
              buildJsonObject {
                put("id", "manual-adjustment-1779253200000")
                put("title", "Catch-up")
                put("deltaMinutes", 60.0)
                put("createdAt", "2026-05-20T05:00:00Z")
                put("note", "offline")
                put("tagName", "408")
              }
            )
          },
        )
        .toString()

    val imported = newRepository(InMemoryFocusWellStore(), clock::now)

    assertTrue(imported.importJson(legacy))
    val record = imported.state.value.focusRecords.single()
    assertEquals("manual-focus-1779253200000", record.id)
    assertEquals("Catch-up", record.task)
    assertEquals("408", record.tagName)
    assertEquals(60.0, record.activeDurationMinutes, 0.0001)
    assertEquals(45.0, record.earnedMinutes, 0.0001)
    val ledgerIds = imported.state.value.ledger.map { it.id }
    assertTrue("ledger-manual-focus-1779253200000" in ledgerIds)
    assertFalse("manual-adjustment-1779253200000" in ledgerIds)
  }

  private fun baseState(
    activeMode: ActiveMode = ActiveMode.None,
    rules: FocusWellRules = FocusWellRules(dayBoundaryHour = 4),
    focusRecords: List<FocusRecord> = emptyList(),
    leisureRecords: List<LeisureRecord> = emptyList(),
    ledger: List<LedgerEntry> = emptyList(),
  ): FocusWellUiState =
    FocusWellUiState(
      dailyDate = "2026-05-20",
      rules = rules,
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

private val testJson = Json { ignoreUnknownKeys = true }

private suspend fun newRepository(store: FocusWellStore, now: () -> Instant): FocusWellRepository =
  FocusWellRepository(store, now).also { it.initialize() }

private fun jsonObject(raw: String): JsonObject = testJson.parseToJsonElement(raw).jsonObject

private fun JsonObject.withProperty(key: String, value: JsonElement): JsonObject =
  buildJsonObject {
    this@withProperty.forEach { (existingKey, existingValue) -> put(existingKey, existingValue) }
    put(key, value)
  }

private fun JsonObject.withArrayItemObject(
  key: String,
  index: Int,
  transform: (JsonObject) -> JsonObject,
): JsonObject =
  withProperty(
    key,
    buildJsonArray {
      this@withArrayItemObject[key]?.jsonArray?.forEachIndexed { currentIndex, element ->
        add(if (currentIndex == index) transform(element.jsonObject) else element)
      }
    },
  )

private class InMemoryFocusWellStore(
  initial: FocusWellUiState? = null,
) : FocusWellStore {
  var current: FocusWellUiState? = initial

  override suspend fun loadState(): FocusWellUiState? = current

  override suspend fun persistChange(previous: FocusWellUiState?, next: FocusWellUiState) {
    current = next
  }

  override suspend fun clear() {
    current = null
  }
}

private class MutableClock(
  var instant: Instant,
) {
  fun now(): Instant = instant
}
