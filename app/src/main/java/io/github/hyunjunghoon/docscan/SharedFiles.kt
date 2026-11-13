package io.github.hyunjunghoon.docscan

import android.content.Intent
import android.net.Uri
import android.os.Build

/** Files another app sent with "Share → SizeScan". */
object SharedFiles {
    fun uris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(stream(intent))
            Intent.ACTION_SEND_MULTIPLE -> streams(intent)
            else -> emptyList()
        // content:// only: a file:// path from another app could point into this app's own storage.
        }.filter { it.scheme == "content" }.distinct()
    }

    private fun stream(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

    private fun streams(intent: Intent): List<Uri> =
        (
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        ).orEmpty()
}
