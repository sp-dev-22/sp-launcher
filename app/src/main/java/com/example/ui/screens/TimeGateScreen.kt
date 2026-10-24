package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.LauncherViewModel
import java.util.Calendar

/**
 * Looks like a plain "set the clock" screen. It is really the secret door to the hidden-apps
 * screen: set the time to the vault PIN (e.g. 12:34 -> PIN 1234) and the hidden-apps screen
 * opens. Any other time is treated as a real time change - the Home clock shows it for 30
 * seconds and then snaps back to the device time.
 */
@Composable
fun TimeGateScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val now = remember { Calendar.getInstance() }
    var hour by remember { mutableStateOf("%02d".format(now.get(Calendar.HOUR_OF_DAY))) }
    var minute by remember { mutableStateOf("%02d".format(now.get(Calendar.MINUTE))) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("time_gate_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Set time",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hour : Minute",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hour,
                onValueChange = { raw -> hour = raw.filter { it.isDigit() }.take(2) },
                singleLine = true,
                modifier = Modifier.width(96.dp).testTag("time_gate_hour")
            )
            Text(
                text = ":",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            OutlinedTextField(
                value = minute,
                onValueChange = { raw -> minute = raw.filter { it.isDigit() }.take(2) },
                singleLine = true,
                modifier = Modifier.width(96.dp).testTag("time_gate_minute")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("time_gate_cancel")) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val h = hour.toIntOrNull()
                    val m = minute.toIntOrNull()
                    if (h != null && m != null && h in 0..23 && m in 0..59) {
                        if (viewModel.checkVaultPin(h, m)) {
                            viewModel.setShowTimeGate(false)
                            viewModel.setShowHiddenApps(true)
                        } else {
                            viewModel.setClockOverride(h, m)
                            onBack()
                        }
                    }
                },
                modifier = Modifier.testTag("time_gate_set")
            ) {
                Text("Set")
            }
        }
    }
}
