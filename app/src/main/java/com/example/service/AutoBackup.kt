package com.example.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes a timestamped backup file into a user-chosen SAF folder. */
object AutoBackup {

    fun write(context: Context, treeUriString: String, json: String): Boolean {
        if (treeUriString.isBlank()) return false
        val treeUri = Uri.parse(treeUriString)
        val name = "sp-launcher-backup-" +
            SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date()) + ".json"
        return try {
            val docUri = DocumentsContract.createDocument(
                context.contentResolver, treeUri, "application/json", name
            ) ?: return false
            context.contentResolver.openOutputStream(docUri)?.use {
                it.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
