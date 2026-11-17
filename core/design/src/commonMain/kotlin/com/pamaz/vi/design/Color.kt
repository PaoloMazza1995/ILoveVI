package com.pamaz.vi.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One theme's full set of color tokens — semantic roles, not raw palette names, so every
 * component that reads [IloveViColors] never has to know which theme is active.
 *
 * [featuredGradientStart] is the one token with no entry in design/tokens.json: it backs
 * [com.pamaz.vi.design.components.IloveViFeaturedCard]'s gradient, previously a bare literal
 * (`Color(0xFF3A1B6B)`) flagged in that file's own KDoc as "should ideally be promoted to a
 * token" — promoted here as part of this step's "no component may use a hard-coded color" audit,
 * since a literal can't vary by theme.
 */
data class ThemeColors(
    val bg900: Color,
    val bg800: Color,
    val border: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val brandPink: Color,
    val brandOrange: Color,
    val brandPurple: Color,
    val brandTeal: Color,
    val inkOnBrand: Color,
    val warningYellow: Color,
    val featuredGradientStart: Color,
)

/** Night — design/tokens.json → color.tokens' only theme until this step. Values unchanged. */
val NightColors = ThemeColors(
    bg900 = Color(0xFF140A24),
    bg800 = Color(0xFF22123A),
    border = Color(0xFF3A2358),
    ink = Color(0xFFFFF4E8),
    inkMuted = Color(0xFFC4B3E0),
    inkFaint = Color(0xFF9A85C2),
    brandPink = Color(0xFFFF4D94),
    brandOrange = Color(0xFFFF9A3D),
    brandPurple = Color(0xFF8B8CF5),
    brandTeal = Color(0xFF2FE6D2),
    inkOnBrand = Color(0xFF140A24),
    warningYellow = Color(0xFFFFD84D),
    featuredGradientStart = Color(0xFF3A1B6B),
)

/**
 * Light — new in this step. Warm off-white background, white surfaces, dark violet text, the
 * brand pink, category colors darkened where the Night value doesn't clear WCAG AA on a light
 * background. Every text/background pair here is asserted in `core/design/src/commonTest/.../
 * ColorContrastTest.kt` (>=4.5:1 for text, >=3:1 for icons/large text/borders) — see that file and
 * `core/design/README.md`'s "Light theme" section for exactly which values had to move, and why,
 * from a naive "just lighten Night" guess.
 *
 * [brandOrange]/[brandTeal] in particular needed real darkening (not just "a shade"):
 * Night's `#FF9A3D`/`#2FE6D2` are both <3:1 against white, nowhere near the 4.5:1 a body-text or
 * icon-adjacent-label use needs. [inkOnBrand] flips from Night's near-black to white, since every
 * brand color here is now dark enough that white text/icons sit on top of it, not dark ones.
 */
val LightColors = ThemeColors(
    bg900 = Color(0xFFFBF2E8),
    bg800 = Color(0xFFFFFFFF),
    border = Color(0xFF9480A8),
    ink = Color(0xFF23132F),
    inkMuted = Color(0xFF5B4870),
    inkFaint = Color(0xFF8678A0),
    brandPink = Color(0xFFD1266E),
    brandOrange = Color(0xFFA04E09),
    brandPurple = Color(0xFF5B4FD6),
    brandTeal = Color(0xFF0B6E61),
    inkOnBrand = Color(0xFFFFFFFF),
    warningYellow = Color(0xFF8A6200),
    featuredGradientStart = Color(0xFFE3D6F5),
)

/**
 * The active theme's colors, provided by [IloveViTheme]. Defaults to [NightColors] so a bare
 * `@Preview` that doesn't wrap [IloveViTheme] (none should exist, but this is a cheap safety net)
 * renders instead of crashing.
 */
val LocalIloveViColors = staticCompositionLocalOf { NightColors }

/**
 * Choice of token exposure, revised for this step (was: "plain Kotlin object, since
 * design/tokens.json defines only ONE theme"). Now backed by [LocalIloveViColors] instead of
 * constants, so every existing `IloveViColors.xxx` call site across every component keeps
 * compiling unchanged while resolving the active theme dynamically — the whole point of this
 * rewrite is that no component file needed to change for Light/Dark/System support.
 */
object IloveViColors {
    val bg900: Color @Composable get() = LocalIloveViColors.current.bg900
    val bg800: Color @Composable get() = LocalIloveViColors.current.bg800
    val border: Color @Composable get() = LocalIloveViColors.current.border
    val ink: Color @Composable get() = LocalIloveViColors.current.ink
    val inkMuted: Color @Composable get() = LocalIloveViColors.current.inkMuted
    val inkFaint: Color @Composable get() = LocalIloveViColors.current.inkFaint
    val brandPink: Color @Composable get() = LocalIloveViColors.current.brandPink
    val brandOrange: Color @Composable get() = LocalIloveViColors.current.brandOrange
    val brandPurple: Color @Composable get() = LocalIloveViColors.current.brandPurple
    val brandTeal: Color @Composable get() = LocalIloveViColors.current.brandTeal
    val inkOnBrand: Color @Composable get() = LocalIloveViColors.current.inkOnBrand
    val warningYellow: Color @Composable get() = LocalIloveViColors.current.warningYellow
    val featuredGradientStart: Color @Composable get() = LocalIloveViColors.current.featuredGradientStart
}

/**
 * The three game categories (Weapons/Vehicles/Items) always share the same color
 * wherever they appear (Chip, Badge, MapPin, ListRow, Card): exposed here as a single
 * reusable constant, instead of recreating the category -> color mapping in every component.
 * `color` reads the active theme the same way [IloveViColors] does.
 */
enum class IloveViCategory(val label: String) {
    Weapons("Weapons"),
    Vehicles("Vehicles"),
    Items("Items"),
    ;

    val color: Color
        @Composable get() = when (this) {
            Weapons -> IloveViColors.brandPink
            Vehicles -> IloveViColors.brandOrange
            Items -> IloveViColors.brandTeal
        }
}

/**
 * News topics (v1.1, NewsRow): `brand-pink` for News and `brand-purple` for Missions, per the
 * mockup. `brand-pink` is reused here as a *topic* color, distinct from its meaning as the
 * Weapons *category* color on the map/catalog (see [IloveViCategory]) — both map to the same
 * token because the mockup specifies it that way, not because the two concepts are related.
 */
enum class IloveViNewsTopic(val label: String) {
    News("News"),
    Missions("Missions"),
    ;

    val color: Color
        @Composable get() = when (this) {
            News -> IloveViColors.brandPink
            Missions -> IloveViColors.brandPurple
        }
}
