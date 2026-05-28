package io.github.sinatoe.slideclick.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = rememberDynamicColorScheme(
            isDark = true,
            isAmoled = true,
            primary = Color(0x000EA5E9),
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        ),
        content = content,
    )
}
