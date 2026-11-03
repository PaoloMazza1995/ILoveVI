package com.pamaz.vi.core.data.news

/**
 * Turns a feed's (often HTML-laden) description/summary/content into the short, plain-text
 * excerpt actually stored locally — ported from the project's earlier TypeScript `fetch-news`
 * job's `html.ts`. [XmlDocument.allText] already turns *real* child elements (CDATA or raw,
 * un-escaped markup alike) into plain concatenated text, so the only markup left to deal with
 * here is literal `<tag>`-looking substrings: either from inside a CDATA block (never
 * XML-entity-decoded, by definition) or from HTML that a feed escaped as entities
 * (`&lt;p&gt;...&lt;/p&gt;`, e.g. Atom's `content type="html"` convention, or the real Google
 * News feed this was verified live against) — [decodeEntities] already turned those back into
 * literal `<`/`>` characters while tokenizing, so both cases end up as ordinary `<tag>` text by
 * the time it reaches this function.
 *
 * A second [decodeEntities] pass runs *after* stripping tags: Google News' real feed was found
 * (via a live fetch, not just its own fixtures) to double-escape `&nbsp;` as `&amp;nbsp;` inside
 * an already-`&lt;`/`&gt;`-escaped description — the tokenizer's one decode pass only resolves it
 * one level, to the still-literal text `&nbsp;`, so without this second pass the excerpt would
 * contain literal `&nbsp;` text instead of a space.
 */
private const val EXCERPT_MAX_CHARS = 200

internal fun stripHtml(html: String): String {
    var text = html
    text = Regex("(?i)<(script|style)[^>]*>[\\s\\S]*?</\\1>").replace(text, " ")
    text = Regex("<[^>]+>").replace(text, " ")
    text = decodeEntities(text)
    text = Regex("\\s+").replace(text, " ")
    return text.trim()
}

/**
 * [stripHtml]'s output, truncated to [EXCERPT_MAX_CHARS] — a short plain-text excerpt, never the
 * full article. Breaks at the last whole word that still fits, rather than mid-word, and appends
 * "…"; returns `null` for empty/blank input so a feed with no real description produces no
 * excerpt at all, not an empty string.
 */
internal fun toExcerpt(rawHtml: String?): String? {
    if (rawHtml.isNullOrEmpty()) return null
    val plain = stripHtml(rawHtml)
    if (plain.isEmpty()) return null
    if (plain.length <= EXCERPT_MAX_CHARS) return plain

    val cut = plain.substring(0, EXCERPT_MAX_CHARS)
    val lastSpace = cut.lastIndexOf(' ')
    val trimmed = (if (lastSpace > 0) cut.substring(0, lastSpace) else cut).trimEnd()
    return "$trimmed…"
}
