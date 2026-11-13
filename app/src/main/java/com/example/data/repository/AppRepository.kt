package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.absoluteValue

class AppRepository(private val context: Context) {

    private val packageManager = context.packageManager

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val activities = packageManager.queryIntentActivities(launcherIntent, 0)
        val appList = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in activities) {
            val pkg = resolveInfo.activityInfo.packageName
            // Exclude the launcher itself to prevent recursive launcher loop
            if (pkg == context.packageName) continue
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val label = try {
                resolveInfo.loadLabel(packageManager).toString()
            } catch (_: Exception) {
                resolveInfo.activityInfo.name
            }

            val activityName = resolveInfo.activityInfo.name
            val colorIndex = (label.hashCode().absoluteValue) % 10

            appList.add(
                AppInfo(
                    packageName = pkg,
                    activityName = activityName,
                    label = label,
                    colorIndex = colorIndex
                )
            )
        }

        appList.sortedBy { it.label.lowercase() }
    }

    // ---- App-list disk cache (v1.17.0, fast boot) ----
    //
    // The full package-manager scan takes a noticeable moment on a low-RAM phone,
    // and the drawer used to wait for it on every cold start. The identity fields
    // (package, activity, label, colour) are cached as JSON in a private file;
    // icons are NOT cached — they still load on demand via RealAppIcon. The
    // cache is regenerable, so it is deliberately NOT part of the user backup.
    // PackageChangedReceiver deletes it whenever an app is installed, removed
    // or changed, so a stale list can only survive until the next refresh.
    // Every cache call is failure-proof: a corrupt or unreadable cache just
    // behaves like no cache at all.

    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    /** True when a cache file exists (a cheap stat call; safe on the main thread). */
    fun hasCachedApps(): Boolean = try {
        cacheFile.exists()
    } catch (_: Exception) {
        false
    }

    suspend fun loadCachedApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val raw = cacheFile.readText()
            val array = JSONArray(raw)
            val apps = mutableListOf<AppInfo>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val pkg = item.optString(KEY_PKG)
                if (pkg.isBlank()) continue
                apps.add(
                    AppInfo(
                        packageName = pkg,
                        activityName = item.optString(KEY_ACTIVITY),
                        label = item.optString(KEY_LABEL, pkg),
                        colorIndex = item.optInt(KEY_COLOR, 0)
                    )
                )
            }
            apps.sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveCachedApps(apps: List<AppInfo>) {
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                for (app in apps) {
                    array.put(
                        JSONObject().apply {
                            put(KEY_PKG, app.packageName)
                            put(KEY_ACTIVITY, app.activityName)
                            put(KEY_LABEL, app.label)
                            put(KEY_COLOR, app.colorIndex)
                        }
                    )
                }
                cacheFile.writeText(array.toString())
            } catch (_: Exception) {
                // The cache is only a speedup; a failed write changes nothing.
            }
        }
    }

    /** Drops the cache (called by PackageChangedReceiver). Safe anywhere. */
    fun clearCachedApps() {
        try {
            if (cacheFile.exists()) cacheFile.delete()
        } catch (_: Exception) {
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Opens the system uninstall confirmation for [packageName].
     *
     * On Android 11+ (API 30+) launching this intent requires the
     * REQUEST_DELETE_PACKAGES permission (declared in the manifest); without it the
     * system silently refuses to show the dialog, which is why "delete" used to do
     * nothing. ACTION_DELETE is tried first, with ACTION_UNINSTALL_PACKAGE as a fallback.
     */
    fun uninstallApp(packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        val primary = Intent(Intent.ACTION_DELETE, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(primary)
            return
        } catch (_: Exception) {
            // Fall through to the alternative intent below.
        }
        try {
            val fallback = Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }

    companion object {
        private const val CACHE_FILE_NAME = "app_list_cache.json"
        private const val KEY_PKG = "pkg"
        private const val KEY_ACTIVITY = "act"
        private const val KEY_LABEL = "label"
        private const val KEY_COLOR = "color"
    }
}
