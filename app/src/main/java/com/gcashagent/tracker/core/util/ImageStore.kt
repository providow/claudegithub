package com.gcashagent.tracker.core.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Persists transaction screenshots into app-internal storage
 * (filesDir/screenshots) so they survive even if the original gallery image is
 * deleted. Stores and returns absolute file paths.
 */
class ImageStore(private val context: Context) {

    private val dir: File by lazy {
        File(context.filesDir, "screenshots").apply { mkdirs() }
    }

    /** Copy [uri] into internal storage; returns the saved absolute path, or null on failure. */
    fun saveFromUri(uri: Uri): String? = runCatching {
        val target = File(dir, "shot_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
