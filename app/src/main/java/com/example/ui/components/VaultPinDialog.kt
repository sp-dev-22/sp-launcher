package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BiometricHelper

/**
 * The vault door (v1.15.0, shared since v1.20.0).
 *
 * Hidden Apps sits behind the four-digit vault PIN. This dialog is the only way
 * in: the search screen opens it from its menu, and the "Hidden apps" row on the
 * Apps & Widgets page opens the same one. Keeping it in one place means a new
 * door can never forget the lock.
 *
 * The PIN is the same one the vault always used (default 1234, changeable
 * inside Hidden Apps). Fingerprint is offered when the device can do it; the
 * PIN always stays available.
 */
@Composable
fun VaultPinDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    checkPin: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    val fingerprintAvailable = BiometricHelper.isAvailable(context)

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
                if (fingerprintAvailable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity,
                                title = "Hidden Apps",
                                subtitle = "Unlock with your fingerprint",
                                onSuccess = onUnlock,
                                onError = { error = it }
                            )
                        },
                        modifier = Modifier.testTag("vault_fingerprint_btn")
                    ) { Text("Use fingerprint") }
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
                    if (checkPin(pin)) onUnlock()
                    else error = "Wrong PIN — try again."
                },
                modifier = Modifier.testTag("vault_pin_unlock_btn")
            ) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
