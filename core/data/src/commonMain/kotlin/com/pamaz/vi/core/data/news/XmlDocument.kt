package com.pamaz.vi.core.data.news

/**
 * A minimal, permissive, hand-rolled XML tree parser — pure `commonMain` Kotlin (string scanning
 * only), so it needs no platform-specific `expect`/`actual` engine and no third-party KMP XML
 * dependency whose JVM/Android/iOS targets would otherwise need vetting. It is deliberately scoped
 * to "good enough for real-world RSS 2.0/Atom feeds", not a validating, spec-complete XML parser:
 * namespaces are kept as literal tag/attribute names (`media:content` stays `"media:content"`,
 * never resolved against its `xmlns:media` declaration) exactly like the project's earlier
 * TypeScript `fetch-news` job (`fast-xml-parser`, used without namespace resolution) did.
 */
internal class XmlParseException(message: String) : Exception(message)

internal sealed interface XmlNode

internal data class XmlText(val value: String) : XmlNode

internal data class XmlElement(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val children: List<XmlNode> = emptyList(),
) : XmlNode {
    fun child(name: String): XmlElement? =
        children.firstOrNull { it is XmlElement && it.name == name } as XmlElement?

    fun children(name: String): List<XmlElement> =
        children.filterIsInstance<XmlElement>().filter { it.name == name }

    fun attr(name: String): String? = attributes[name]

    /** Direct text children only, trimmed — for simple scalar fields (title, link, pubDate, guid). `null` if empty. */
    fun directText(): String? {
        val text = children.filterIsInstance<XmlText>().joinToString("") { it.value }.trim()
        return text.ifEmpty { null }
    }

    /**
     * All text under this subtree, document order, descending into child elements too — the one
     * extraction rule that makes description/content:encoded/summary/content work whether the
     * feed wrapped its HTML in CDATA (one single text child) or embedded it as raw, un-escaped
     * child elements (Google News' own `<a>headline</a>&nbsp;<font>source</font>` convention):
     * no special-casing needed either way, unlike the project's earlier TypeScript `fetch-news`
     * job, which needed fast-xml-parser's `stopNodes` option to work around exactly this.
     */
    fun allText(): String {
        val sb = StringBuilder()
        fun walk(node: XmlNode) {
            when (node) {
                is XmlText -> sb.append(node.value)
                is XmlElement -> node.children.forEach(::walk)
            }
        }
        children.forEach(::walk)
        return sb.toString()
    }
}

/** Parses [xml], returning its single root element (`<rss>` or `<feed>`) — prolog/doctype/comments are skipped. */
internal fun parseXmlDocument(xml: String): XmlElement {
    val root = XmlTokenizer(xml).parseDocument()
    return root ?: throw XmlParseException("empty document")
}

private class XmlTokenizer(private val input: String) {
    private var pos = 0
    private val len = input.length

    fun parseDocument(): XmlElement? =
        parseNodes(parentName = null).filterIsInstance<XmlElement>().firstOrNull()

    /**
     * Reads sibling nodes until EOF (top level, [parentName] `null`) or until it consumes the
     * matching `</parentName>` close tag (a nested call) — recursion depth mirrors XML nesting
     * depth, which is shallow (well under 10) for every real RSS/Atom feed.
     */
    private fun parseNodes(parentName: String?): List<XmlNode> {
        val nodes = mutableListOf<XmlNode>()
        while (pos < len) {
            when {
                input.startsWith("<!--", pos) -> skipComment()
                input.startsWith("<![CDATA[", pos) -> nodes.add(readCData())
                input.startsWith("<?", pos) -> skipDelimited("?>")
                input.startsWith("<!", pos) -> skipDelimited(">")
                input.startsWith("</", pos) -> {
                    val closeName = readCloseTagName()
                    if (parentName != null && closeName != parentName) {
                        throw XmlParseException("mismatched close tag </$closeName>, expected </$parentName>")
                    }
                    return nodes
                }

                input[pos] == '<' -> nodes.add(readElement())
                else -> {
                    val text = readText()
                    if (text.isNotEmpty()) nodes.add(XmlText(text))
                }
            }
        }
        if (parentName != null) throw XmlParseException("unterminated element <$parentName>")
        return nodes
    }

    private fun skipComment() {
        val end = input.indexOf("-->", pos + 4)
        if (end < 0) throw XmlParseException("unterminated comment")
        pos = end + 3
    }

    private fun skipDelimited(terminator: String) {
        val end = input.indexOf(terminator, pos)
        if (end < 0) throw XmlParseException("unterminated declaration (expected \"$terminator\")")
        pos = end + terminator.length
    }

    private fun readCData(): XmlText {
        val start = pos + 9 // "<![CDATA[".length
        val end = input.indexOf("]]>", start)
        if (end < 0) throw XmlParseException("unterminated CDATA section")
        pos = end + 3
        // CDATA content is never entity-decoded — that's the whole point of CDATA.
        return XmlText(input.substring(start, end))
    }

    private fun readCloseTagName(): String {
        val start = pos + 2 // "</"
        val end = input.indexOf('>', start)
        if (end < 0) throw XmlParseException("unterminated close tag")
        pos = end + 1
        val raw = input.substring(start, end).trim()
        val wsIndex = raw.indexOfFirst { it.isWhitespace() }
        return if (wsIndex >= 0) raw.substring(0, wsIndex) else raw
    }

    private fun readText(): String {
        val start = pos
        val end = input.indexOf('<', pos).let { if (it < 0) len else it }
        pos = end
        return decodeEntities(input.substring(start, end))
    }

    private fun readElement(): XmlElement {
        val tagStart = pos + 1
        var i = tagStart
        while (i < len && !isNameEnd(input[i])) i++
        val name = input.substring(tagStart, i)
        if (name.isEmpty()) throw XmlParseException("malformed tag at position $pos")
        pos = i

        val attributes = readAttributes()

        skipWhitespace()
        if (pos >= len) throw XmlParseException("unterminated tag <$name>")

        if (input[pos] == '/') {
            if (pos + 1 >= len || input[pos + 1] != '>') {
                throw XmlParseException("malformed self-closing tag <$name>")
            }
            pos += 2
            return XmlElement(name, attributes, emptyList())
        }
        if (input[pos] != '>') throw XmlParseException("malformed tag <$name>")
        pos += 1 // consume '>'

        val children = parseNodes(parentName = name)
        return XmlElement(name, attributes, children)
    }

    private fun isNameEnd(c: Char) = c.isWhitespace() || c == '>' || c == '/'

    private fun readAttributes(): Map<String, String> {
        val attrs = LinkedHashMap<String, String>()
        while (true) {
            skipWhitespace()
            if (pos >= len) throw XmlParseException("unterminated tag")
            val c = input[pos]
            if (c == '>' || c == '/') return attrs

            val nameStart = pos
            while (pos < len && input[pos] != '=' && !input[pos].isWhitespace() &&
                input[pos] != '>' && input[pos] != '/'
            ) {
                pos++
            }
            val attrName = input.substring(nameStart, pos)
            skipWhitespace()

            if (pos < len && input[pos] == '=') {
                pos++ // consume '='
                skipWhitespace()
                if (pos >= len) throw XmlParseException("malformed attribute value")
                val quote = input[pos]
                if (quote != '"' && quote != '\'') throw XmlParseException("attribute value must be quoted")
                pos++
                val valueStart = pos
                val valueEnd = input.indexOf(quote, pos)
                if (valueEnd < 0) throw XmlParseException("unterminated attribute value")
                pos = valueEnd + 1
                if (attrName.isNotEmpty()) attrs[attrName] =
                    decodeEntities(input.substring(valueStart, valueEnd))
            } else if (attrName.isEmpty()) {
                pos++ // stray character; avoid an infinite loop
            }
            // else: a valueless attribute (not valid XML, but harmless to just skip).
        }
    }

    private fun skipWhitespace() {
        while (pos < len && input[pos].isWhitespace()) pos++
    }
}

/**
 * Decodes the five predefined XML entities, numeric character references, and `&nbsp;` (common in
 * real feeds despite not being valid in bare XML without a DTD) — any other named entity (e.g.
 * `&mdash;`) is left as literal text, the same scope the project's earlier TypeScript
 * `fetch-news` job's `html.ts` covered.
 */
internal fun decodeEntities(text: String): String {
    if ('&' !in text) return text
    val sb = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c != '&') {
            sb.append(c)
            i++
            continue
        }
        val semi = text.indexOf(';', i + 1)
        if (semi < 0 || semi - i > 10) {
            sb.append(c)
            i++
            continue
        }
        val entity = text.substring(i + 1, semi)
        val decoded = when {
            entity == "lt" -> "<"
            entity == "gt" -> ">"
            entity == "amp" -> "&"
            entity == "quot" -> "\""
            entity == "apos" -> "'"
            entity == "nbsp" -> "\u0020"
            entity.startsWith("#x") || entity.startsWith("#X") -> entity.drop(2).toIntOrNull(16)
                ?.let(::codePointToString)

            entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let(::codePointToString)
            else -> null
        }
        if (decoded != null) {
            sb.append(decoded)
            i = semi + 1
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

private fun codePointToString(codePoint: Int): String? {
    if (codePoint !in 0..0x10FFFF) return null
    if (codePoint <= 0xFFFF) return codePoint.toChar().toString()
    val cp = codePoint - 0x10000
    val high = (cp shr 10) + 0xD800
    val low = (cp and 0x3FF) + 0xDC00
    return charArrayOf(high.toChar(), low.toChar()).concatToString()
}
