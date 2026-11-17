package com.pamaz.vi.design

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WCAG 2.x contrast-ratio check (relative luminance formula from the spec, §1.4.3/§1.4.11) over
 * every text/background and icon/border/large-text pair this step's Light/Night palettes actually
 * use (`Color.kt`'s component files, audited by hand while writing the Light palette). This is the
 * step's own acceptance criterion ("verify with a contrast check in a test over every text/
 * background token pair") — not a design-time sanity check, the real one.
 *
 * Minimums: 4.5:1 for normal text (WCAG AA §1.4.3), 3:1 for large text / icons / meaningful UI
 * borders (§1.4.3's large-text exception, §1.4.11 non-text contrast).
 */
class ColorContrastTest {

    private fun srgbChannelToLinear(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(color: Color): Double {
        val r = srgbChannelToLinear(color.red)
        val g = srgbChannelToLinear(color.green)
        val b = srgbChannelToLinear(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertTextContrast(
        theme: String,
        pairName: String,
        foreground: Color,
        background: Color
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            ratio >= 4.5,
            "$theme: $pairName contrast is $ratio, needs >= 4.5:1 for normal text",
        )
    }

    private fun assertLargeOrIconContrast(
        theme: String,
        pairName: String,
        foreground: Color,
        background: Color
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            ratio >= 3.0,
            "$theme: $pairName contrast is $ratio, needs >= 3:1 for large text/icons/borders",
        )
    }

    private fun assertEveryTextPair(theme: String, colors: ThemeColors) {
        assertTextContrast(theme, "ink/bg900", colors.ink, colors.bg900)
        assertTextContrast(theme, "ink/bg800", colors.ink, colors.bg800)
        assertTextContrast(theme, "inkMuted/bg900", colors.inkMuted, colors.bg900)
        assertTextContrast(theme, "inkMuted/bg800", colors.inkMuted, colors.bg800)
        assertTextContrast(theme, "brandPink/bg900", colors.brandPink, colors.bg900)
        assertTextContrast(theme, "brandPink/bg800", colors.brandPink, colors.bg800)
        assertTextContrast(theme, "brandOrange/bg900", colors.brandOrange, colors.bg900)
        assertTextContrast(theme, "brandOrange/bg800", colors.brandOrange, colors.bg800)
        assertTextContrast(theme, "brandPurple/bg900", colors.brandPurple, colors.bg900)
        assertTextContrast(theme, "brandPurple/bg800", colors.brandPurple, colors.bg800)
        assertTextContrast(theme, "brandTeal/bg900", colors.brandTeal, colors.bg900)
        assertTextContrast(theme, "brandTeal/bg800", colors.brandTeal, colors.bg800)
        assertTextContrast(theme, "warningYellow/bg900", colors.warningYellow, colors.bg900)
        assertTextContrast(theme, "warningYellow/bg800", colors.warningYellow, colors.bg800)
        // inkOnBrand sits on a solid brand fill (buttons, badges, the active switch thumb,
        // ClusterBadge's count text) — never on bg900/bg800 directly.
        assertTextContrast(theme, "inkOnBrand/brandPink", colors.inkOnBrand, colors.brandPink)
        assertTextContrast(theme, "inkOnBrand/brandOrange", colors.inkOnBrand, colors.brandOrange)
        assertTextContrast(theme, "inkOnBrand/brandPurple", colors.inkOnBrand, colors.brandPurple)
        assertTextContrast(theme, "inkOnBrand/brandTeal", colors.inkOnBrand, colors.brandTeal)
    }

    private fun assertEveryLargeOrIconPair(theme: String, colors: ThemeColors) {
        assertLargeOrIconContrast(theme, "inkFaint/bg900", colors.inkFaint, colors.bg900)
        assertLargeOrIconContrast(theme, "inkFaint/bg800", colors.inkFaint, colors.bg800)
    }

    @Test
    fun nightTextPairsMeetAA() = assertEveryTextPair("Night", NightColors)

    @Test
    fun nightLargeOrIconPairsMeetAA() = assertEveryLargeOrIconPair("Night", NightColors)

    @Test
    fun lightTextPairsMeetAA() = assertEveryTextPair("Light", LightColors)

    @Test
    fun lightLargeOrIconPairsMeetAA() = assertEveryLargeOrIconPair("Light", LightColors)

    /**
     * `border` is asserted separately, Light-only: Night's `border` (`#3A2358` on `#22123A` bg800,
     * ~1.3:1) was never designed to meet non-text contrast — it's a decorative divider between two
     * already-similar dark surfaces, pre-existing and unchanged by this step, not a focus/selection
     * indicator WCAG 1.4.11 would require 3:1 for. Light's `border` *was* deliberately chosen to
     * clear 3:1 against `bg800` (white surfaces, where a divider needs to actually be visible) —
     * that choice is what's under test here, not a retroactive Night requirement.
     */
    @Test
    fun lightBorderMeetsNonTextContrastAgainstSurfaces() {
        assertLargeOrIconContrast("Light", "border/bg800", LightColors.border, LightColors.bg800)
        assertLargeOrIconContrast("Light", "border/bg900", LightColors.border, LightColors.bg900)
    }
}
