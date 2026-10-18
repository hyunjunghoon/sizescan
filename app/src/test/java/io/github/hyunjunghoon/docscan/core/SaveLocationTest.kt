package io.github.hyunjunghoon.docscan.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveLocationTest {
    private val ext = "com.android.externalstorage.documents"
    private val dl = "com.android.providers.downloads.documents"

    @Test fun externalStorageFile() = assertEquals("Download", SaveLocation.folder(ext, "primary:Download/scan.pdf", false))

    @Test fun nestedFolderKeepsLastTwoLevels() =
        assertEquals("Documents/Scans", SaveLocation.folder(ext, "primary:Work/Documents/Scans/a.pdf", false))

    @Test fun sdCardFile() = assertEquals("DCIM", SaveLocation.folder(ext, "1A2B-3C4D:DCIM/a.jpg", false))

    @Test fun fileAtStorageRootHasNoFolder() = assertNull(SaveLocation.folder(ext, "primary:scan.pdf", false))

    @Test fun pickedTreeIsTheFolderItself() = assertEquals("Documents", SaveLocation.folder(ext, "primary:Documents", true))

    @Test fun storageRootTreeHasNoFolder() = assertNull(SaveLocation.folder(ext, "primary:", true))

    @Test fun downloadsProviderOpaqueIds() {
        assertEquals("Download", SaveLocation.folder(dl, "msf:1234", false))
        assertEquals("Download", SaveLocation.folder(dl, "42", false))
    }

    @Test fun rawPath() = assertEquals("Download", SaveLocation.folder(dl, "raw:/storage/emulated/0/Download/a.pdf", false))

    @Test fun cloudProviderHasNoPath() =
        assertNull(SaveLocation.folder("com.google.android.apps.docs.storage", "doc=encoded=abc", false))
}
