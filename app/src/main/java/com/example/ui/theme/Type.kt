package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography().copy(
    displayLarge = Typography().displayLarge.copy(fontFamily = PromptFontFamily),
    displayMedium = Typography().displayMedium.copy(fontFamily = PromptFontFamily),
    displaySmall = Typography().displaySmall.copy(fontFamily = PromptFontFamily),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = PromptFontFamily),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = PromptFontFamily),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = PromptFontFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = PromptFontFamily),
    titleMedium = Typography().titleMedium.copy(fontFamily = PromptFontFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = PromptFontFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = PromptFontFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = PromptFontFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = PromptFontFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = PromptFontFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = PromptFontFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = PromptFontFamily)
)
