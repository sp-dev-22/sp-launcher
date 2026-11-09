package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * One shared answer to "can auto-backup actually write to the chosen folder?"
 * (v1.13). Before this existed nothing checked, so a restored phone showed
 * Auto-backup ON while every daily backup silently failed: a restore brings back
 * the folder's Uri string (it is a preference) but NOT the folder permission —
 * that grant is system state, created only by the folder picker — and a folder
 * deleted later fails the same silent way.
 */
object BackupSupport {

    /**
     * The folder is usable only when all three hold: a folder Uri is stored, the
     * system still holds a write grant for exactly that Uri, and the folder still
     * exists. Touches the content resolver — call off the main thread.
     */
    fun isFolderValid(context: Context, folderUri: String?): Boolean {
        if (folderUri.isNullOrBlank()) return false
        val uri = try {
            Uri.parse(folderUri)
        } catch (_: Exception) {
            return false
        }
        val granted = context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        if (!granted) return false
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                uri, DocumentsContract.getTreeDocumentId(uri)
            )
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null
            )?.use { it.moveToFirst() } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
