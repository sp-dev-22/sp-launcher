package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.model.ContactEntry
import com.example.data.model.DeviceFile
import com.example.util.PhoneNumbers
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

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

    /**
     * True when the user has granted All Files Access (Android 11+). This is the
     * switch in system settings — there is no runtime prompt for it.
     */
    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()

    /**
     * The v1.13 on-demand directory walk: starting at the root of shared storage,
     * recursively visits folders (skipping `Android/data`, `Android/obb` and hidden
     * dot-folders, which are either blocked or noise) and reports every file whose
     * name contains [query]. Nothing is indexed — the walk runs fresh for each
     * search, and matches are handed to [onBatch] as they are found so the UI can
     * show them streaming in. Stops at [limit] matches. Must be called from a
     * background dispatcher; cancellation of the calling coroutine stops the walk.
     */
    suspend fun walkFiles(
        context: Context,
        query: String,
        limit: Int,
        onBatch: (List<DeviceFile>) -> Unit
    ) {
        val q = query.trim().lowercase()
        if (q.length < 2 || !hasAllFilesAccess()) return
        val root = Environment.getExternalStorageDirectory() ?: return
        val authority = context.packageName + ".fileprovider"
        val found = mutableListOf<DeviceFile>()
        val seenPaths = mutableSetOf<String>()
        var sinceCheck = 0
        val walker = root.walkTopDown()
            .onEnter { dir ->
                // Prune: never descend into hidden folders, or into the Android/data
                // and Android/obb sandboxes (blocked even with All Files Access).
                if (dir == root) return@onEnter true
                if (dir.name.startsWith(".")) return@onEnter false
                val underAndroid = dir.parentFile?.name == "Android"
                !(underAndroid && dir.name in SKIP_DIRS)
            }
            .onFail { _, _ -> kotlin.io.OnErrorAction.SKIP }
        for (file in walker) {
            if (++sinceCheck >= 200) {
                sinceCheck = 0
                currentCoroutineContext().ensureActive()
            }
            if (!file.isFile) continue
            if (!file.name.lowercase().contains(q)) continue
            val path = file.absolutePath
            if (!seenPaths.add(path)) continue
            val mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension.lowercase())
            val uri = try {
                FileProvider.getUriForFile(context, authority, file).toString()
            } catch (_: Exception) {
                continue // outside the provider's roots — cannot be opened, skip it
            }
            found.add(
                DeviceFile(
                    name = file.name,
                    uri = uri,
                    mimeType = mime,
                    sizeBytes = file.length(),
                    path = path
                )
            )
            onBatch(found.toList())
            if (found.size >= limit) return
        }
    }

    private val SKIP_DIRS = setOf("data", "obb")
}
