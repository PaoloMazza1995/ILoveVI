package com.pamaz.vi.design

import androidx.compose.ui.graphics.Color

/**
 * Colors of the "Night" theme (the design system's only theme), from design/tokens.json → color.tokens.
 * Naming: camelCase of the token's "name" (bg-900 -> bg900, ink-muted -> inkMuted).
 */
object IloveViColors {
    val bg900 = Color(0xFF140A24)
    val bg800 = Color(0xFF22123A)
    val border = Color(0xFF3A2358)
    val ink = Color(0xFFFFF4E8)
    val inkMuted = Color(0xFFC4B3E0)
    val inkFaint = Color(0xFF9A85C2)
    val brandPink = Color(0xFFFF4D94)
    val brandOrange = Color(0xFFFF9A3D)
    val brandPurple = Color(0xFF8B8CF5)
    val brandTeal = Color(0xFF2FE6D2)
    val inkOnBrand = Color(0xFF140A24)
}

/**
 * The three game categories (Weapons/Vehicles/Items) always share the same color
 * wherever they appear (Chip, Badge, MapPin, ListRow, Card): exposed here as a single
 * reusable constant, instead of recreating the category -> color mapping in every component.
 */
enum class IloveViCategory(val color: Color, val label: String) {
    Weapons(IloveViColors.brandPink, "Weapons"),
    Vehicles(IloveViColors.brandOrange, "Vehicles"),
    Items(IloveViColors.brandTeal, "Items"),
}

/**
 * News topics (v1.1, NewsRow): `brand-pink` for News and `brand-purple` for Missions, per the
 * mockup. `brand-pink` is reused here as a *topic* color, distinct from its meaning as the
 * Weapons *category* color on the map/catalog (see [IloveViCategory]) — both map to the same
 * token because the mockup specifies it that way, not because the two concepts are related.
 */
enum class IloveViNewsTopic(val color: Color, val label: String) {
    News(IloveViColors.brandPink, "News"),
    Missions(IloveViColors.brandPurple, "Missions"),
}
