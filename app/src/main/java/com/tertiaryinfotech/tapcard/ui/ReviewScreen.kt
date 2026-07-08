package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.CardLink
import com.tertiaryinfotech.tapcard.model.CardLinkKinds
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.CardThemes
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel

/** Preset accent colors offered in the editor. */
private val AccentSwatches = listOf(
    0xFF2563EB, 0xFF0EA5E9, 0xFF7C5CFF, 0xFF10B981,
    0xFFF59E0B, 0xFFEF4444, 0xFFEC4899, 0xFF334155,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: CardViewModel) {
    val card = vm.draft
    val isNew = card.createdAtEpoch == 0L

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Review details" else "Edit card", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (isNew) vm.goHome() else vm.openCard(card) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SectionLabel("ABOUT")
            Field("Name", card.name, Icons.Filled.Person) { vm.draft = card.copy(name = it) }
            Field("Job title", card.title, Icons.Filled.Badge) { vm.draft = card.copy(title = it) }
            Field("Company", card.company, Icons.Filled.Apartment) { vm.draft = card.copy(company = it) }
            Field("Department", card.department, Icons.Filled.Groups) { vm.draft = card.copy(department = it) }
            Field("Tagline", card.tagline, Icons.Filled.Info) { vm.draft = card.copy(tagline = it) }
            Field("Short bio", card.bio, Icons.Filled.Description, singleLine = false) { vm.draft = card.copy(bio = it) }
            AiWriteButton("bio", busy = vm.generatingField == "bio", enabled = vm.generatingField == null) { vm.generateField("bio") }
            Field("About", card.about, Icons.Filled.Description, singleLine = false) { vm.draft = card.copy(about = it) }
            AiWriteButton("about", busy = vm.generatingField == "about", enabled = vm.generatingField == null) { vm.generateField("about") }

            Spacer(Modifier.height(6.dp))
            SectionLabel("CONTACT")
            Field("Mobile", card.phone, Icons.Filled.Phone, KeyboardType.Phone) { vm.draft = card.copy(phone = it) }
            Field("Office phone", card.officePhone, Icons.Filled.Call, KeyboardType.Phone) { vm.draft = card.copy(officePhone = it) }
            Field("WhatsApp", card.whatsapp, Icons.AutoMirrored.Filled.Chat, KeyboardType.Phone) { vm.draft = card.copy(whatsapp = it) }
            Field("Email", card.email, Icons.Filled.Email, KeyboardType.Email) { vm.draft = card.copy(email = it) }
            Field("Website", card.website, Icons.Filled.Language, KeyboardType.Uri) { vm.draft = card.copy(website = it) }
            Field("Address", card.address, Icons.Filled.LocationOn, singleLine = false) { vm.draft = card.copy(address = it) }

            Spacer(Modifier.height(6.dp))
            SectionLabel("SOCIAL")
            Field("LinkedIn", card.linkedin, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(linkedin = it) }
            Field("Instagram", card.instagram, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(instagram = it) }
            Field("X (Twitter)", card.twitter, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(twitter = it) }
            Field("Facebook", card.facebook, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(facebook = it) }
            Field("TikTok", card.tiktok, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(tiktok = it) }
            Field("Telegram", card.telegram, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(telegram = it) }
            Field("YouTube", card.youtube, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(youtube = it) }

            Spacer(Modifier.height(6.dp))
            SectionLabel("MEDIA (image URLs)")
            Field("Profile photo URL", card.profilePhoto, Icons.Filled.Image, KeyboardType.Uri) { vm.draft = card.copy(profilePhoto = it) }
            Field("Company logo URL", card.companyLogo, Icons.Filled.Image, KeyboardType.Uri) { vm.draft = card.copy(companyLogo = it) }
            Field("Cover banner URL", card.coverBanner, Icons.Filled.Image, KeyboardType.Uri) { vm.draft = card.copy(coverBanner = it) }
            Field("Intro video URL", card.introVideo, Icons.Filled.Movie, KeyboardType.Uri) { vm.draft = card.copy(introVideo = it) }

            Spacer(Modifier.height(6.dp))
            SectionLabel("GALLERY (image URLs)")
            GalleryEditor(card.gallery) { vm.draft = card.copy(gallery = it) }

            Spacer(Modifier.height(6.dp))
            SectionLabel("LINKS")
            LinksEditor(card.links) { vm.draft = card.copy(links = it) }

            Spacer(Modifier.height(6.dp))
            SectionLabel("DESIGN")
            ThemePicker(selected = card.theme) { vm.draft = card.copy(theme = it) }
            Spacer(Modifier.height(4.dp))
            ColorPicker(selected = card.accentColor) { vm.draft = card.copy(accentColor = it) }

            Spacer(Modifier.height(14.dp))
            SaveButton(isNew = isNew, accent = Color(card.accentColor), saving = vm.isSaving) {
                vm.saveDraft(System.currentTimeMillis())
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AiWriteButton(field: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BrandBlue.copy(alpha = 0.12f))
            .then(if (enabled) Modifier.tappable(onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            androidx.compose.material3.CircularProgressIndicator(
                Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandBlue,
            )
        } else {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            if (busy) "Writing…" else "Write with AI",
            color = BrandBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ThemePicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CardThemes.forEach { theme ->
            val isSelected = theme.key == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(width = 60.dp, height = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(theme.bannerStart, theme.bannerEnd)))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelect(theme.key) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = theme.bannerText, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    theme.label,
                    fontSize = 11.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ColorPicker(selected: Long, onSelect: (Long) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AccentSwatches.forEach { argb ->
            val isSelected = argb == selected
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 0.9f else 0f),
                        shape = CircleShape,
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SaveButton(isNew: Boolean, accent: Color, saving: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent)
            .then(if (!saving) Modifier.tappable(onClick) else Modifier)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (saving) {
            androidx.compose.material3.CircularProgressIndicator(
                Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White,
            )
        } else {
            Text(
                if (isNew) "Create card" else "Save changes",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun GalleryEditor(items: List<String>, onChange: (List<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, url ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { v -> onChange(items.toMutableList().also { it[index] = v }) },
                    label = { Text("Image URL ${index + 1}") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onChange(items.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        AddRowButton("Add image") { onChange(items + "") }
    }
}

@Composable
private fun LinksEditor(items: List<CardLink>, onChange: (List<CardLink>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, link ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Link ${index + 1}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onChange(items.toMutableList().also { it.removeAt(index) }) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove link", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CardLinkKinds.forEach { kind ->
                        val selected = kind == link.kind
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) BrandBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onChange(items.toMutableList().also { it[index] = link.copy(kind = kind) }) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(linkIcon(kind), contentDescription = null, tint = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                kind.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = link.label,
                    onValueChange = { v -> onChange(items.toMutableList().also { it[index] = link.copy(label = v) }) },
                    label = { Text("Label (e.g. Book a meeting)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = link.url,
                    onValueChange = { v -> onChange(items.toMutableList().also { it[index] = link.copy(url = v) }) },
                    label = { Text("URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        AddRowButton("Add link") { onChange(items + CardLink()) }
    }
}

@Composable
private fun AddRowButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrandBlue.copy(alpha = 0.10f))
            .tappable(onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = BrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}
