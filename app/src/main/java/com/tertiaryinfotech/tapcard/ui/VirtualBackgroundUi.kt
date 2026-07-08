package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.util.VirtualBackground

/**
 * Previews the card's virtual meeting background and lets the user save/share it
 * for use in Zoom / Teams / Meet.
 */
@Composable
fun VirtualBackgroundDialog(card: DigitalCard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val preview = remember(card.id) { VirtualBackground.generate(card).asImageBitmap() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Virtual background") },
        text = {
            Column {
                Text(
                    "Use this as your Zoom / Teams / Meet background — it shows your card's QR so people can scan it right off the call.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Image(
                    bitmap = preview,
                    contentDescription = "Virtual background preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                VirtualBackground.share(context, card)
                onDismiss()
            }) { Text("Save / Share") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}