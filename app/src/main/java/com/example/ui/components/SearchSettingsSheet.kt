package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel

/**
 * Search Settings (v1.12): which sources the SP Assistant searches, from the search
 * box's three-dot menu. Every source defaults to ON. Turning one off only stops that
 * source from appearing — the Contacts and Files permissions are still asked lazily,
 * the first time a search actually needs them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsSheet(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.searchContactsEnabled.collectAsState()
    val files by viewModel.searchFilesEnabled.collectAsState()
    val calc by viewModel.searchCalcEnabled.collectAsState()
    val web by viewModel.searchWebEnabled.collectAsState()
    val voice by viewModel.searchVoiceEnabled.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
        ) {
            Text(
                "Search Settings",
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Assistant किन-किन जगहों पर खोजे. Permission सिर्फ़ तब माँगा जायेगा जब किसी खोज को उसकी सच में ज़रूरत होगी.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            SettingRow(
                title = "Contacts",
                subtitle = "नाम या number से खोजो — Call, SMS, WhatsApp बटन के साथ",
                checked = contacts,
                testTag = "search_setting_contacts"
            ) { viewModel.setSearchContactsEnabled(it) }
            SettingRow(
                title = "Files",
                subtitle = "Photos, videos, audio और documents नाम से खोजो",
                checked = files,
                testTag = "search_setting_files"
            ) { viewModel.setSearchFilesEnabled(it) }
            SettingRow(
                title = "Calculator",
                subtitle = "हिसाब और unit बदलना — 45*12, 5 km in m",
                checked = calc,
                testTag = "search_setting_calc"
            ) { viewModel.setSearchCalcEnabled(it) }
            SettingRow(
                title = "Web shortcuts",
                subtitle = "हर खोज के नीचे Google और YouTube पर खोजने के रास्ते",
                checked = web,
                testTag = "search_setting_web"
            ) { viewModel.setSearchWebEnabled(it) }
            SettingRow(
                title = "Voice input",
                subtitle = "Search box में mic बटन — बोलकर खोजो",
                checked = voice,
                testTag = "search_setting_voice"
            ) { viewModel.setSearchVoiceEnabled(it) }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
