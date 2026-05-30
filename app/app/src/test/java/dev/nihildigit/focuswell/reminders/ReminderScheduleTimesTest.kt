package dev.nihildigit.focuswell.reminders

import dev.nihildigit.focuswell.domain.FocusWellRules
import kotlinx.datetime.TimeZone
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderScheduleTimesTest {
  private val shanghai = TimeZone.of("Asia/Shanghai")

  @Test
  fun nextLateNightInstant_returnsTonightWhenSleepProtectionStartsWithinSixteenHours() {
    val now = Instant.parse("2026-05-20T06:00:00Z") // 14:00 Shanghai

    assertEquals(
      Instant.parse("2026-05-20T15:00:00Z"), // 23:00 Shanghai
      nextLateNightInstant(now, FocusWellRules(sleepProtectionStartHour = 23), zone = shanghai),
    )
  }

  @Test
  fun nextLateNightInstant_returnsTomorrowWhenSleepProtectionStartsWithinSixteenHours() {
    val now = Instant.parse("2026-05-20T23:30:00Z") // 07:30 Shanghai, next 23:00 is 15.5h away

    assertEquals(
      Instant.parse("2026-05-21T15:00:00Z"),
      nextLateNightInstant(now, FocusWellRules(sleepProtectionStartHour = 23), zone = shanghai),
    )
  }

  @Test
  fun nextLateNightInstant_returnsNullWhenNextStartIsTooFarAway() {
    val earlyMorning = Instant.parse("2026-05-20T22:00:00Z") // 06:00 Shanghai, next 23:00 is 17h away
    assertNull(nextLateNightInstant(earlyMorning, FocusWellRules(sleepProtectionStartHour = 23), zone = shanghai))
  }
}
