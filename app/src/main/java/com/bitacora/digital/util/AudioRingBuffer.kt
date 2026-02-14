package com.bitacora.digital.util

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Circular buffer for storing recent audio samples.
 * Used to provide audio context to AI when speech recognition fails.
 */
class AudioRingBuffer(
    seconds: Int = Config.RING_BUFFER_SECONDS,
    private val sampleRate: Int = Config.AUDIO_SAMPLE_RATE,
    private val channels: Int = Config.AUDIO_CHANNELS
) {
    private val capacity = seconds * sampleRate * channels
    private val buffer = FloatArray(capacity)
    private var writePosition = 0
    private var totalSamplesWritten = 0L

    /**
     * Append audio samples to the buffer.
     */
    @Synchronized
    fun append(samples: FloatArray) {
        for (sample in samples) {
            buffer[writePosition] = sample
            writePosition = (writePosition + 1) % capacity
            totalSamplesWritten++
        }
    }

    /**
     * Get recent audio samples.
     */
    @Synchronized
    fun getRecentAudio(seconds: Int): FloatArray {
        val samplesToGet = min(seconds * sampleRate * channels, capacity)
        val availableSamples = min(totalSamplesWritten.toInt(), capacity)
        val actualSamples = min(samplesToGet, availableSamples)

        if (actualSamples == 0) return FloatArray(0)

        val result = FloatArray(actualSamples)
        var readPosition = (writePosition - actualSamples + capacity) % capacity

        for (i in 0 until actualSamples) {
            result[i] = buffer[readPosition]
            readPosition = (readPosition + 1) % capacity
        }

        return result
    }

    /**
     * Clear the buffer.
     */
    @Synchronized
    fun clear() {
        buffer.fill(0f)
        writePosition = 0
        totalSamplesWritten = 0
    }

    /**
     * Export recent audio as WAV bytes.
     */
    fun exportToWAV(seconds: Int): ByteArray {
        val samples = getRecentAudio(seconds)
        if (samples.isEmpty()) return ByteArray(0)

        val byteBuffer = ByteArrayOutputStream()

        // WAV header
        val dataSize = samples.size * 2 // 16-bit samples
        val fileSize = 36 + dataSize

        // RIFF header
        byteBuffer.write("RIFF".toByteArray())
        byteBuffer.write(intToLittleEndianBytes(fileSize))
        byteBuffer.write("WAVE".toByteArray())

        // fmt subchunk
        byteBuffer.write("fmt ".toByteArray())
        byteBuffer.write(intToLittleEndianBytes(16)) // Subchunk1Size (PCM)
        byteBuffer.write(shortToLittleEndianBytes(1)) // AudioFormat (PCM)
        byteBuffer.write(shortToLittleEndianBytes(channels.toShort()))
        byteBuffer.write(intToLittleEndianBytes(sampleRate))
        byteBuffer.write(intToLittleEndianBytes(sampleRate * channels * 2)) // ByteRate
        byteBuffer.write(shortToLittleEndianBytes((channels * 2).toShort())) // BlockAlign
        byteBuffer.write(shortToLittleEndianBytes(16)) // BitsPerSample

        // data subchunk
        byteBuffer.write("data".toByteArray())
        byteBuffer.write(intToLittleEndianBytes(dataSize))

        // Convert float samples to 16-bit PCM
        for (sample in samples) {
            val clampedSample = sample.coerceIn(-1f, 1f)
            val shortSample = (clampedSample * 32767).toInt().toShort()
            byteBuffer.write(shortToLittleEndianBytes(shortSample))
        }

        return byteBuffer.toByteArray()
    }

    /**
     * Export recent audio as Base64-encoded WAV.
     */
    fun exportToBase64(seconds: Int): String {
        val wavBytes = exportToWAV(seconds)
        return if (wavBytes.isNotEmpty()) {
            Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        } else {
            ""
        }
    }

    /**
     * Get buffer statistics.
     */
    fun getStats(): BufferStats {
        val filledSamples = min(totalSamplesWritten.toInt(), capacity)
        val filledSeconds = filledSamples.toFloat() / (sampleRate * channels)
        val capacitySeconds = capacity.toFloat() / (sampleRate * channels)

        return BufferStats(
            filledSamples = filledSamples,
            filledSeconds = filledSeconds,
            capacitySeconds = capacitySeconds,
            fillPercentage = (filledSamples.toFloat() / capacity) * 100
        )
    }

    private fun intToLittleEndianBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(value)
            .array()
    }

    private fun shortToLittleEndianBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(value)
            .array()
    }
}

/**
 * Buffer statistics.
 */
data class BufferStats(
    val filledSamples: Int,
    val filledSeconds: Float,
    val capacitySeconds: Float,
    val fillPercentage: Float
)
