package com.tertiaryinfotech.tapcard.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.net.ApiConfig
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.util.VCard
import com.tertiaryinfotech.tapcard.util.launchSafely

/** Public shareable URL for a card once it has been published (has a slug). */
private fun DigitalCard.publicUrl(): String? =
    slug.takeIf { it.isNotBlank() }?.let { "${ApiConfig.PUBLIC_WEB_URL}/c/$it" }

/**
 * Blinq-style share surface: a big QR (works offline via vCard), the public link
 * with copy, and quick share / save-contact actions. Opened from the My Card hero
 * and from the card detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(card: DigitalCard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qr = remember(card) { VCard.qrBitmap(card).asImageBitmap() }
    val link = card.publicUrl()

    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { reveal = true }
    val qrScale by animateFloatAsState(
        targetValue = if (reveal) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "qrScale",
    )
    val qrAlpha by animateFloatAsState(if (reveal) 1f else 0f, label = "qrAlpha")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.page)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Share your card",
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                card.displayName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            // QR panel (springs in on open)
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = qrScale
                        scaleY = qrScale
                        alpha = qrAlpha
                    }
                    .shadow(16.dp, RoundedCornerShape(Radius.lg), spotColor = BrandBlue.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(Color.White)
                    .padding(20.dp),
            ) {
                Image(
                    bitmap = qr,
                    contentDescription = "QR code for ${card.displayName}",
                    modifier = Modifier.size(230.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Scan to save my contact",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(20.dp))

            // Public link (only once published)
            if (link != null) {
                LinkRow(link = link, clipboard = clipboard) {
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Share link", icon = Icons.Filled.Share) {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "$link\n\nMy digital business card, powered by Tapcard.")
                    }
                    context.launchSafely(Intent.createChooser(share, "Share card link"))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedActionButton("Send via SMS", Icons.Filled.Sms) {
                    val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                        putExtra("sms_body", "$link\n\nMy digital business card, powered by Tapcard.")
                    }
                    runCatching { context.startActivity(sms) }
                        .onFailure { Toast.makeText(context, "No messaging app found", Toast.LENGTH_SHORT).show() }
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedActionButton("Share QR image", Icons.Filled.QrCode2) {
                VCard.shareQr(context, card)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedActionButton("Save to contacts", Icons.Filled.PersonAdd) {
                VCard.addToContacts(context, card)
            }
        }
    }
}

@Composable
private fun LinkRow(link: String, clipboard: ClipboardManager, onCopied: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .tappable {
                clipboard.setText(AnnotatedString(link))
                onCopied()
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            link.removePrefix("https://"),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(8.dp))
        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy link", tint = BrandBlue, modifier = Modifier.size(18.dp))
    }
}