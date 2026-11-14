package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DigiBrainItem
import com.example.data.model.DigiBrainKind
import com.example.data.repository.LauncherPreferences
import com.example.ui.theme.FocusLauncherTheme
import com.example.ui.theme.LauncherTheme
import com.example.util.DigiBrain

/**
 * The one-tap save door for Digi Brain (v1.19.0): it answers both the share
 * sheet (ACTION_SEND, text/plain) and the text-selection menu (PROCESS_TEXT —
 * the activity label is what the menu shows). No new permission, no ViewModel:
 * it writes straight to [LauncherPreferences] and finishes.
 *
 * A link saved before is not saved twice: the duplicate warning offers the
 * item that already exists, opening the launcher on it.
 */
class DigiBrainCaptureActivity : ComponentActivity() {

    private lateinit var preferences: LauncherPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = LauncherPreferences(application)

        val shared = readSharedText(intent)
        if (shared == null) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val (rawText, subject) = shared
        val url = DigiBrain.extractUrl(rawText)
            ?: if (DigiBrain.looksLikeUrl(rawText)) rawText.trim() else null
        val kind = if (url != null) DigiBrainKind.LINK else DigiBrainKind.NOTE
        val defaultTitle = if (kind == DigiBrainKind.LINK) {
            subject?.trim().takeUnless { it.isNullOrBlank() }
                ?: DigiBrain.domainOf(url!!)
        } else {
            DigiBrain.titleFromText(rawText)
        }
        val existing = if (url != null) preferences.findDigiBrainByUrl(url) else null

        setContent {
            val isDarkTheme by preferences.isNightMode.collectAsState()
            val themeKey by preferences.themeKey.collectAsState()

            FocusLauncherTheme(darkTheme = isDarkTheme, theme = LauncherTheme.fromKey(themeKey)) {
                var title by remember { mutableStateOf(defaultTitle) }
                var isPublic by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .testTag("digi_brain_capture")
                ) {
                    Text(
                        text = "Save to Digi Brain",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (kind == DigiBrainKind.LINK) "Link" else "Note",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (existing != null) {
                        // Duplicate: stop, and offer the one that already exists.
                        Text(
                            text = "This link is already saved as " +
                                "\"${existing.title.ifBlank { existing.content }}\".",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val open = Intent(this@DigiBrainCaptureActivity, MainActivity::class.java).apply {
                                    putExtra(EXTRA_DIGI_BRAIN_FOCUS_ID, existing.id)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                startActivity(open)
                                finish()
                            },
                            modifier = Modifier.testTag("digi_brain_capture_view_existing")
                        ) {
                            Text("View saved item")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { finish() }) { Text("Close") }
                        return@Column
                    }

                    Text(
                        text = if (kind == DigiBrainKind.LINK) url!! else rawText.trim(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        maxLines = 6
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("digi_brain_capture_title"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { isPublic = !isPublic }
                            .testTag("digi_brain_capture_public")
                    ) {
                        Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                        Text("Show on widget", fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val item = if (kind == DigiBrainKind.LINK) {
                                    DigiBrainItem(
                                        kind = DigiBrainKind.LINK,
                                        title = title.trim().ifBlank { DigiBrain.domainOf(url!!) },
                                        content = DigiBrain.normalizeUrl(url!!),
                                        isPublic = isPublic
                                    )
                                } else {
                                    DigiBrainItem(
                                        kind = DigiBrainKind.NOTE,
                                        title = title.trim().ifBlank { DigiBrain.titleFromText(rawText) },
                                        content = rawText.trim(),
                                        isPublic = isPublic
                                    )
                                }
                                preferences.addDigiBrainItem(item)
                                Toast.makeText(
                                    this@DigiBrainCaptureActivity,
                                    "Saved to Digi Brain",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            },
                            modifier = Modifier.testTag("digi_brain_capture_save")
                        ) {
                            Text("Save")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(onClick = { finish() }) { Text("Cancel") }
                    }
                }
            }
        }
    }

    /**
     * The shared text and an optional title, from either entry point:
     * ACTION_SEND carries EXTRA_TEXT (+ EXTRA_SUBJECT), PROCESS_TEXT carries
     * EXTRA_PROCESS_TEXT. Null when there is nothing to save.
     */
    private fun readSharedText(intent: Intent?): Pair<String, String?>? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                if (text.isNullOrBlank()) null
                else text to intent.getStringExtra(Intent.EXTRA_SUBJECT)
            }
            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.trim()
                if (text.isNullOrBlank()) null else text to null
            }
            else -> null
        }
    }

    companion object {
        /** MainActivity extra: open Digi Brain on this item's detail (duplicate flow). */
        const val EXTRA_DIGI_BRAIN_FOCUS_ID = "digi_brain_focus_id"
    }
}
