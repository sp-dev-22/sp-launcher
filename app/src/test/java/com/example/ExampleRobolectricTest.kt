package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BannerItem
import com.example.data.repository.LauncherPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SP Launcher", appName)
    }

    @Test
    fun `preferences stores and toggles night mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LauncherPreferences(context)
        prefs.setNightMode(true)
        assertTrue(prefs.isNightMode.value)
        prefs.setNightMode(false)
        assertFalse(prefs.isNightMode.value)
    }

    @Test
    fun `preferences stores tasks and toggles completion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LauncherPreferences(context)
        prefs.addTask("Test Robolectric Task")
        val task = prefs.tasks.value.first { it.text == "Test Robolectric Task" }
        assertFalse(task.isCompleted)
        prefs.toggleTask(task.id)
        val updated = prefs.tasks.value.first { it.id == task.id }
        assertTrue(updated.isCompleted)
    }

    @Test
    fun `preferences manages banners`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LauncherPreferences(context)
        val banner = BannerItem(title = "Test Banner", content = "Test Content", category = "Goal")
        prefs.addBanner(banner)
        val found = prefs.banners.value.any { it.title == "Test Banner" }
        assertTrue(found)
    }

    @Test
    fun `preferences manages focus mode settings and allowed apps`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LauncherPreferences(context)
        prefs.setFocusModeActive(true)
        assertTrue(prefs.isFocusModeActive.value)
        prefs.setStudyDurationMinutes(30)
        assertEquals(30, prefs.studyDurationMinutes.value)
        prefs.setBreakDurationMinutes(10)
        assertEquals(10, prefs.breakDurationMinutes.value)

        // Break phase toggle
        prefs.setFocusBreakPhase(true)
        assertTrue(prefs.isFocusBreakPhase.value)
        prefs.setFocusBreakPhase(false)
        assertFalse(prefs.isFocusBreakPhase.value)

        // Allowed apps test
        prefs.setAllowedFocusApps(listOf("com.test.allowed"))
        assertTrue(prefs.allowedFocusApps.value.contains("com.test.allowed"))
        prefs.toggleAllowedFocusApp("com.test.allowed")
        assertFalse(prefs.allowedFocusApps.value.contains("com.test.allowed"))

        prefs.setFocusModeActive(false)
        assertFalse(prefs.isFocusModeActive.value)
    }

    @Test
    fun `preferences records sessions and app limits`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LauncherPreferences(context)

        prefs.setAppLimit("com.social.app", 30)
        assertEquals(30, prefs.appLimits.value["com.social.app"])

        // Test completed session records
        prefs.recordCompletedFocusSession(25)
        assertTrue(prefs.completedSessionsCount.value >= 1)
    }

    @Test
    fun `preferences manages focus history and custom clear`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LauncherPreferences(context)

        prefs.clearAllFocusHistory()
        assertEquals(0, prefs.focusHistory.value.size)

        prefs.recordCompletedFocusSession(25, "Study Session")
        assertEquals(1, prefs.focusHistory.value.size)
        assertEquals(25, prefs.focusHistory.value.first().durationMinutes)

        // Clear before future timestamp (clears all)
        val future = System.currentTimeMillis() + 100000L
        prefs.clearFocusHistoryBefore(future)
        assertEquals(0, prefs.focusHistory.value.size)
    }

    @Test
    fun `preferences toggles notes widget visibility`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LauncherPreferences(context)
        prefs.setShowNotes(false)
        assertFalse(prefs.showNotes.value)
        prefs.setShowNotes(true)
        assertTrue(prefs.showNotes.value)
    }
}
