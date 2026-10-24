package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.repository.LauncherPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a backup JSON into the folder the user picked (Storage Access Framework), keeping the
 * last 7 daily files. Only the folder grant is needed - no storage permission.
 */
object AutoBackup {

    private const val PREFIX = "sp-launcher-backup-"
    private const val KEEP_DAYS = 7

    fun perform(context: Context, prefs: LauncherPreferences): Boolean {
        val treeString = prefs.backupFolderUri.value ?: return false
        return try {
            val treeUri = Uri.parse(treeString)
            val resolver = context.contentResolver
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)

            val children = mutableListOf<Pair<String, String>>() // documentId to displayName
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) children.add(c.getString(0) to c.getString(1))
            }

            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = fmt.format(Date())
            val keepFrom = System.currentTimeMillis() - KEEP_DAYS * 24L * 60 * 60 * 1000L

            for ((id, name) in children) {
                if (!name.startsWith(PREFIX) || !name.endsWith(".json")) continue
                val day = name.removePrefix(PREFIX).removeSuffix(".json")
                val isToday = day == today
                val old = fmt.parse(day)?.time?.let { it < keepFrom } ?: false
                if (isToday || old) {
                    try {
                        DocumentsContract.deleteDocument(
                            resolver,
                            DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        )
                    } catch (_: Exception) {
                    }
                }
            }

            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val docUri = DocumentsContract.createDocument(
                resolver, parentUri, "application/json", "$PREFIX$today.json"
            ) ?: return false

            resolver.openOutputStream(docUri, "wt")?.use { out ->
                out.write(prefs.exportAllData().toByteArray())
                out.flush()
            } ?: return false

            prefs.setLastBackupMillis(System.currentTimeMillis())
            true
        } catch (_: Exception) {
            false
        }
    }
}
