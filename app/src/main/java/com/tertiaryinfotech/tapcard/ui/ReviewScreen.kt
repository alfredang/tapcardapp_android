package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
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
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.CardThemes
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.util.CardImages
import com.tertiaryinfotech.tapcard.vm.CardViewModel

/** Preset accent colours offered in the editor — a full, varied spectrum. */
private val AccentSwatches = listOf(
    0xFF2563EB, // blue
    0xFF0EA5E9, // sky
    0xFF06B6D4, // cyan
    0xFF14B8A6, // teal
    0xFF10B981, // emerald
    0xFF84CC16, // lime
    0xFFEAB308, // yellow
    0xFFF59E0B, // amber
    0xFFF97316, // orange
    0xFFEF4444, // red
    0xFFEC4899, // pink
    0xFFD946EF, // fuchsia
    0xFF8B5CF6, // violet
    0xFF6366F1, // indigo
    0xFF334155, // slate
    0xFF111114, // black
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: CardViewModel) {
    val card = vm.draft
    val isNew = card.createdAtEpoch == 0L

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Tight header — compact back button sits just above the title (no big gap).
            Column {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .offset(x = (-4).dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .tappable { if (isNew) vm.goHome() else vm.openCard(card) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(4.dp))
                ScreenTitle(
                    if (isNew) "New card" else "Edit card",
                    "Fill in the details — everything syncs to your account",
                )
            }

            // Live preview — updates instantly as you edit fields, theme, or colour.
            CardFace(card)

            EditorSection("ABOUT") {
                GroupedField("Name", card.name, Icons.Filled.Person) { vm.draft = card.copy(name = it) }
                GroupedField("Job title", card.title, Icons.Filled.Badge) { vm.draft = card.copy(title = it) }
                GroupedField("Company", card.company, Icons.Filled.Apartment) { vm.draft = card.copy(company = it) }
                GroupedField("Department", card.department, Icons.Filled.Groups) { vm.draft = card.copy(department = it) }
                GroupedField("Tagline", card.tagline, Icons.Filled.Info) { vm.draft = card.copy(tagline = it) }
                GroupedField("Short bio", card.bio, Icons.Filled.Description, singleLine = false) { vm.draft = card.copy(bio = it) }
                AiWriteButton("bio", busy = vm.generatingField == "bio", enabled = vm.generatingField == null) { vm.generateField("bio") }
                GroupedField("About", card.about, Icons.Filled.Description, singleLine = false) { vm.draft = card.copy(about = it) }
                AiWriteButton("about", busy = vm.generatingField == "about", enabled = vm.generatingField == null) { vm.generateField("about") }
            }

            EditorSection("CONTACT") {
                GroupedField("Mobile", card.phone, Icons.Filled.Phone, KeyboardType.Phone) { vm.draft = card.copy(phone = it) }
                GroupedField("Office phone", card.officePhone, Icons.Filled.Call, KeyboardType.Phone) { vm.draft = card.copy(officePhone = it) }
                GroupedField("WhatsApp", card.whatsapp, Icons.AutoMirrored.Filled.Chat, KeyboardType.Phone) { vm.draft = card.copy(whatsapp = it) }
                GroupedField("Email", card.email, Icons.Filled.Email, KeyboardType.Email) { vm.draft = card.copy(email = it) }
                GroupedField("Website", card.website, Icons.Filled.Language, KeyboardType.Uri) { vm.draft = card.copy(website = it) }
                GroupedField("Address", card.address, Icons.Filled.LocationOn, singleLine = false) { vm.draft = card.copy(address = it) }
            }

            EditorSection("SOCIAL") {
                GroupedField("LinkedIn", card.linkedin, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(linkedin = it) }
                GroupedField("Instagram", card.instagram, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(instagram = it) }
                GroupedField("X (Twitter)", card.twitter, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(twitter = it) }
                GroupedField("Facebook", card.facebook, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(facebook = it) }
                GroupedField("TikTok", card.tiktok, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(tiktok = it) }
                GroupedField("Telegram", card.telegram, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(telegram = it) }
                GroupedField("YouTube", card.youtube, Icons.Filled.Link, KeyboardType.Uri) { vm.draft = card.copy(youtube = it) }
            }

            EditorSection("MEDIA") {
                ImagePickField("Profile photo", card.profilePhoto) { vm.draft = card.copy(profilePhoto = it) }
                ImagePickField("Company logo", card.companyLogo) { vm.draft = card.copy(companyLogo = it) }
                ImagePickField("Cover banner", card.coverBanner) { vm.draft = card.copy(coverBanner = it) }
            }

            EditorSection("DESIGN") {
                PickerLabel("Card theme", "The overall style of your card")
                ThemePicker(selected = card.theme) { vm.draft = card.copy(theme = it) }
                Spacer(Modifier.height(4.dp))
                PickerLabel("Accent colour", "Used for highlights and buttons")
                ColorPicker(selected = card.accentColor) { vm.draft = card.copy(accentColor = it) }
            }

            Spacer(Modifier.height(14.dp))
            SaveButton(isNew = isNew, accent = BrandBlue, saving = vm.isSaving) {
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

/**
 * An image field that lets the user pick from the gallery or take a photo. The
 * chosen image is downscaled and stored inline as a base64 data URL (see
 * [CardImages]); a pasted URL still works too if one is already set.
 */
@Composable
private fun ImagePickField(label: String, value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun ingest(uri: Uri?) {
        if (uri == null) return
        busy = true
        scope.launch {
            val dataUrl = CardImages.uriToDataUrl(context, uri)
            busy = false
            if (dataUrl != null) onChange(dataUrl)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> ingest(uri) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success: Boolean -> if (success) ingest(cameraUri) }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    busy -> androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = BrandBlue)
                    value.isNotBlank() -> AsyncImage(
                        model = CardImages.model(value),
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                    else -> Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickChip(Icons.Filled.PhotoLibrary, "Choose") {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                    PickChip(Icons.Filled.PhotoCamera, "Camera") {
                        val uri = newCameraUri(context)
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                }
                if (value.isNotBlank()) {
                    Text(
                        "Remove",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.tappable { onChange("") },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickChip(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BrandBlue.copy(alpha = 0.12f))
            .tappable(onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A cache-dir file URI (via FileProvider) for the camera to write a capture into. */
private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "cam-${System.nanoTime()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** A small titled label + hint above a picker, so its purpose is obvious. */
@Composable
private fun PickerLabel(title: String, hint: String) {
    Column(Modifier.padding(start = 2.dp, bottom = 8.dp)) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A labelled group of fields inside a clean surface card. */
@Composable
private fun EditorSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    SectionLabel(label)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(Radius.md))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Borderless filled field, used inside [EditorSection] cards for a clean grouped look. */
@Composable
private fun GroupedField(
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

