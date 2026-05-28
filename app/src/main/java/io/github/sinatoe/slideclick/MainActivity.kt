package io.github.sinatoe.slideclick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.sinatoe.slideclick.clicker.ClickerScreen
import io.github.sinatoe.slideclick.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                ClickerScreen()
            }
        }
    }
}
