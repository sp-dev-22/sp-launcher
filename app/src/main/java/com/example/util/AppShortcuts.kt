package com.example.util

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * App shortcuts for the drawer long-press sheet (v1.18.0).
 *
 * An app's own shortcuts (static ones from its manifest plus the dynamic ones
 * it publishes, e.g. WhatsApp's Camera / New chat) are read through
 * LauncherApps — no permission needed, and no new dependency. Shortcuts need
 * API 25+, so below that the sheet simply shows no section.
 */
object AppShortcuts {

    /** Shortcuts exist from Android 7.1 (API 25) onward. */
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    /** The app's enabled shortcuts, static + dynamic. Empty when there are none. */
    fun getShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
        if (!isSupported()) return emptyList()
        return try {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
                ?: return emptyList()
            val query = ShortcutQuery()
                .setPackage(packageName)
                .setQueryFlags(
                    ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_MANIFEST
                )
            launcherApps.getShortcuts(query, Process.myUserHandle())
                ?.filter { it.isEnabled && !it.shortLabel.isNullOrBlank() }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Launches a shortcut. False when the system refuses (e.g. it vanished). */
    fun launchShortcut(context: Context, shortcut: ShortcutInfo): Boolean {
        return try {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
                ?: return false
            launcherApps.startShortcut(shortcut, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** The shortcut's icon as a bitmap, or null when it has none. */
    fun shortcutIcon(context: Context, shortcut: ShortcutInfo, sizePx: Int): ImageBitmap? {
        return try {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
                ?: return null
            val drawable: Drawable = launcherApps.getShortcutIconDrawable(
                shortcut,
                context.resources.displayMetrics.densityDpi
            ) ?: return null
            // A drawable with no intrinsic size would make toBitmap() throw.
            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else sizePx
            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else sizePx
            drawable.toBitmap(width = w, height = h).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
