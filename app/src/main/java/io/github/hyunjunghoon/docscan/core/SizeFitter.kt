package io.github.hyunjunghoon.docscan.core

import kotlin.math.sqrt

/** Encodes one page as JPEG at a relative [scale] (1 = working resolution) and [quality] (0..100). */
interface PageEncoder {
    fun encode(scale: Float, quality: Int): ByteArray
    /** Frees decoded pixels; the encoder may be used again afterwards (it re-decodes). */
    fun release() {}
}

data class FitResult(val bytes: ByteArray, val quality: Int, val scale: Float, val reached: Boolean)

/**
 * Finds the best-looking JPEG that is not larger than a byte budget.
 * Strategy: keep quality >= [readableQuality] and shrink the image first; only when the image
 * reaches [minReadableScale] is quality allowed to drop to [minQuality]. Within a scale, binary
 * search picks the highest quality that fits.
 */
class SizeFitter(
    private val maxQuality: Int = 92,
    private val readableQuality: Int = 55,
    private val minQuality: Int = 12,
    private val minReadableScale: Float = 0.35f,
    private val minScale: Float = 0.02f,
) {
    fun fitSingle(encoder: PageEncoder, budget: Long): FitResult {
        var scale = 1f
        var floor = readableQuality
        var smallest: FitResult? = null
        repeat(MAX_STEPS) {
            val low = encoder.encode(scale, floor)
            if (low.size <= budget) {
                var best = low
                var bestQ = floor
                var lo = floor + 1
                var hi = maxQuality
                while (lo <= hi) {
                    val mid = (lo + hi) ushr 1
                    val enc = encoder.encode(scale, mid)
                    if (enc.size <= budget) { best = enc; bestQ = mid; lo = mid + 1 } else hi = mid - 1
                }
                return FitResult(best, bestQ, scale, true)
            }
            if (smallest == null || low.size < smallest!!.bytes.size) smallest = FitResult(low, floor, scale, false)
            if (floor > minQuality && scale <= minReadableScale) {
                floor = minQuality
                return@repeat
            }
            if (scale <= minScale) return smallest!!
            // JPEG size is roughly proportional to pixel count.
            val factor = (sqrt(budget.toDouble() / low.size) * 0.95).coerceIn(0.25, 0.92).toFloat()
            var next = scale * factor
            if (floor > minQuality) next = maxOf(next, minReadableScale)
            scale = maxOf(next, minScale)
        }
        return smallest!!
    }

    /**
     * Fits several pages into one shared budget. Each page gets a share proportional to its
     * size at a reference setting, so detailed pages keep more bytes than blank ones.
     */
    fun fitMany(
        encoders: List<PageEncoder>,
        budget: Long,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<FitResult> {
        require(encoders.isNotEmpty())
        val weights = encoders.map { e ->
            e.encode(1f, REFERENCE_QUALITY).size.toDouble().also { e.release() }
        }
        val sum = weights.sum()
        return encoders.mapIndexed { i, e ->
            onProgress(i, encoders.size)
            val share = (budget * (weights[i] / sum)).toLong().coerceAtLeast(1)
            fitSingle(e, share).also { e.release() }
        }.also { onProgress(encoders.size, encoders.size) }
    }

    companion object {
        private const val MAX_STEPS = 16
        private const val REFERENCE_QUALITY = 75
    }
}
