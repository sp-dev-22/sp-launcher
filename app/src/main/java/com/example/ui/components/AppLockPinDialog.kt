package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppLockDialog
import com.example.util.AppLock

/**
 * The App Lock PIN dialog (v1.14.0).
 *
 * One dialog for all four moments: setting the first PIN (locking the first app),
 * verifying the PIN to open a locked app, verifying it to unlock an app, and
 * changing it. The PIN is 4-6 digits; it is checked here and hashed in the
 * ViewModel — the plaintext never leaves this dialog.
 */
@Composable
fun AppLockPinDialog(
    dialog: AppLockDialog,
    appLabel: String,
    onDismiss: () -> Unit,
    onSetupPin: (String) -> Unit,
    /** Raw PIN check, used only to confirm the current PIN when changing it. */
    onVerifyPin: (String) -> Boolean,
    /** Verify + act for the launch/unlock dialogs (closes the dialog on success). */
    onSubmitVerify: (String) -> Boolean,
    onChangePin: (String) -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var third by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val (title, body, confirmLabel) = when (dialog) {
        is AppLockDialog.SetupPin -> Triple(
            "Set a PIN to lock $appLabel",
            "4-6 digits. This one PIN locks every app you choose, and it is stored " +
                "only as a secure hash — the PIN itself is never saved.",
            "Lock app"
        )
        is AppLockDialog.VerifyToLaunch -> Triple(
            "Enter PIN to open $appLabel",
            "This app is locked. Enter your App Lock PIN to open it.",
            "Unlock"
        )
        is AppLockDialog.VerifyToUnlock -> Triple(
            "Enter PIN to unlock $appLabel",
            "Unlocking needs the current App Lock PIN.",
            "Unlock"
        )
        AppLockDialog.ChangePin -> Triple(
            "Change PIN",
            "Enter the current PIN, then the new one twice. The new PIN must be 4-6 digits.",
            "Save"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(body, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                when (dialog) {
                    is AppLockDialog.SetupPin -> {
                        PinField(
                            value = first,
                            onValueChange = { first = it },
                            label = "New PIN (4-6 digits)",
                            testTag = "applock_pin_new"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PinField(
                            value = second,
                            onValueChange = { second = it },
                            label = "Repeat PIN",
                            testTag = "applock_pin_repeat"
                        )
                    }
                    is AppLockDialog.VerifyToLaunch,
                    is AppLockDialog.VerifyToUnlock -> {
                        PinField(
                            value = first,
                            onValueChange = { first = it },
                            label = "PIN",
                            testTag = "applock_pin_input"
                        )
                    }
                    AppLockDialog.ChangePin -> {
                        PinField(
                            value = first,
                            onValueChange = { first = it },
                            label = "Current PIN",
                            testTag = "applock_pin_current"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PinField(
                            value = second,
                            onValueChange = { second = it },
                            label = "New PIN (4-6 digits)",
                            testTag = "applock_pin_new"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PinField(
                            value = third,
                            onValueChange = { third = it },
                            label = "Repeat new PIN",
                            testTag = "applock_pin_repeat"
                        )
                    }
                }
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when (dialog) {
                        is AppLockDialog.SetupPin -> when {
                            !AppLock.isValidPin(first) -> "The PIN must be 4-6 digits."
                            first != second -> "The two PINs do not match."
                            else -> {
                                onSetupPin(first)
                                null
                            }
                        }
                        is AppLockDialog.VerifyToLaunch,
                        is AppLockDialog.VerifyToUnlock -> {
                            if (onSubmitVerify(first)) {
                                null
                            } else {
                                first = ""
                                "Wrong PIN — try again."
                            }
                        }
                        AppLockDialog.ChangePin -> when {
                            !onVerifyPin(first) -> "Current PIN is wrong."
                            !AppLock.isValidPin(second) -> "The new PIN must be 4-6 digits."
                            second != third -> "The two PINs do not match."
                            else -> {
                                onChangePin(second)
                                null
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("applock_pin_confirm")
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    )
}
