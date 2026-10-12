package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

}
