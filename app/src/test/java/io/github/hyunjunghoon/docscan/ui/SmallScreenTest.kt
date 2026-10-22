package io.github.hyunjunghoon.docscan.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.hyunjunghoon.docscan.R
import io.github.hyunjunghoon.docscan.data.Doc
import io.github.hyunjunghoon.docscan.data.ExportFormat
import io.github.hyunjunghoon.docscan.data.ExportResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Smallest common phone width with long German labels: every action must stay on screen. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "de-w320dp-h568dp-mdpi")
class SmallScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun resultDialogShowsAllButtons() {
        val result = ExportResult(listOf(File("a.pdf")), 98_000, 100_000, true, ExportFormat.PDF)
        compose.setContent { AppTheme(dynamic = false) { ExportResultDialog(result, {}, {}, {}, onOpen = {}) } }
        listOf(R.string.action_open, R.string.action_save, R.string.action_share, R.string.close).forEach {
            compose.onNodeWithText(str(it)).assertIsDisplayed()
        }
    }

    @Test
    fun docScreenShowsBottomActions() {
        val doc = Doc("1", "Antrag", 0L, emptyList())
        compose.setContent {
            AppTheme(dynamic = false) {
                DocScreen(doc, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText(str(R.string.action_add_pages)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.action_export)).assertIsDisplayed()
    }

    @Test
    fun docScreenWithPagesShowsHintAndActions() {
        // Missing file: the thumbnail stays empty, which is all this layout check needs.
        val doc = Doc("1", "Antrag", 0L, listOf(File("missing.jpg")))
        compose.setContent {
            AppTheme(dynamic = false) {
                DocScreen(doc, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {})
            }
        }
        listOf(R.string.page_tap_hint, R.string.action_add_pages, R.string.action_export).forEach {
            compose.onNodeWithText(str(it)).assertIsDisplayed()
        }
    }

    @Test
    fun helpDialogFits() {
        compose.setContent { AppTheme(dynamic = false) { HelpDialog {} } }
        compose.onNodeWithText(str(R.string.ok)).assertIsDisplayed()
    }

    @Test
    fun themeDialogShowsAllChoices() {
        compose.setContent { AppTheme(dynamic = false, mode = ThemeMode.DARK) { ThemeDialog(ThemeMode.DARK, {}, {}) } }
        listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark, R.string.close).forEach {
            compose.onNodeWithText(str(it)).assertIsDisplayed()
        }
    }
}
