package com.pamaz.vi.core.database

private val ACCENT_FOLD = mapOf(
    'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a',
    'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
    'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
    'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
    'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
    'ñ' to 'n', 'ç' to 'c', 'ý' to 'y', 'ÿ' to 'y',
)

/**
 * Lowercases and strips common Latin accents so searchKey columns can be matched with a plain
 * SQL LIKE without a native ICU/unaccent extension (not available across Android/iOS/JVM sqlite).
 */
internal fun String.foldForSearch(): String =
    lowercase().map { ACCENT_FOLD[it] ?: it }.joinToString("")
