package com.bitacora.digital.service

import android.content.Context
import com.bitacora.digital.util.Config
import com.bitacora.digital.util.formatBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File system management for sessions and thumbnails.
 */
@Singleton
class StorageService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Sessions directory for video files.
     */
    val sessionsDir: File
        get() = File(context.filesDir, Config.SESSIONS_DIR)

    /**
     * Thumbnails directory for preview images.
     */
    val thumbnailsDir: File
        get() = File(context.filesDir, Config.THUMBNAILS_DIR)

    /**
     * Temporary files directory.
     */
    val tempDir: File
        get() = context.cacheDir

    /**
     * Initialize storage directories.
     */
    fun initialize() {
        sessionsDir.mkdirs()
        thumbnailsDir.mkdirs()
    }

    /**
     * Get session file path for a session ID.
     */
    fun sessionFilePath(sessionId: String): String {
        return File(sessionsDir, "interview_$sessionId.mp4").absolutePath
    }

    /**
     * Get temporary file path.
     */
    fun tempFilePath(prefix: String): String {
        return File(tempDir, "${prefix}_${System.currentTimeMillis()}").absolutePath
    }

    /**
     * Get all session files sorted by date (newest first).
     */
    fun getSessionFiles(): List<File> {
        return sessionsDir.listFiles()
            ?.filter { it.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Delete a session file and its thumbnail.
     */
    fun deleteSession(filename: String) {
        val sessionFile = File(sessionsDir, filename)
        if (sessionFile.exists()) {
            sessionFile.delete()
        }

        // Also delete thumbnail if exists
        val thumbnailFile = File(thumbnailsDir, filename.replace(".mp4", ".jpg"))
        if (thumbnailFile.exists()) {
            thumbnailFile.delete()
        }
    }

    /**
     * Check if a file exists.
     */
    fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    /**
     * Get file size.
     */
    fun fileSize(path: String): Long {
        return File(path).length()
    }

    /**
     * Get available storage space.
     */
    fun availableSpace(): Long {
        return context.filesDir.usableSpace
    }

    /**
     * Get total storage used by sessions.
     */
    fun sessionsStorageUsed(): Long {
        return sessionsDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Format storage used as human-readable string.
     */
    fun formattedStorageUsed(): String {
        return sessionsStorageUsed().formatBytes()
    }

    /**
     * Clean up temporary files.
     */
    fun cleanupTempFiles() {
        tempDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("temp_")) {
                file.delete()
            }
        }
    }

    /**
     * Export a session to external storage.
     */
    fun exportSession(filename: String, destination: File): Boolean {
        val source = File(sessionsDir, filename)
        return try {
            source.copyTo(destination, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
