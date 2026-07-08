package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: CardViewModel) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item { AccountCard(vm) }

            item { SectionLabel("Sync") }
            item {
                val signedIn = vm.isAuthenticated
                ActionRow(
                    title = if (signedIn) "Cloud sync is on" else "Working offline",
                    subtitle = if (signedIn) "Cards, contacts & analytics sync to your account" else "Sign in to sync and unlock contacts & insights",
                    icon = if (signedIn) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    tint = if (signedIn) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    trailingChevron = false,
                    onClick = { if (!signedIn) vm.openAuth() },
                )
            }

            item { SectionLabel("Account") }
            item {
                if (vm.isAuthenticated) {
                    ActionRow(
                        title = "Log out",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        tint = Color(0xFFDC2626),
                        trailingChevron = false,
                        onClick = vm::logout,
                    )
                } else {
                    ActionRow(
                        title = "Sign in or create account",
                        icon = Icons.AutoMirrored.Filled.Login,
                        onClick = vm::openAuth,
                    )
                }
            }

            item { SectionLabel("About") }
            item {
                ActionRow(
                    title = "Tapcard",
                    subtitle = "Your smart digital business card",
                    icon = Icons.Filled.Person,
                    trailingChevron = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun AccountCard(vm: CardViewModel) {
    val name = vm.authUserName ?: "Guest"
    val email = vm.authUserEmail ?: "Not signed in"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep)))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(email, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}
