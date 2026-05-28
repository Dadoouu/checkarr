package com.checkarr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.checkarr.ui.MainViewModel
import com.checkarr.ui.RuddarrApp
import com.checkarr.ui.theme.RuddarrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val theme by mainViewModel.theme.collectAsState()
            val accentColor by mainViewModel.accentColor.collectAsState()
            RuddarrTheme(
                darkTheme = when (theme) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                },
                accentColorName = accentColor
            ) {
                RuddarrApp(viewModel = mainViewModel)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun isSystemInDarkTheme(): Boolean =
        androidx.compose.foundation.isSystemInDarkTheme()
}
