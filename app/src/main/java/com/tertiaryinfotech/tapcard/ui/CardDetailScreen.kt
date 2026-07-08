package com.tertiaryinfotech.tapcard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.tapcard.model.CardLink
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.util.VCard
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

            PrimaryButton("Share card", icon = Icons.Filled.Share) { vm.openShare(card) }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Contact")
            Spacer(Modifier.height(6.dp))
            ContactActions(card)

            val activeLinks = card.links.filter { it.url.isNotBlank() }
            if (activeLinks.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Links")
                Spacer(Modifier.height(6.dp))
                LinksSection(activeLinks)
            }

            if (card.introVideo.isNotBlank() || card.gallery.any { it.isNotBlank() }) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Media")
                Spacer(Modifier.height(6.dp))
                MediaSection(card)
            }

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

@Composable
private fun ContactActions(card: DigitalCard) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (card.phone.isNotBlank()) {
            ActionRow("Call", Icons.Filled.Call, subtitle = card.phone) {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${card.phone.filter { it.isDigit() || it == '+' }}")),
                )
            }
            ActionRow("WhatsApp", Icons.AutoMirrored.Filled.Chat, subtitle = card.phone) {
                val num = card.phone.filter { it.isDigit() }
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num")))
            }
        }
        if (card.email.isNotBlank()) {
            ActionRow("Email", Icons.Filled.Email, subtitle = card.email) {
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${card.email}")))
            }
        }
        if (card.website.isNotBlank()) {
            ActionRow("Website", Icons.Filled.Language, subtitle = card.website) {
                val url = if (card.website.startsWith("http")) card.website else "https://${card.website}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        ActionRow("Save to contacts", Icons.Filled.PersonAdd) {
            VCard.addToContacts(context, card)
        }
    }
}

@Composable
private fun LinksSection(links: List<CardLink>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        links.forEach { link ->
            val title = link.label.ifBlank { link.kind.lowercase().replaceFirstChar { it.uppercase() } }
            ActionRow(title, linkIcon(link.kind)) {
                val u = if (link.url.startsWith("http")) link.url else "https://${link.url}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
            }
        }
    }
}

@Composable
private fun MediaSection(card: DigitalCard) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (card.introVideo.isNotBlank()) {
            ActionRow("Watch intro video", Icons.Filled.PlayCircle) {
                val u = if (card.introVideo.startsWith("http")) card.introVideo else "https://${card.introVideo}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
            }
        }
        val imgs = card.gallery.filter { it.isNotBlank() }
        if (imgs.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                imgs.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }
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
                    .tappable {
                        val u = if (url.startsWith("http")) url else "https://$url"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
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
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }) { Text("Open") }
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(share, "Share card link"))
                }) { Text("Share") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}