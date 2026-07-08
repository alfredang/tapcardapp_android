package com.tertiaryinfotech.tapcard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.AppScreen
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueTint
import com.tertiaryinfotech.tapcard.vm.CardViewModel

private val TabRoots = setOf(
    AppScreen.HOME, AppScreen.CONTACTS, AppScreen.ANALYTICS, AppScreen.SETTINGS,
)

/**
 * Top-level shell. A persistent bottom tab bar (Blinq-style) is shown on the tab
 * root screens; the scan / review / card-detail flows push full-screen over it.
 * The share sheet is hosted here so it can overlay any tab.
 */
@Composable
fun RootScreen(vm: CardViewModel) {
    if (vm.screen != AppScreen.HOME) {
        BackHandler { vm.goHome() }
    }

    val showBar = vm.screen in TabRoots

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBar) TapBottomBar(vm) },
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = vm.screen,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val bothTabs = targetState in TabRoots && initialState in TabRoots
                if (bothTabs) {
                    // Quiet cross-fade when switching between tabs.
                    fadeIn(tween(200)).togetherWith(fadeOut(tween(150)))
                } else {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(320)) { w -> dir * w / 5 } + fadeIn(tween(240)))
                        .togetherWith(
                            slideOutHorizontally(tween(320)) { w -> -dir * w / 5 } + fadeOut(tween(200)),
                        )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = scaffoldPadding.calculateBottomPadding()),
            label = "screen",
        ) { screen ->
            when (screen) {
                AppScreen.HOME -> HomeScreen(vm)
                AppScreen.SCAN -> ScanScreen(vm)
                AppScreen.REVIEW -> ReviewScreen(vm)
                AppScreen.CARD -> CardDetailScreen(vm)
                AppScreen.CONTACTS -> ContactsScreen(vm)
                AppScreen.ANALYTICS -> AnalyticsScreen(vm)
                AppScreen.SETTINGS -> SettingsScreen(vm)
            }
        }
    }

    vm.shareCard?.let { card ->
        ShareSheet(card = card, onDismiss = vm::dismissShare)
    }

    vm.activeAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { vm.activeAlert = null },
            title = { Text(alert.title) },
            text = { Text(alert.message) },
            confirmButton = { TextButton(onClick = { vm.activeAlert = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun TapBottomBar(vm: CardViewModel) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        TabItem(vm, AppScreen.HOME, "My Card", Icons.Filled.CreditCard) { vm.goHome() }
        TabItem(vm, AppScreen.CONTACTS, "Contacts", Icons.Filled.Groups) { vm.openContacts() }
        TabItem(vm, AppScreen.ANALYTICS, "Insights", Icons.Filled.BarChart) { vm.openAnalytics() }
        TabItem(vm, AppScreen.SETTINGS, "Settings", Icons.Filled.Person) { vm.openSettings() }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    vm: CardViewModel,
    target: AppScreen,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val selected = vm.screen == target
    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) onClick() },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = BrandBlue,
            selectedTextColor = BrandBlue,
            indicatorColor = BrandBlueTint,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}