package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.AppRepository
import com.example.data.repository.LauncherPreferences
import com.example.ui.components.AppIconLoader

/**
 * App-list cache invalidation (v1.17.0) + favourites pruning (v1.18.0).
 *
 * The launcher keeps a disk cache of the installed-app list so the drawer opens
 * instantly (see [AppRepository]). When an app is installed, removed or changed,
 * this receiver drops that cache; the next refresh re-scans the package manager
 * and rebuilds it. It only deletes a file — no scan happens here, so the
 * broadcast stays cheap.
 *
 * On a removal it also prunes the package from the favourites groups and the
 * favourites bar (v1.18.0): an uninstalled app used to linger in a group as a
 * ghost entry the editor counted but could never show or untick. An update is
 * not a removal — PACKAGE_REMOVED fires for those too, with EXTRA_REPLACING —
 * so updates only clear the cache.
 */
class PackageChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_CHANGED -> {
                AppRepository(context).clearCachedApps()
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                AppRepository(context).clearCachedApps()
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
                val packageName = intent.data?.schemeSpecificPart ?: return
                LauncherPreferences(context).pruneUninstalledPackage(packageName)
                AppIconLoader.evict(packageName)
            }
        }
    }
}
