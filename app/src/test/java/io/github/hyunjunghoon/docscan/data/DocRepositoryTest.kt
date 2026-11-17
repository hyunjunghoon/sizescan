package io.github.hyunjunghoon.docscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class DocRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Page width identifies which page is where. */
    private fun jpeg(width: Int, height: Int): Uri {
        val f = File.createTempFile("src", ".jpg", context.cacheDir)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(f)
    }

    private fun size(f: File): Pair<Int, Int> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        return o.outWidth to o.outHeight
    }

    private fun newDoc(repo: DocRepository): String {
        val id = repo.create("t")
        repo.addPages(id, listOf(jpeg(100, 200), jpeg(110, 200), jpeg(120, 200)), fromScanner = true)
        return id
    }

    @Test
    fun movePageSwapsWithNeighbour() {
        val repo = DocRepository(context)
        val id = newDoc(repo)
        val before = repo.get(id)!!
        repo.movePage(id, before.pages[0], 1)
        val after = repo.get(id)!!
        assertEquals(listOf(110, 100, 120), after.pages.map { size(it).first })
        assertNotEquals(before.stamp, after.stamp)
        // Moving past either end does nothing.
        repo.movePage(id, after.pages[0], -1)
        repo.movePage(id, after.pages[2], 1)
        assertEquals(listOf(110, 100, 120), repo.get(id)!!.pages.map { size(it).first })
    }

    @Test
    fun rotatePageTurnsItSideways() {
        val repo = DocRepository(context)
        val id = newDoc(repo)
        val page = repo.get(id)!!.pages[1]
        repo.rotatePage(id, page)
        assertEquals(200 to 110, size(page))
        assertEquals(3, repo.get(id)!!.pages.size)
    }

    /** Photos picked from the gallery go through normalizeToJpeg (broken before 1.1.1: always "Cannot open"). */
    @Test
    fun photosFromGalleryAreImported() {
        val repo = DocRepository(context)
        val id = repo.create("photos")
        val failed = repo.addPages(id, listOf(jpeg(1200, 1600), jpeg(800, 600)), fromScanner = false)
        assertEquals(0, failed)
        val pages = repo.get(id)!!.pages
        assertEquals(2, pages.size)
        assertEquals(1200 to 1600, size(pages[0]))
    }

    @Test
    fun notAnImageIsReportedAsFailed() {
        val repo = DocRepository(context)
        val id = repo.create("bad")
        val f = File.createTempFile("bad", ".jpg", context.cacheDir).apply { writeText("not an image") }
        assertEquals(1, repo.addPages(id, listOf(Uri.fromFile(f)), fromScanner = false))
    }

    /** A document that is still being imported has no pages yet; listing must not delete it. */
    @Test
    fun listKeepsAFreshEmptyDocument() {
        val repo = DocRepository(context)
        val id = repo.create("importing")
        repo.list()
        assertEquals(true, File(context.filesDir, "docs/$id").isDirectory)
        assertEquals(0, repo.addPages(id, listOf(jpeg(300, 400)), fromScanner = false))
        assertEquals(1, repo.get(id)!!.pages.size)
    }
}
