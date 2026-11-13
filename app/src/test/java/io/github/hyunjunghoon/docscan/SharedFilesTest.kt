package io.github.hyunjunghoon.docscan

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SharedFilesTest {
    private val pdf = Uri.parse("content://media/external/file/12")
    private val photo = Uri.parse("content://media/external/images/media/7")

    @Test
    fun singleShare() {
        val i = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, pdf)
        assertEquals(listOf(pdf), SharedFiles.uris(i))
    }

    @Test
    fun multipleShareKeepsOrderAndDropsDuplicates() {
        val i = Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(photo, pdf, photo))
        assertEquals(listOf(photo, pdf), SharedFiles.uris(i))
    }

    @Test
    fun fileUrisAreIgnored() {
        val i = Intent(Intent.ACTION_SEND).setType("image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///data/data/com.tallowbird.sizescan/files/docs/x/p0001.jpg"))
        assertEquals(emptyList<Uri>(), SharedFiles.uris(i))
    }

    @Test
    fun launcherAndEmptyIntentsImportNothing() {
        assertEquals(emptyList<Uri>(), SharedFiles.uris(null))
        assertEquals(emptyList<Uri>(), SharedFiles.uris(Intent(Intent.ACTION_MAIN)))
        assertEquals(emptyList<Uri>(), SharedFiles.uris(Intent(Intent.ACTION_SEND).setType("image/png")))
    }
}
