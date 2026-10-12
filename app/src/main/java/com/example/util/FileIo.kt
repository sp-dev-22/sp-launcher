package com.example.util

import android.content.Context
import android.net.Uri

/** Tiny helpers for reading/writing text and bytes through a Storage Access Framework [Uri]. */
object FileIo {

    fun writeText(context: Context, uri: Uri, text: String): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        true
    } catch (_: Exception) {
        false
    }

    fun writeBytes(context: Context, uri: Uri, bytes: ByteArray): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        true
    } catch (_: Exception) {
        false
    }

    fun readText(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (_: Exception) {
        null
    }
}
