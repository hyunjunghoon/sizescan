package io.github.hyunjunghoon.docscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import io.github.hyunjunghoon.docscan.core.PageEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

object Images {
    private fun sampleSize(w: Int, h: Int, maxLongSide: Int): Int {
        var s = 1
        while (max(w, h) / (s * 2) >= maxLongSide) s *= 2
        return s
    }

    private fun scaleDown(src: Bitmap, maxLongSide: Int): Bitmap {
        val long = max(src.width, src.height)
        if (long <= maxLongSide) return src
        val f = maxLongSide.toFloat() / long
        val out = Bitmap.createScaledBitmap(src, max(1, (src.width * f).roundToInt()), max(1, (src.height * f).roundToInt()), true)
        if (out !== src) src.recycle()
        return out
    }

    /** Decodes [file] so that its long side is at most [maxLongSide] pixels. */
    fun decodeSampled(file: File, maxLongSide: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxLongSide) }
        val bmp = BitmapFactory.decodeFile(file.path, opts) ?: throw IOException("Cannot decode ${file.name}")
        return scaleDown(bmp, maxLongSide)
    }

    /** Copies any picked image into an upright, opaque JPEG (EXIF rotation applied, alpha on white). */
    fun normalizeToJpeg(context: Context, uri: Uri, out: File, maxLongSide: Int = 4000) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: throw IOException("Cannot open")
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxLongSide) }
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IOException("Cannot decode")
        val exif = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it) } }.getOrNull()
        val rotation = exif?.rotationDegrees ?: 0
        val flipped = exif?.isFlipped ?: false
        if (rotation != 0 || flipped) {
            val m = Matrix().apply {
                if (flipped) postScale(-1f, 1f)
                postRotate(rotation.toFloat())
            }
            val r = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (r !== bmp) bmp.recycle()
            bmp = r
        }
        bmp = scaleDown(bmp, maxLongSide)
        if (bmp.hasAlpha()) {
            val opaque = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(bmp, 0f, 0f, null) }
            bmp.recycle()
            bmp = opaque
        }
        try {
            out.outputStream().buffered().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        } finally {
            bmp.recycle()
        }
    }
}

/** Small in-memory thumbnail cache keyed by path + modification time. */
object ThumbCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun get(file: File): Bitmap? {
        val key = "${file.path}:${file.lastModified()}"
        cache.get(key)?.let { return it }
        return runCatching { Images.decodeSampled(file, 400) }.getOrNull()?.also { cache.put(key, it) }
    }
}

/** Encodes a stored page at a working resolution of at most [maxLongSide] pixels. */
class AndroidPageEncoder(private val file: File, private val maxLongSide: Int = 2480) : PageEncoder {
    private var base: Bitmap? = null
    private var scaled: Bitmap? = null
    private var scaledFor = -1f

    override fun encode(scale: Float, quality: Int): ByteArray {
        val b = base ?: Images.decodeSampled(file, maxLongSide).also { base = it }
        val bmp = if (scale >= 0.999f) b else {
            if (scaledFor != scale) {
                scaled?.let { if (it !== b) it.recycle() }
                scaled = Bitmap.createScaledBitmap(b, max(1, (b.width * scale).roundToInt()), max(1, (b.height * scale).roundToInt()), true)
                scaledFor = scale
            }
            scaled!!
        }
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
    }

    override fun release() {
        scaled?.let { if (it !== base) it.recycle() }
        base?.recycle()
        scaled = null
        base = null
        scaledFor = -1f
    }
}
