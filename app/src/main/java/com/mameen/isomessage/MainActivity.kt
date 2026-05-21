package com.mameen.isomessage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mameen.isomessage.ui.navigation.AppNavigation
import com.mameen.isomessage.ui.theme.ISOMessageTheme
import com.mameen.isomessage.ui.theme.PosNavy900
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity — entry point for the ISO8583 POS Simulator app.
 *
 * @AndroidEntryPoint enables Hilt dependency injection in this Activity.
 * All @HiltViewModel ViewModels in the navigation graph are automatically
 * injected by Hilt when the composable screen is displayed.
 *
 * Architecture flow:
 *   MainActivity → AppNavigation → Screen composables → HiltViewModels → UseCases → Repository → API
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ISOMessageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PosNavy900
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
