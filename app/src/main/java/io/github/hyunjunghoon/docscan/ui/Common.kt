package io.github.hyunjunghoon.docscan.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.hyunjunghoon.docscan.data.ExportFormat
import io.github.hyunjunghoon.docscan.data.ThumbCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun Thumbnail(file: File?, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, file?.path, file?.lastModified()) {
        value = file?.let { withContext(Dispatchers.IO) { ThumbCache.get(it) } }?.asImageBitmap()
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

fun Context.findActivity(): Activity {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    error("No activity")
}

/** Human-readable size using 1024-based units, like most file managers and upload portals. */
fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> {
        val kb = bytes / 1024.0
        if (kb < 10) String.format(Locale.getDefault(), "%.1f KB", kb) else String.format(Locale.getDefault(), "%.0f KB", kb)
    }
    else -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
}

/** Label for a limit chosen in the export sheet (limits use 1000-based units). */
fun formatLimit(kb: Int): String =
    if (kb >= 1000 && kb % 1000 == 0) "${kb / 1000} MB" else "$kb KB"

/** "≤ 100 KB", or "20–100 KB" when a minimum is set. */
fun formatRange(minKb: Int?, maxKb: Int): String =
    if (minKb == null) "≤ " + formatLimit(maxKb) else "$minKb–" + formatLimit(maxKb)

/** Opens [file] in another app for a quick look. Returns false when no app can show it. */
fun openFile(context: Context, file: File, format: ExportFormat): Boolean {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, if (format == ExportFormat.PDF) "application/pdf" else "image/jpeg")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

fun shareFiles(context: Context, files: List<File>, format: ExportFormat) {
    val authority = context.packageName + ".files"
    val uris = files.map { FileProvider.getUriForFile(context, authority, it) }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    intent.type = if (format == ExportFormat.PDF) "application/pdf" else "image/jpeg"
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newRawUri("", uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
    context.startActivity(Intent.createChooser(intent, null))
}
