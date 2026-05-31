package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.sync.CloudSnapshot
import dev.nihildigit.focuswell.sync.CloudSnapshotMetadata
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloudSyncUiStateTest {
  @Test
  fun cloudSyncDecision_uploadsWhenLocalSnapshotIsNewer() {
    val decision =
      cloudSyncDecision(
        localUpdatedAt = Instant.parse("2026-05-20T06:00:00Z"),
        snapshot = snapshot(updatedAt = "2026-05-20T05:00:00Z"),
      )

    assertEquals(CloudSyncDecisionKind.Upload, decision?.kind)
    assertEquals(Instant.parse("2026-05-20T06:00:00Z"), decision?.localUpdatedAt)
    assertNull(decision?.cloudPayload)
  }

  @Test
  fun cloudSyncDecision_restoresWhenCloudSnapshotIsNewer() {
    val decision =
      cloudSyncDecision(
        localUpdatedAt = Instant.parse("2026-05-20T05:00:00Z"),
        snapshot = snapshot(updatedAt = "2026-05-20T06:00:00Z"),
      )

    assertEquals(CloudSyncDecisionKind.Restore, decision?.kind)
    assertEquals("""{"dailyDate":"2026-05-20"}""", decision?.cloudPayload)
  }

  @Test
  fun cloudSyncDecision_returnsNullWhenSnapshotsMatch() {
    val decision =
      cloudSyncDecision(
        localUpdatedAt = Instant.parse("2026-05-20T05:00:00Z"),
        snapshot = snapshot(updatedAt = "2026-05-20T05:00:00Z"),
      )

    assertNull(decision)
  }

  @Test
  fun withCloudDecision_setsAlreadySyncedMessageWhenSnapshotsMatch() {
    val state =
      CloudSyncUiState(syncing = false, error = "old")
        .withCloudDecision(
          localUpdatedAt = Instant.parse("2026-05-20T05:00:00Z"),
          snapshot = snapshot(updatedAt = "2026-05-20T05:00:00Z"),
        )

    assertNull(state.pendingDecision)
    assertNull(state.error)
    assertEquals("This device and the cloud backup are in sync", state.message)
  }

  @Test
  fun cloudUploadSucceeded_clearsSyncingAndKeepsLoginMessage() {
    val state = CloudSyncUiState(syncing = true, error = "old").cloudUploadSucceeded("nihildigit")

    assertEquals(false, state.syncing)
    assertEquals("nihildigit", state.userLogin)
    assertNull(state.error)
    assertEquals("Cloud backup updated", state.message)
  }

  @Test
  fun cloudRestoreFailure_clearsDecisionAndReportsError() {
    val state =
      CloudSyncUiState(
        pendingDecision =
          CloudSyncDecision(
            kind = CloudSyncDecisionKind.Restore,
            localUpdatedAt = Instant.parse("2026-05-20T05:00:00Z"),
            cloudMetadata = snapshot(updatedAt = "2026-05-20T06:00:00Z").metadata,
            cloudPayload = "{}",
          ),
      ).cloudRestoreFailed()

    assertNull(state.pendingDecision)
    assertEquals("Could not restore cloud backup", state.error)
  }

  @Test
  fun cloudOAuthStarted_setsProgressMessageAndClearsError() {
    val state = CloudSyncUiState(error = "old").cloudOAuthStarted()

    assertEquals(true, state.syncing)
    assertNull(state.error)
    assertEquals("Finishing GitHub sign-in", state.message)
  }

  @Test
  fun cloudOAuthSucceeded_returnsSignedInState() {
    val state = cloudOAuthSucceeded("nihildigit")

    assertEquals(false, state.syncing)
    assertEquals("nihildigit", state.userLogin)
    assertEquals("Signed in as nihildigit", state.message)
  }

  @Test
  fun cloudSyncRedirect_extractsOAuthCodeAndRejection() {
    assertEquals(
      CloudSyncRedirect.Code("abc"),
      cloudSyncRedirect(scheme = "focuswell", host = "sync", path = "/oauth", code = "abc", error = null),
    )
    assertEquals(
      CloudSyncRedirect.Rejected("denied"),
      cloudSyncRedirect(scheme = "focuswell", host = "sync", path = "/oauth", code = null, error = "denied"),
    )
    assertEquals(
      CloudSyncRedirect.Ignored,
      cloudSyncRedirect(scheme = "focuswell", host = "other", path = "/oauth", code = "abc", error = null),
    )
  }

  @Test
  fun cloudPayloadFromExport_returnsJsonObjectPayload() {
    val payload = cloudPayloadFromExport("""{"dailyDate":"2026-05-20"}""")

    assertEquals("""{"dailyDate":"2026-05-20"}""", payload.toString())
  }

  private fun snapshot(updatedAt: String): CloudSnapshot =
    CloudSnapshot(
      metadata =
        CloudSnapshotMetadata(
          updatedAtUtc = Instant.parse(updatedAt),
          uploadedAtUtc = Instant.parse("2026-05-20T07:00:00Z"),
          appVersion = "26.5.8",
          jsonHash = "hash",
        ),
      payload =
        buildJsonObject {
          put("dailyDate", "2026-05-20")
        },
    )
}
