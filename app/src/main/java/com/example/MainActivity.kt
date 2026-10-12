package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.LauncherViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.FocusLauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            val isDarkTheme by viewModel.isNightMode.collectAsState()

            FocusLauncherTheme(darkTheme = isDarkTheme) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApps()
        viewModel.refreshUsageStats()
    }

    /**
     * On Android 13+ (API 33+) notifications cannot be shown unless the user grants
     * POST_NOTIFICATIONS. The Focus Mode timer and app-limits notification rely on
     * this, so ask for it once when the launcher opens.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
            }
        }
    }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 1001
    }
}
