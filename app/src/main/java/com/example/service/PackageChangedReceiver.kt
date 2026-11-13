package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.AppRepository

/**
 * App-list cache invalidation (v1.17.0).
 *
 * The launcher keeps a disk cache of the installed-app list so the drawer opens
 * instantly (see [AppRepository]). When an app is installed, removed or changed,
 * this receiver drops that cache; the next refresh re-scans the package manager
 * and rebuilds it. It only deletes a file — no scan happens here, so the
 * broadcast stays cheap.
 */
class PackageChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED -> {
                AppRepository(context).clearCachedApps()
            }
        }
    }
}
