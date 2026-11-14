package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Share an installed app's APK through the system share sheet (v1.18.0).
 *
 * The APK lives under /data/app, which the FileProvider cannot serve directly,
 * so it is copied into the cache first (the copy is a file operation — call
 * from a background thread) and shared from there. No new permission: the
 * FileProvider is the one the files search already uses, with a cache path
 * added for the shared copies.
 */
object ShareApp {

    /**
     * Copies [packageName]'s APK to the cache and opens the share sheet.
     * Returns false when the APK cannot be read or the share cannot start.
     */
    suspend fun share(context: Context, packageName: String, appLabel: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val source = File(appInfo.sourceDir)
            val dir = File(context.cacheDir, "shared_apks").apply { mkdirs() }
            val dest = File(dir, "$packageName.apk")
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                dest
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share $appLabel").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }
}
