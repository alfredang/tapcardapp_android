package com.tertiaryinfotech.tapcard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.util.CardLinks
import com.tertiaryinfotech.tapcard.util.PhoneNumber
import com.tertiaryinfotech.tapcard.util.QrMode
import com.tertiaryinfotech.tapcard.util.VCard
import com.tertiaryinfotech.tapcard.util.launchSafely
import com.tertiaryinfotech.tapcard.vm.CardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(vm: CardViewModel) {
    val context = LocalContext.current
    val card = vm.draft
    var confirmDelete by remember { mutableStateOf(false) }
    var showNfc by remember { mutableStateOf(false) }
    var showSignature by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Digital card", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::goHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.openShare(card) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = vm::editDraft) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page, vertical = 8.dp),
        ) {
            CardFace(card)
            Spacer(Modifier.height(18.dp))

            // QR first: showing your phone to be scanned is the main in-person flow.
            QrPanel(card, vm.settings.defaultQrMode, vm.settings.countryCode)
            Spacer(Modifier.height(14.dp))

            PrimaryButton("Share card", icon = Icons.Filled.Share) { vm.openShare(card) }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Contact")
            Spacer(Modifier.height(6.dp))
            ContactActions(card, vm.settings.countryCode)

            if (card.bio.isNotBlank() || card.about.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("About")
                Spacer(Modifier.height(6.dp))
                AboutSection(card)
            }

            if (card.socialLinks.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Social")
                Spacer(Modifier.height(6.dp))
                SocialSection(card)
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Tools")
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hceAvailable(context)) {
                    ActionRow("Tap to share (NFC)", Icons.Filled.Contactless) { showNfc = true }
                }
                ActionRow("Email signature", Icons.Filled.AlternateEmail) { showSignature = true }
                ActionRow("Virtual background", Icons.Filled.Videocam) { showBackground = true }
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (vm.isPublishing) "Publishing…" else "Publish to web",
                icon = Icons.Filled.CloudUpload,
                loading = vm.isPublishing,
            ) { vm.publishDraft() }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showNfc) NfcShareDialog(card = card, onDismiss = { showNfc = false })
    if (showSignature) EmailSignatureDialog(card = card, onDismiss = { showSignature = false })
    if (showBackground) VirtualBackgroundDialog(card = card, onDismiss = { showBackground = false })

    vm.publishedUrl?.let { url ->
        PublishedDialog(url = url, onDismiss = vm::dismissPublished)
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

/**
 * Always-visible QR panel. Opens on the account's default mode (Settings →
 * Default QR code); the pill switcher flips between WhatsApp, LinkedIn, the
 * card link and the offline vCard for this one share.
 */
@Composable
private fun QrPanel(card: DigitalCard, defaultMode: QrMode, countryCode: String) {
    val modes = remember(card, countryCode) { CardLinks.availableModes(card, countryCode) }
    var mode by remember(card, defaultMode, countryCode) {
        mutableStateOf(CardLinks.resolveMode(card, defaultMode, countryCode))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(Radius.lg))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QrModePicker(modes, mode, { mode = it }, Modifier.fillMaxWidth())
        if (modes.size > 1) Spacer(Modifier.height(16.dp))
        CardQrImage(card, mode, countryCode, size = 200.dp)
        Spacer(Modifier.height(12.dp))
        QrCaption(card, mode, countryCode)
    }
}

@Composable
private fun ContactActions(card: DigitalCard, countryCode: String) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (card.phone.isNotBlank()) {
            ContactCircle("Call", Icons.Filled.Call) {
                context.launchSafely(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${PhoneNumber.international(card.phone, countryCode)}")),
                    "No phone app available",
                )
            }
        }
        CardLinks.whatsappChat(card, countryCode)?.let { chat ->
            ContactCircle("WhatsApp", Icons.AutoMirrored.Filled.Chat) {
                context.launchSafely(Intent(Intent.ACTION_VIEW, Uri.parse(chat)), "WhatsApp isn't installed")
            }
        }
        if (card.email.isNotBlank()) {
            ContactCircle("Email", Icons.Filled.Email) {
                context.launchSafely(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${card.email}")),
                    "No email app available",
                )
            }
        }
        if (card.website.isNotBlank()) {
            ContactCircle("Website", Icons.Filled.Language) {
                val url = if (card.website.startsWith("http")) card.website else "https://${card.website}"
                context.launchSafely(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                    "No browser available",
                )
            }
        }
        ContactCircle("Save", Icons.Filled.PersonAdd) {
            VCard.addToContacts(context, card)
        }
    }
}

/** A circular icon button with a label — the polished "contact actions" pattern. */
@Composable
private fun ContactCircle(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.tappable(onClick),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AboutSection(card: DigitalCard) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
    ) {
        val body = listOf(card.bio, card.about).filter { it.isNotBlank() }.joinToString("\n\n")
        Text(body, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SocialSection(card: DigitalCard) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        card.socialLinks.forEach { (label, url) ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(Radius.sm))
                    .tappable {
                        val u = if (url.startsWith("http")) url else "https://$url"
                        context.launchSafely(Intent(Intent.ACTION_VIEW, Uri.parse(u)), "No browser available")
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun PublishedDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card published 🎉") },
        text = { Text("Your live card is ready to share:\n\n$url") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    context.launchSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser available")
                }) { Text("Open") }
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.launchSafely(Intent.createChooser(share, "Share card link"))
                }) { Text("Share") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}