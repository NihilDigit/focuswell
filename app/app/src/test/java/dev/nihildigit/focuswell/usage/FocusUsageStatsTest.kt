package dev.nihildigit.focuswell.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusUsageStatsTest {
  @Test
  fun fallbackAppName_usesReadableLastPackageSegment() {
    assertEquals("youtube", "com.google.android.youtube".fallbackAppName())
    assertEquals("reader app", "org.example.reader_app".fallbackAppName())
  }

  @Test
  fun fallbackAppName_keepsPackageWhenNoReadableSegmentExists() {
    assertEquals("com.", "com.".fallbackAppName())
  }
}
