package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.LauncherViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.FocusLauncherTheme
import com.example.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    // Registered before the activity is created, as the Activity Result API requires.
    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* The system switches the home app itself when the role is granted. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            val isDarkTheme by viewModel.isNightMode.collectAsState()
            val themeKey by viewModel.themeKey.collectAsState()

            FocusLauncherTheme(darkTheme = isDarkTheme, theme = LauncherTheme.fromKey(themeKey)) {
                MainScreen(
                    viewModel = viewModel,
                    onSetDefaultLauncher = { requestDefaultLauncher() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApps()
    }

    /**
     * Asks the system to make SP Launcher the default home app.
     *
     * On Android 10+ the role request must be launched from an Activity (the system returns
     * the outcome through the Activity result). Starting it from an application context does
     * not work, which is why the option used to do nothing.
     */
    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                try {
                    homeRoleLauncher.launch(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    )
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "Role request failed; falling back to system settings", e)
                }
            }
        }
        // Older Android, or the role is unavailable: open the system screen instead.
        openHomeSettings()
    }

    private fun openHomeSettings() {
        val candidates = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(packageManager) == null) continue
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Could not open ${intent.action}", e)
            }
        }
        Log.w(TAG, "No system screen available to change the default home app")
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
        private const val TAG = "MainActivity"
    }
}
