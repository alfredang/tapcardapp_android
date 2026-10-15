package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.util.CardLinks
import com.tertiaryinfotech.tapcard.util.QrMode
import com.tertiaryinfotech.tapcard.util.VCard

/** The QR image for [card] in [mode] on a white tile (QR scanners need the contrast). */
@Composable
fun CardQrImage(card: DigitalCard, mode: QrMode, countryCode: String, size: Dp, modifier: Modifier = Modifier) {
    val payload = remember(card, mode, countryCode) { CardLinks.qrPayload(card, mode, countryCode) }
    val bitmap = remember(payload) { VCard.qrBitmap(payload).asImageBitmap() }
    Box(
        modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Image(bitmap = bitmap, contentDescription = CardLinks.caption(mode), modifier = Modifier.size(size))
    }
}

/**
 * Pill switcher between the QR modes this card supports. Hidden when only one
 * mode is possible, so a brand-new unpublished card shows just the QR.
 */
@Composable
fun QrModePicker(modes: List<QrMode>, selected: QrMode, onSelect: (QrMode) -> Unit, modifier: Modifier = Modifier) {
    if (modes.size < 2) return
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) BrandBlue else Color.Transparent)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .tappable { onSelect(mode) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Caption under the QR, plus a nudge when WhatsApp mode isn't possible yet. */
@Composable
fun QrCaption(card: DigitalCard, mode: QrMode, countryCode: String) {
    Text(
        CardLinks.caption(mode),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    val hint = when {
        mode == QrMode.WHATSAPP -> "They point their camera — WhatsApp opens a chat with you, your card underneath."
        mode == QrMode.LINKEDIN -> "Opens your LinkedIn profile so they can connect."
        CardLinks.publicUrl(card) == null -> "Sign in and save your card to get a WhatsApp QR."
        CardLinks.whatsappNumber(card, countryCode) == null -> "Add a mobile or WhatsApp number to get a WhatsApp QR."
        else -> null
    }
    if (hint != null) {
        Text(
            hint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 12.dp, end = 12.dp),
        )
    }
}
