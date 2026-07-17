package com.tertiaryinfotech.tapcard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.util.EmailSignature
import com.tertiaryinfotech.tapcard.util.launchSafely

/**
 * Previews the card's email signature and lets the user copy it (as rich HTML +
 * plain text, so it pastes formatted into Gmail/Outlook) or share it.
 */
@Composable
fun EmailSignatureDialog(card: DigitalCard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val plain = EmailSignature.plain(card)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Email signature") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Copy this into your email app's signature settings (Gmail, Outlook…).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    plain,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                copySignature(context, card)
                onDismiss()
            }) { Text("Copy") }
        },
        dismissButton = {
            TextButton(onClick = {
                context.launchSafely(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, plain)
                        },
                        "Share signature",
                    ),
                )
                onDismiss()
            }) { Text("Share") }
        },
    )
}

private fun copySignature(context: Context, card: DigitalCard) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newHtmlText("Email signature", EmailSignature.plain(card), EmailSignature.html(card)),
    )
    Toast.makeText(context, "Signature copied", Toast.LENGTH_SHORT).show()
}