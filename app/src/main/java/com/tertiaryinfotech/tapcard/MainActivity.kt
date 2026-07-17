package com.tertiaryinfotech.tapcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tertiaryinfotech.tapcard.ui.AuthScreen
import com.tertiaryinfotech.tapcard.ui.LoadingScreen
import com.tertiaryinfotech.tapcard.ui.RootScreen
import com.tertiaryinfotech.tapcard.ui.WelcomeScreen
import com.tertiaryinfotech.tapcard.ui.theme.TapcardTheme
import com.tertiaryinfotech.tapcard.vm.CardViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapcardTheme {
                val vm: CardViewModel = viewModel()
                when {
                    vm.isBooting -> LoadingScreen()
                    vm.showAuth && vm.authLanding -> WelcomeScreen(vm)
                    vm.showAuth -> AuthScreen(vm)
                    else -> RootScreen(vm)
                }
            }
        }
    }
}
