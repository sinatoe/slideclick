package io.github.sinatoe.slideclick.ui

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
            primary = Color(0xFF0EA5E9),
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        ),
        content = content,
    )
}
