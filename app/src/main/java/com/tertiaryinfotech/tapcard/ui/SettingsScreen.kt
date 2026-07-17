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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.tapcard.net.ApiConfig
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
                    "Tapcard · v1.0",
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
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        }
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
