package io.github.hyunjunghoon.docscan.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.hyunjunghoon.docscan.data.Doc
import io.github.hyunjunghoon.docscan.data.ExportFormat
import io.github.hyunjunghoon.docscan.data.ExportResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders Play Store screenshots. Saved only with -Proborazzi.test.record=true. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h780dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val out = "build/outputs/store"

    /** A fake scanned page: white sheet with a title bar and text lines. */
    private fun page(dir: File, name: String, accent: Int, lines: Int): File {
        val bmp = Bitmap.createBitmap(600, 840, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val p = Paint().apply { color = accent }
        c.drawRect(60f, 70f, 380f, 110f, p)
        p.color = Color.rgb(170, 170, 170)
        repeat(lines) { i -> c.drawRect(60f, 160f + i * 42f, if (i % 4 == 3) 330f else 540f, 178f + i * 42f, p) }
        val f = File(dir, name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return f
    }

    private fun docs(): List<Doc> {
        val dir = File(context.cacheDir, "shots").apply { mkdirs() }
        val day = 86_400_000L
        val now = 1_791_000_000_000L
        return listOf(
            Doc("1", "Passport application", now, listOf(page(dir, "a1.jpg", Color.rgb(26, 115, 232), 14), page(dir, "a2.jpg", Color.rgb(26, 115, 232), 10))),
            Doc("2", "Rental contract", now - day, List(5) { page(dir, "b$it.jpg", Color.rgb(52, 168, 83), 16) }),
            Doc("3", "Exam admit card", now - 3 * day, listOf(page(dir, "c1.jpg", Color.rgb(234, 67, 53), 8))),
            Doc("4", "Receipts March", now - 9 * day, List(3) { page(dir, "d$it.jpg", Color.rgb(251, 188, 4), 12) }),
        )
    }

    private fun settle() {
        compose.waitForIdle()
        Thread.sleep(800) // thumbnails decode on Dispatchers.IO
        compose.waitForIdle()
    }

    @Test
    fun home() {
        val d = docs()
        compose.setContent {
            AppTheme(dynamic = false) {
                HomeScreen(d, false, SnackbarHostState(), false, {}, {}, {}, {})
            }
        }
        settle()
        captureScreenRoboImage("$out/1_home.png")
    }

    @Test
    fun document() {
        val d = docs()[1]
        compose.setContent {
            AppTheme(dynamic = false) {
                DocScreen(d, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {})
            }
        }
        settle()
        captureScreenRoboImage("$out/2_document.png")
    }

    @Test
    fun exportOptions() {
        val d = docs()[0]
        compose.setContent {
            AppTheme(dynamic = false) {
                DocScreen(d, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {})
                ExportSheet(ExportFormat.PDF, 100, {}, { _, _ -> })
            }
        }
        settle()
        captureScreenRoboImage("$out/3_export.png")
    }

    @Test
    fun result() {
        val d = docs()[0]
        val r = ExportResult(listOf(File("Passport application.pdf")), 97_340, 100_000, true, ExportFormat.PDF)
        compose.setContent {
            AppTheme(dynamic = false) {
                DocScreen(d, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {})
                ExportResultDialog(r, {}, {}, {})
            }
        }
        settle()
        captureScreenRoboImage("$out/4_result.png")
    }
}
