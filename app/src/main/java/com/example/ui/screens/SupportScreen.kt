package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

private const val UPI_ID = "sp.26@ptyes"
private const val GITHUB_URL = "https://github.com/sp-dev-22"
private const val YOUTUBE_URL = "https://youtube.com/@sp-dev-22"
private const val EMAIL = "somu.22.2026@gmail.com"

/**
 * Tips and contact details.
 *
 * Nothing here talks to the network: the buttons only hand a link or a `upi://` payment request
 * to an app the user already has. That is why SP Launcher can keep working without the INTERNET
 * permission.
 */
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .testTag("support_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("support_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Support",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Text(
            text = "SP Launcher is free, has no ads and asks for no account. It does not even have " +
                "the internet permission, so nothing you do here leaves your phone.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "If it saves you some time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "You can send a tip on UPI. Any amount, once, no subscription.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Image(
                    painter = painterResource(id = R.drawable.upi_qr),
                    contentDescription = "UPI QR code for $UPI_ID",
                    modifier = Modifier
                        .size(200.dp)
                        .testTag("support_qr")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = UPI_ID,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(UPI_ID))
                            Toast.makeText(context, "UPI ID copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("support_copy_upi")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        openUri(
                            context = context,
                            uri = Uri.parse("upi://pay?pa=$UPI_ID&pn=SP%20Launcher&cu=INR"),
                            failure = "No UPI app found on this phone"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_open_upi")
                ) {
                    Text(text = "Open a UPI app", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ELSEWHERE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LinkRow(
            title = "GitHub",
            subtitle = "github.com/sp-dev-22",
            onClick = { openUri(context, Uri.parse(GITHUB_URL), "No browser found") }
        )
        LinkRow(
            title = "YouTube",
            subtitle = "@sp-dev-22",
            onClick = { openUri(context, Uri.parse(YOUTUBE_URL), "No browser found") }
        )
        LinkRow(
            title = "Email",
            subtitle = EMAIL,
            onClick = { openUri(context, Uri.parse("mailto:$EMAIL"), "No email app found") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "These buttons only hand the link to an app you already have. SP Launcher " +
                "itself never connects to the internet.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun openUri(context: Context, uri: Uri, failure: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
    }
}
