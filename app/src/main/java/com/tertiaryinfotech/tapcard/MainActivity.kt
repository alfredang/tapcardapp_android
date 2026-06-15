package com.tertiaryinfotech.tapcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tertiaryinfotech.tapcard.ui.RootScreen
import com.tertiaryinfotech.tapcard.ui.theme.TapcardTheme
import com.tertiaryinfotech.tapcard.vm.CardViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapcardTheme {
                val vm: CardViewModel = viewModel()
                RootScreen(vm)
            }
        }
    }
}
