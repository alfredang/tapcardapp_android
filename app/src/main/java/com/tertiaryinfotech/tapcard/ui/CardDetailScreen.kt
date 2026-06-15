package com.tertiaryinfotech.tapcard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.util.VCard
import com.tertiaryinfotech.tapcard.vm.CardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(vm: CardViewModel) {
    val context = LocalContext.current
    val card = vm.draft
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital card") },
                navigationIcon = {
                    IconButton(onClick = vm::goHome) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::editDraft) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CardFace(card)
            Spacer(Modifier.height(20.dp))
            QrPanel(card)
            Spacer(Modifier.height(20.dp))
            QuickActions(card)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete card?") },
            text = { Text("This removes “${card.displayName}” from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteCard(card)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CardFace(card: DigitalCard) {
    val accent = Color(card.accentColor)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.7f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.75f)))),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                card.displayName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            if (card.title.isNotBlank()) {
                Text(card.title, color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp)
            }
            if (card.company.isNotBlank()) {
                Text(card.company, color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            if (card.phone.isNotBlank()) ContactLine(card.phone)
            if (card.email.isNotBlank()) ContactLine(card.email)
            if (card.website.isNotBlank()) ContactLine(card.website)
        }
        Text(
            "Powered by Tapcard",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        )
    }
}

@Composable
private fun ContactLine(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp)
}

@Composable
private fun QrPanel(card: DigitalCard) {
    val qr = remember(card) { VCard.qrBitmap(card).asImageBitmap() }
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = qr,
            contentDescription = "QR code for ${card.displayName}",
            modifier = Modifier.size(220.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text("Scan to save this contact", color = Color(0xFF555555), fontSize = 13.sp)
    }
}

@Composable
private fun QuickActions(card: DigitalCard) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Share QR", Icons.Filled.Share, Modifier.weight(1f)) {
                VCard.shareQr(context, card)
            }
            ActionTile("Save contact", Icons.Filled.PersonAdd, Modifier.weight(1f)) {
                VCard.addToContacts(context, card)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (card.phone.isNotBlank()) {
                ActionTile("Call", Icons.Filled.Call, Modifier.weight(1f)) {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${card.phone.filter { it.isDigit() || it == '+' }}")),
                    )
                }
                ActionTile("WhatsApp", Icons.Filled.Chat, Modifier.weight(1f)) {
                    val num = card.phone.filter { it.isDigit() }
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num")),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (card.email.isNotBlank()) {
                ActionTile("Email", Icons.Filled.Email, Modifier.weight(1f)) {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${card.email}")),
                    )
                }
            }
            if (card.website.isNotBlank()) {
                ActionTile("Website", Icons.Filled.Language, Modifier.weight(1f)) {
                    val url = if (card.website.startsWith("http")) card.website else "https://${card.website}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
