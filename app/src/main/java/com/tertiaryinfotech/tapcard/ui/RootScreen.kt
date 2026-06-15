package com.tertiaryinfotech.tapcard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.tertiaryinfotech.tapcard.model.AppScreen
import com.tertiaryinfotech.tapcard.vm.CardViewModel

/**
 * Top-level screen router. Switches between the four screens and hosts the
 * shared alert (mirrors the iOS RootView).
 */
@Composable
fun RootScreen(vm: CardViewModel) {
    // Hardware back returns to Home from any sub-screen.
    if (vm.screen != AppScreen.HOME) {
        BackHandler { vm.goHome() }
    }

    Crossfade(targetState = vm.screen, animationSpec = tween(250), label = "screen") { screen ->
        when (screen) {
            AppScreen.HOME -> HomeScreen(vm)
            AppScreen.SCAN -> ScanScreen(vm)
            AppScreen.REVIEW -> ReviewScreen(vm)
            AppScreen.CARD -> CardDetailScreen(vm)
        }
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
