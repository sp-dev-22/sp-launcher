package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.ContactsContract
import android.provider.MediaStore
import com.example.data.model.ContactEntry
import com.example.data.model.DeviceFile
import com.example.util.PhoneNumbers

/**
 * Where the SP Assistant (v1.12) reads the phone itself: the address book and the
 * media store. Both are only ever called after the matching runtime permission has
 * been granted (the search screen asks lazily, never up front), and both run on a
 * background thread — the ViewModel sees to that.
 */
object AssistantRepository {

    /** Every saved (name, number) pair, sorted by name, duplicates removed. */
    fun loadContacts(context: Context): List<ContactEntry> {
        val out = mutableListOf<ContactEntry>()
        val seen = mutableSetOf<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol)?.trim().orEmpty()
                val number = cursor.getString(numberCol)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val key = name.lowercase() + "|" + PhoneNumbers.digits(number)
                if (!seen.add(key)) continue
                val id = if (idCol >= 0) cursor.getLong(idCol).toString() else key
                out.add(ContactEntry(id = id, name = if (name.isEmpty()) number else name, number = number))
            }
        }
        return out
    }

    /** Files (photos, videos, audio, documents) whose name contains [query], newest first. */
    fun searchFiles(context: Context, query: String, limit: Int = 30): List<DeviceFile> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val out = mutableListOf<DeviceFile>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE
        )
        val selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?"
        val args = arrayOf("%$q%")
        context.contentResolver.query(
            collection, projection, selection, args,
            MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            while (cursor.moveToNext() && out.size < limit) {
                val name = cursor.getString(nameCol) ?: continue
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                out.add(
                    DeviceFile(
                        name = name,
                        uri = uri.toString(),
                        mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) else null,
                        sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    )
                )
            }
        }
        return out
    }
}
