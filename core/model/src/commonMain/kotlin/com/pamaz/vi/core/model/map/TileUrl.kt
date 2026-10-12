package com.pamaz.vi.core.model.map

/**
 * Builds concrete tile URLs from a [MapConfig.tileUrlTemplate][com.pamaz.vi.core.model.MapConfig]
 * -style template containing the `{z}`, `{x}` and `{y}` placeholders.
 */
object TileUrl {
    /**
     * Substitutes [z]/[x]/[y] into [template]'s `{z}`/`{x}`/`{y}` placeholders. Throws
     * [IllegalArgumentException] if any placeholder is missing, since a template missing one of
     * the three would otherwise silently fetch the same tile for every coordinate on that axis.
     */
    fun build(template: String, z: Int, x: Int, y: Int): String {
        require("{z}" in template && "{x}" in template && "{y}" in template) {
            "template must contain {z}, {x} and {y} placeholders, was \"$template\""
        }
        return template
            .replace("{z}", z.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }
}
