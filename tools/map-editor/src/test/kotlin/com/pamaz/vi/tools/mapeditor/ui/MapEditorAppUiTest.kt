package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * The acceptance's explicit UI test: "place a pin -> create a new entry -> press Commit ->
 * success." Scoped to this one flow, not a general Compose UI test harness — no other UI test
 * exists anywhere in this repo. Uses the real `MapEditorApp`/`EditorState` wired to the same fakes
 * `AppModule.kt` binds by default; `FakeCommitService(delayMs = 0)` so the test doesn't wait the
 * ~1.5s a real run shows.
 *
 * This sandbox has no attached display (see this module's README) — whether this actually renders
 * and passes here, versus needing a real machine, is exactly what running it tells you.
 */
class MapEditorAppUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun placeAPin_createANewEntry_commit_success() {
        val state = previewEditorState()

        composeRule.setContent { MapEditorApp(state) }
        composeRule.waitForIdle()

        // Sign in as admin (EDT-01) — the email field is pre-filled with the admin account.
        composeRule.onNodeWithTag("sign-in-password").performTextInput("any-password")
        composeRule.onNodeWithText("SIGN IN").performClick()
        composeRule.waitForIdle()

        // Place a pin (EDT-30) — the Inspector opens automatically (EDT-31, no active entry yet).
        // `MapViewer`'s tap detector also recognizes double-tap-to-zoom, so `detectTapGestures`
        // only fires `onTap` after waiting out the double-tap timeout; `waitForIdle()` alone
        // doesn't advance through that timed wait, so the test clock has to be nudged past it.
        composeRule.onNodeWithContentDescription("Map. Drag to pan, pinch or double-tap to zoom.")
            .performMouseInput { click(Offset(300f, 300f)) }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitForIdle()

        // Create a new entry for it (EDT-54). The form can be taller than the Inspector's visible
        // height (EDT-54's full field list), so the Inspector scrolls (InspectorPanel.kt) and the
        // button needs to be scrolled into view before a synthetic click can reach it.
        composeRule.onNodeWithText("New entry").performClick()
        composeRule.onNodeWithTag("new-entry-name").performTextInput("Test Weapon")
        composeRule.onNodeWithText("Create entry").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Commit (EDT-70..74).
        composeRule.onNodeWithText("COMMIT", substring = true).performClick()
        composeRule.onNodeWithText("Commit").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Committed").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Committed").assertExists()
    }
}
