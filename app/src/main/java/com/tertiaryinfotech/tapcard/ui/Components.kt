package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily

// ─── Design tokens ─────────────────────────────────────────────────────────────
object Spacing {
    val page = 20.dp
    val section = 12.dp
}

object Radius {
    val sm = 14.dp
    val md = 18.dp
    val lg = 24.dp
    val pill = 999.dp
}

// ─── Ambient background ─────────────────────────────────────────────────────────
/** Soft brand-tinted mesh blobs over the canvas so screens don't feel flat/empty. */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            Modifier
                .size(360.dp)
                .offset(x = 160.dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(listOf(BrandBlue.copy(alpha = 0.16f), Color.Transparent)),
                ),
        )
        Box(
            Modifier
                .size(320.dp)
                .offset(x = (-140).dp, y = 80.dp)
                .background(
                    Brush.radialGradient(listOf(BrandBlue.copy(alpha = 0.10f), Color.Transparent)),
                ),
        )
    }
}

// ─── Section label ──────────────────────────────────────────────────────────────
/** Quiet uppercase group heading used across the app's scrolling screens. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

// ─── Large screen title ─────────────────────────────────────────────────────────
/** Big bold large-title header (+ optional subtitle) for the top of a screen. */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Text(
            title,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

// ─── Primary CTA ────────────────────────────────────────────────────────────────
/** Full-width blue gradient call-to-action with an optional icon and busy spinner. */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val brush = Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep))
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(if (enabled) 12.dp else 0.dp, RoundedCornerShape(Radius.md), spotColor = BrandBlue, ambientColor = BrandBlue)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color(0xFFB9C2D6), Color(0xFFB9C2D6))))
            .tappable { if (enabled && !loading) onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ─── Secondary / outlined action ─────────────────────────────────────────────────
/** Blue-outlined pill for secondary actions and tools. */
@Composable
fun OutlinedActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(Radius.md))
            .border(1.5.dp, BrandBlue.copy(alpha = 0.5f), RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .tappable(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, color = BrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ─── Action row ───────────────────────────────────────────────────────────────
/**
 * Blinq-style list row: tinted leading icon chip + title (+ optional subtitle) +
 * trailing chevron, on a soft surface card. The core building block of the card
 * detail actions and tool lists.
 */
@Composable
fun ActionRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = BrandBlue,
    trailingChevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(Radius.md))
            .tappable(onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailingChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Signed-out state ───────────────────────────────────────────────────────────
/** Full-screen prompt shown on tabs that need an account (Contacts, Insights). */
@Composable
fun SignedOutState(
    icon: ImageVector,
    title: String,
    message: String,
    onSignIn: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(Radius.lg))
                .background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Sign in", onClick = onSignIn)
    }
}

