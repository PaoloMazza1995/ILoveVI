package com.pamaz.vi.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ilovevi.core.design.generated.resources.Res
import ilovevi.core.design.generated.resources.anton_regular
import ilovevi.core.design.generated.resources.barlow_medium
import ilovevi.core.design.generated.resources.barlow_regular
import ilovevi.core.design.generated.resources.barlow_semibold
import org.jetbrains.compose.resources.Font

/**
 * Font families from design/tokens.json → type.families, loaded from the .ttf files in
 * core/design/src/commonMain/composeResources/font/ (see FONTS.md). `Font(...)` is
 * @Composable, so the families are built inside composition.
 */
object IloveViFontFamily {
    /** type.families.display ("Anton"): single weight, never synthesized bold. */
    @Composable
    fun display(): FontFamily = FontFamily(
        Font(Res.font.anton_regular, FontWeight.Normal),
    )

    /** type.families.sans ("Barlow"): only weights 400/500/600 exist. */
    @Composable
    fun sans(): FontFamily = FontFamily(
        Font(Res.font.barlow_regular, FontWeight.Normal),
        Font(Res.font.barlow_medium, FontWeight.Medium),
        Font(Res.font.barlow_semibold, FontWeight.SemiBold),
    )
}

/**
 * M3 typography built on the styles from design/tokens.json → type.groups (Display, Text).
 *
 * Mapping to Material3 slots (in decreasing size group order):
 * - "Display" group (Anton family) → the three M3 display* slots, 1:1:
 *     display-xl -> displayLarge, display-lg -> displayMedium, display-sm -> displaySmall
 * - "Text" group (Barlow family) → M3 slots chosen for closest semantic role:
 *     body-lg  (button/action text)         -> labelLarge  (M3 role for button text)
 *     body     (regular text/fields)        -> bodyLarge   (main text)
 *     body-md  (row title in lists)         -> titleMedium (M3 role for list item titles)
 *     label    (uppercase labels/filters)   -> labelMedium
 *     caption  (metadata/captions)          -> bodySmall   (short secondary text)
 *
 * M3 slots not covered by a token (displayMedium, headlineLarge/Medium/Small, titleLarge,
 * titleSmall, bodyMedium, labelSmall) stay at Typography()'s default values: no corresponding
 * tokens exist, so they are not invented.
 *
 * NavBar uses a 12sp/600 label under the icon ("caption-small" in the component's README)
 * which does NOT correspond to any tokens.json token (caption is 13sp/500): it's a literal
 * value taken from preview.html, applied locally in NavBar.kt and not exposed here as a reusable style.
 */
@Composable
fun rememberIloveViTypography(): Typography {
    val display = IloveViFontFamily.display()
    val sans = IloveViFontFamily.sans()
    return remember(display, sans) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = display,
                fontWeight = FontWeight.Normal,
                fontSynthesis = FontSynthesis.None,
                fontSize = 40.sp,
                lineHeight = 42.sp,
                letterSpacing = 1.sp,
            ),
            displayMedium = TextStyle(
                fontFamily = display,
                fontWeight = FontWeight.Normal,
                fontSynthesis = FontSynthesis.None,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                letterSpacing = 2.sp,
            ),
            displaySmall = TextStyle(
                fontFamily = display,
                fontWeight = FontWeight.Normal,
                fontSynthesis = FontSynthesis.None,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                letterSpacing = 2.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = sans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 1.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = sans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
        )
    }
}
