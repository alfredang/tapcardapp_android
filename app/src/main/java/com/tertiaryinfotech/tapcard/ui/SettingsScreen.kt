package com.tertiaryinfotech.tapcard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.BuildConfig
import com.tertiaryinfotech.tapcard.net.ApiConfig
import com.tertiaryinfotech.tapcard.ui.theme.cardTheme
import com.tertiaryinfotech.tapcard.util.PhoneNumber
import com.tertiaryinfotech.tapcard.util.QrMode
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.util.CardImages
import com.tertiaryinfotech.tapcard.util.launchSafely
import com.tertiaryinfotech.tapcard.vm.CardViewModel

private val DangerRed = Color(0xFFDC2626)

// Per-category chip colours — a calm, professional spread (iOS-style).
private val ColorSync = Color(0xFF059669)     // green
private val ColorInsights = Color(0xFF2563EB) // blue
private val ColorShare = Color(0xFF0891B2)    // cyan
private val ColorHelp = Color(0xFFF59E0B)     // amber
private val ColorPrivacy = Color(0xFF7C3AED)  // violet
private val ColorNeutral = Color(0xFF64748B)  // slate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: CardViewModel) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showDesign by remember { mutableStateOf(false) }
    var showCountry by remember { mutableStateOf(false) }
    val myCard = vm.cards.firstOrNull()
    // Pick up anything changed on the web since launch.
    LaunchedEffect(Unit) { vm.refreshSettings() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item { ScreenTitle("Settings") }
            item { AccountCard(vm) { vm.cards.firstOrNull()?.let(vm::openCard) } }

            // Mobile-first: the everyday card + sharing choices live here, and
            // sync to the web's /settings when signed in.
            item { SectionLabel("Your card") }
            item {
                SettingsGroup {
                    SettingRow(
                        title = "Card design",
                        subtitle = myCard?.let { "${cardTheme(it.theme).label} · tap to change style & colour" }
                            ?: "Create your card first",
                        icon = Icons.Filled.Palette,
                        tint = ColorPrivacy,
                    ) { if (myCard != null) showDesign = true else vm.scanForMyCard() }
                    RowDivider()
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingIcon(Icons.Filled.QrCode2, ColorShare)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Default QR code", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "What people get when they scan your code",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        QrModePicker(
                            modes = QrMode.defaults,
                            selected = vm.settings.defaultQrMode,
                            onSelect = vm::setDefaultQrMode,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    RowDivider()
                    SettingRow(
                        title = "Country code",
                        subtitle = PhoneNumber.countryLabel(vm.settings.countryCode) +
                            " · added to local numbers for WhatsApp",
                        icon = Icons.Filled.Public,
                        tint = ColorSync,
                    ) { showCountry = true }
                }
            }
            if (vm.isAuthenticated) {
                item {
                    Text(
                        "Synced with your account — changes here also apply on tapcard.tertiaryinfotech.com.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item { SectionLabel("Sync") }
            item {
                SettingsGroup {
                    val signedIn = vm.isAuthenticated
                    SettingRow(
                        title = if (signedIn) "Cloud sync is on" else "Working offline",
                        subtitle = if (signedIn) "Cards, contacts & analytics sync to your account"
                        else "Sign in to sync and unlock contacts & insights",
                        icon = if (signedIn) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        tint = if (signedIn) ColorSync else ColorNeutral,
                        showChevron = false,
                        onClick = { if (!signedIn) vm.openAuth() },
                    )
                }
            }

            item { SectionLabel("General") }
            item {
                SettingsGroup {
                    SettingRow(
                        title = "Insights",
                        subtitle = "Views, taps and leads on your cards",
                        icon = Icons.Filled.BarChart,
                        tint = ColorInsights,
                        onClick = vm::openAnalytics,
                    )
                    RowDivider()
                    SettingRow(
                        title = "Share Tapcard",
                        subtitle = "Send your card link to anyone",
                        icon = Icons.Filled.Share,
                        tint = ColorShare,
                    ) {
                        val slug = vm.cards.firstOrNull()?.slug.orEmpty()
                        val link = if (slug.isNotBlank()) "${ApiConfig.PUBLIC_WEB_URL}/c/$slug" else ApiConfig.PUBLIC_WEB_URL
                        context.launchSafely(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Here's my digital card on Tapcard: $link")
                                },
                                "Share Tapcard",
                            ),
                        )
                    }
                    RowDivider()
                    SettingRow(title = "Help & support", icon = Icons.Filled.HelpOutline, tint = ColorHelp) {
                        // Local testing: points at the dev server. Switch to PUBLIC_WEB_URL for release.
                        context.launchSafely(
                            Intent(Intent.ACTION_VIEW, Uri.parse("${ApiConfig.BASE_URL}/help")),
                            "No browser available",
                        )
                    }
                    RowDivider()
                    SettingRow(title = "Privacy policy", icon = Icons.Filled.Shield, tint = ColorPrivacy) {
                        // Local testing: points at the dev server. Switch to PUBLIC_WEB_URL for release.
                        context.launchSafely(
                            Intent(Intent.ACTION_VIEW, Uri.parse("${ApiConfig.BASE_URL}/privacy")),
                            "No browser available",
                        )
                    }
                }
            }

            item { SectionLabel("Account") }
            item {
                SettingsGroup {
                    if (vm.isAuthenticated) {
                        SettingRow(
                            title = "Log out",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            showChevron = false,
                            onClick = vm::logout,
                        )
                        RowDivider()
                        SettingRow(
                            title = "Delete account",
                            subtitle = "Permanently remove your account & cards",
                            icon = Icons.Filled.DeleteForever,
                            danger = true,
                            showChevron = false,
                        ) { confirmDelete = true }
                    } else {
                        SettingRow(
                            title = "Sign in or create account",
                            icon = Icons.AutoMirrored.Filled.Login,
                            tint = ColorInsights,
                            onClick = vm::openAuth,
                        )
                    }
                }
            }

            item { SectionLabel("About") }
            item {
                SettingsGroup {
                    SettingRow(
                        title = "Tapcard",
                        subtitle = "Your smart digital business card",
                        icon = Icons.Filled.CreditCard,
                        tint = BrandBlue,
                        showChevron = false,
                        onClick = {},
                    )
                }
            }

            item {
                Text(
                    "Tapcard · v${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }
    }

    if (showDesign && myCard != null) {
        CardDesignSheet(
            card = myCard,
            onApply = { theme, accent -> vm.updateCardDesign(myCard, theme, accent) },
            onDismiss = { showDesign = false },
        )
    }

    if (showCountry) {
        CountryCodeDialog(
            selected = vm.settings.countryCode,
            onSelect = {
                vm.setCountryCode(it)
                showCountry = false
            },
            onDismiss = { showCountry = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete account?") },
            text = {
                Text("This permanently deletes your account, cards and analytics. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAccount()
                }) { Text("Delete", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AccountCard(vm: CardViewModel, onClick: () -> Unit) {
    val name = vm.authUserName ?: "Guest"
    val email = vm.authUserEmail ?: "Not signed in"
    val photo = vm.cards.firstOrNull()?.profilePhoto.orEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep)))
            .tappable(onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            if (photo.isNotBlank()) {
                AsyncImage(
                    model = CardImages.model(photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(email, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
        )
    }
}

/** Rounded, bordered container that groups a set of [SettingRow]s (iOS-style). */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(Radius.md)),
        content = content,
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    )
}

@Composable
private fun SettingRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = ColorNeutral,
    danger: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    val accent = if (danger) DangerRed else tint
    val titleColor = if (danger) DangerRed else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .fillMaxWidth()
            .tappable(onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon, accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = titleColor)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

/**
 * Pick a card style and accent with a live preview. Each tap applies (and, when
 * signed in, syncs to the web card) immediately — no separate save step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardDesignSheet(card: DigitalCard, onApply: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var theme by remember(card.id) { mutableStateOf(card.theme) }
    var accent by remember(card.id) { mutableStateOf(card.accentColor) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page)
                .padding(bottom = 24.dp),
        ) {
            Text("Card design", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(
                "Pick a style — it updates your live card instantly.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            CardFace(card.copy(theme = theme, accentColor = accent))
            Spacer(Modifier.height(20.dp))
            SectionLabel("Style")
            Spacer(Modifier.height(8.dp))
            ThemeGrid(selected = theme) {
                theme = it
                onApply(theme, accent)
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("Accent colour")
            Spacer(Modifier.height(8.dp))
            ColorPicker(selected = accent, themeKey = theme) {
                accent = it
                onApply(theme, accent)
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Done", onClick = onDismiss)
        }
    }
}

@Composable
private fun CountryCodeDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default country") },
        text = {
            LazyColumn(Modifier.height(360.dp)) {
                item {
                    Text(
                        "Added to numbers typed without a country code, so WhatsApp and calls connect.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                PhoneNumber.countries.forEach { (code, name, flag) ->
                    item(key = code) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .tappable { onSelect(code) }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(flag, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(name, Modifier.weight(1f), fontSize = 15.sp)
                            Text("+$code", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (code == selected) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = BrandBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
