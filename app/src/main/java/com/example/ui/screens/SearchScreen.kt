package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactEntry
import com.example.data.model.DeviceFile
import com.example.ui.LauncherViewModel
import com.example.ui.components.SearchSettingsSheet
import com.example.util.CalcEngine
import com.example.util.GlobalSearch
import com.example.util.PhoneNumbers
import com.example.util.SearchHit
import com.example.util.SearchKind
import java.util.Locale

/**
 * The SP Assistant (v1.12): one search box over apps, contacts, files, tasks, notes,
 * study classes and cards, with a calculator on top, Call / SMS / WhatsApp actions on
 * people and numbers, Google and YouTube shortcuts at the bottom, and voice input
 * from the mic. The three-dot menu holds Search Settings (which sources take part),
 * Hidden Apps (behind the vault PIN) and Locked Apps; the Contacts and Files
 * permissions are asked only when a search first needs them — never up front.
 */
@Composable
fun SearchScreen(
    viewModel: LauncherViewModel,
    autoStartVoice: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.drawerApps.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val courses by viewModel.studyCourses.collectAsState()
    val cards by viewModel.flashcardBank.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val fileHits by viewModel.fileHits.collectAsState()
    val searchingFiles by viewModel.isSearchingFiles.collectAsState()
    val contactsOn by viewModel.searchContactsEnabled.collectAsState()
    val filesOn by viewModel.searchFilesEnabled.collectAsState()
    val calcOn by viewModel.searchCalcEnabled.collectAsState()
    val webOn by viewModel.searchWebEnabled.collectAsState()
    val voiceOn by viewModel.searchVoiceEnabled.collectAsState()

    var query by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showVaultPin by remember { mutableStateOf(false) }
    var contactsGranted by remember { mutableStateOf(viewModel.hasContactsPermission()) }
    var filesGranted by remember { mutableStateOf(viewModel.hasFilesPermission()) }
    var allFilesAccess by remember { mutableStateOf(viewModel.hasAllFilesAccess()) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsGranted = granted || viewModel.hasContactsPermission()
        if (contactsGranted) viewModel.refreshContacts()
    }
    val filesPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        filesGranted = viewModel.hasFilesPermission()
        if (filesGranted) viewModel.searchFiles(query)
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) query = spoken
    }
    val voiceAvailable = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .resolveActivity(context.packageManager) != null
    }

    // All Files Access can only be granted on a system settings page; coming back
    // from there, re-check it and re-run the search so documents appear at once.
    val allFilesSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        allFilesAccess = viewModel.hasAllFilesAccess()
        if (allFilesAccess) viewModel.searchFiles(query)
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening… speak now")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context, "Voice search is not available on this phone",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Opened from the Home bar's mic: start listening immediately, exactly once.
    LaunchedEffect(autoStartVoice) {
        if (autoStartVoice) {
            viewModel.consumeSearchAutoVoice()
            if (voiceOn && voiceAvailable) startVoice()
        }
    }

    LaunchedEffect(Unit) { viewModel.refreshContacts() }
    LaunchedEffect(query, filesOn, filesGranted, allFilesAccess) {
        if (filesOn && (filesGranted || allFilesAccess)) viewModel.searchFiles(query)
    }

    val hits = remember(query, apps, tasks, alerts, courses, cards, contacts, contactsOn, contactsGranted) {
        GlobalSearch.search(
            query, apps, tasks, alerts, courses, cards,
            contacts = if (contactsOn && contactsGranted) contacts else emptyList()
        )
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val calcResult = remember(query, calcOn) {
        if (calcOn) CalcEngine.evaluate(query) else null
    }
    val numberText = remember(query) {
        if (PhoneNumbers.looksLikeNumber(query)) query.trim() else null
    }
    val showContactsPrompt = contactsOn && !contactsGranted && query.trim().length >= 2
    val showFilesPrompt = filesOn && !filesGranted && query.trim().length >= 2
    // The All Files Access hint (v1.13): media search works without it, but
    // documents are only found once it is granted in system settings.
    val showAllFilesPrompt = filesOn && filesGranted && !allFilesAccess && query.trim().length >= 2
    val nothingFound = hits.isEmpty() && calcResult == null && numberText == null &&
        fileHits.isEmpty() && !showContactsPrompt && !showFilesPrompt && !searchingFiles
    val hasContent = !nothingFound || webOn

    fun closeSearch() = viewModel.setShowSearch(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("search_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps, contacts, files...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (voiceOn && voiceAvailable) {
                    {
                        IconButton(
                            onClick = { startVoice() },
                            modifier = Modifier.testTag("search_mic_btn")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice search")
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("search_input")
            )
            // Three-dot menu (v1.15.0): Search Settings, Hidden Apps (PIN first)
            // and Locked Apps. The old `vault <pin>` / `apps <pin>` commands are gone.
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("search_settings_btn")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Search Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showSettings = true
                        },
                        modifier = Modifier.testTag("search_menu_settings")
                    )
                    DropdownMenuItem(
                        text = { Text("Hidden Apps") },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showVaultPin = true
                        },
                        modifier = Modifier.testTag("search_menu_hidden_apps")
                    )
                    DropdownMenuItem(
                        text = { Text("Locked Apps") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            closeSearch()
                            viewModel.setShowAppLock(true)
                        },
                        modifier = Modifier.testTag("search_menu_locked_apps")
                    )
                }
            }
        }

        when {
            query.isBlank() -> Hint(
                "Type or speak to search apps, contacts, files, tasks, notes, study classes and cards.\n\n" +
                    "Try:   45*12   -   5 km in m   -   a phone number"
            )
            !hasContent -> Hint("No results for \"$query\"")
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // Calculator answer, always first.
                if (calcResult != null) {
                    item(key = "calc") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("result", calcResult)
                                    )
                                    Toast.makeText(context, "Copied: $calcResult", Toast.LENGTH_SHORT)
                                        .show()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = query.trim(),
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                        .copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "= $calcResult",
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Tap to copy",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                        .copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }

                // A bare phone number gets its own action card.
                if (numberText != null) {
                    item(key = "number") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Text(numberText, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                "Actions for this number:",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            ContactActions(context, numberText) { closeSearch() }
                        }
                    }
                }

                // Nothing matched anywhere: say so, above the web shortcuts.
                if (nothingFound) {
                    item(key = "empty") {
                        Text(
                            text = "No results for \"${query.trim()}\"",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)
                        )
                    }
                }

                items(hits, key = { it.kind.name + ":" + it.refId }) { hit ->
                    if (hit.kind == SearchKind.CONTACT) {
                        val entry = contacts.firstOrNull { it.id == hit.refId }
                            ?: ContactEntry(hit.refId, hit.title, hit.subtitle)
                        ContactRow(context, entry) { closeSearch() }
                    } else {
                        SearchRow(hit) {
                            when (hit.kind) {
                                SearchKind.APP -> {
                                    val app = apps.firstOrNull { it.packageName == hit.refId }
                                    closeSearch()
                                    if (app != null) viewModel.launchApp(app)
                                }
                                SearchKind.TASK, SearchKind.NOTE -> {
                                    closeSearch()
                                    viewModel.setShowWidgetsDialog(true)
                                }
                                SearchKind.CLASS -> {
                                    closeSearch()
                                    viewModel.setShowStudyManager(true)
                                }
                                SearchKind.CARD -> {
                                    closeSearch()
                                    viewModel.setShowCards(true)
                                }
                                SearchKind.CONTACT -> Unit
                            }
                        }
                    }
                }

                if (showContactsPrompt) {
                    item(key = "perm_contacts") {
                        PermissionRow(
                            title = "Search contacts too?",
                            subtitle = "Contacts permission is needed once to search by name or number"
                        ) {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                }

                if (fileHits.isNotEmpty()) {
                    item(key = "files_header") {
                        Text(
                            "FILES",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp)
                        )
                    }
                    items(fileHits, key = { it.uri }) { file ->
                        FileRow(file) {
                            closeSearch()
                            openFile(context, file)
                        }
                    }
                }

                // The storage walk is still running: say so where its results land.
                if (searchingFiles) {
                    item(key = "files_searching") {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.components.LoadingIndicator(label = "Searching files…")
                        }
                    }
                }

                if (showAllFilesPrompt) {
                    item(key = "perm_all_files") {
                        PermissionRow(
                            title = "Documents need All Files Access",
                            subtitle = "Photos and videos are searched already. Allow access to all files to also find documents and every other file type.",
                            buttonLabel = "Open settings"
                        ) {
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:" + context.packageName)
                            )
                            try {
                                allFilesSettingsLauncher.launch(intent)
                            } catch (e: ActivityNotFoundException) {
                                startIntent(
                                    context,
                                    Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                )
                            }
                        }
                    }
                }

                if (showFilesPrompt) {
                    item(key = "perm_files") {
                        PermissionRow(
                            title = "Search files too?",
                            subtitle = "Storage permission is needed once to search photos, videos and documents"
                        ) {
                            val perms = if (Build.VERSION.SDK_INT >= 33) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_IMAGES,
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    Manifest.permission.READ_MEDIA_AUDIO
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            filesPermissionLauncher.launch(perms)
                        }
                    }
                }

                if (webOn) {
                    item(key = "web_google") {
                        WebRow(Icons.Default.Search, "Search on Google: \"${query.trim()}\"") {
                            closeSearch()
                            startIntent(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "https://www.google.com/search?q=" +
                                            Uri.encode(query.trim())
                                    )
                                )
                            )
                        }
                    }
                    item(key = "web_youtube") {
                        WebRow(Icons.Default.OndemandVideo, "Search on YouTube: \"${query.trim()}\"") {
                            closeSearch()
                            startIntent(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "https://www.youtube.com/results?search_query=" +
                                            Uri.encode(query.trim())
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SearchSettingsSheet(viewModel) { showSettings = false }
    }

    if (showVaultPin) {
        VaultPinDialog(
            onDismiss = { showVaultPin = false },
            onUnlock = {
                showVaultPin = false
                closeSearch()
                viewModel.setShowHiddenApps(true)
            },
            checkPin = { viewModel.checkVaultPin(it) }
        )
    }
}

/**
 * The vault door (v1.15.0): the old `vault <pin>` / `apps <pin>` search commands are
 * gone, so Hidden Apps now asks for the four-digit PIN here. The PIN itself is the
 * same one the vault always used (default 1234, changeable inside Hidden Apps).
 */
@Composable
private fun VaultPinDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    checkPin: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hidden Apps", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the four-digit PIN.", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { c -> c.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("vault_pin_input")
                )
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (checkPin(pin)) onUnlock()
                    else error = "Wrong PIN — try again."
                },
                modifier = Modifier.testTag("vault_pin_unlock_btn")
            ) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Hint(text: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
    }
}

/** The Call / SMS / WhatsApp trio shown under a contact and under a typed number. */
@Composable
private fun ContactActions(context: Context, rawNumber: String, afterAction: () -> Unit) {
    Row(modifier = Modifier.padding(top = 6.dp)) {
        ActionButton(Icons.Default.Call, "Call") {
            afterAction()
            startIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawNumber")))
        }
        ActionButton(Icons.Default.Sms, "SMS") {
            afterAction()
            startIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$rawNumber")))
        }
        ActionButton(Icons.Default.Chat, "WhatsApp") {
            afterAction()
            openWhatsApp(context, rawNumber)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(21.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ContactRow(context: Context, entry: ContactEntry, afterAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Text(
            text = entry.name,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = entry.number,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ContactActions(context, entry.number, afterAction)
    }
}

@Composable
private fun FileRow(file: DeviceFile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = file.name,
                fontSize = 14.5.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = fileSubtitle(file),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    buttonLabel: String = "Turn on",
    onEnable: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnable() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(
                subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        TextButton(onClick = onEnable) { Text(buttonLabel) }
    }
}

@Composable
private fun WebRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 14.5.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun SearchRow(hit: SearchHit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hit.subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------- intent plumbing ----------

private fun startIntent(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found for this", Toast.LENGTH_SHORT).show()
    }
}

private fun openWhatsApp(context: Context, rawNumber: String) {
    val url = "https://wa.me/" + PhoneNumbers.international(rawNumber)
    val targeted = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.whatsapp")
    try {
        context.startActivity(targeted)
    } catch (e: ActivityNotFoundException) {
        startIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun openFile(context: Context, file: DeviceFile) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(file.uri), file.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startIntent(context, intent)
}

private fun fileSubtitle(file: DeviceFile): String {
    val type = file.mimeType?.substringAfter('/')?.uppercase(Locale.US) ?: "File"
    val size = formatSize(file.sizeBytes)
    return if (size.isEmpty()) type else "$type - $size"
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
