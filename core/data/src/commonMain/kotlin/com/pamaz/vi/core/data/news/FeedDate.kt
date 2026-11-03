package com.pamaz.vi.core.data.news

import kotlin.time.Instant

/**
 * RSS's `pubDate` is RFC 822 ("Mon, 03 Nov 2026 09:00:00 GMT"); Atom's `updated`/`published` is
 * ISO 8601 ("2026-11-03T08:45:00Z"). [kotlin.time.Instant.parse] only understands the latter, and
 * there's no `java.time`/`NSDate` available from `commonMain`, so RFC 822 gets its own small
 * hand-rolled parser below rather than pulling in a platform date-formatting dependency for one
 * field. Returns `null` (never throws) for anything that doesn't look like either format, so
 * callers can fall back to "now", same as the project's earlier TypeScript `fetch-news` job did.
 */
internal fun parseFeedDate(text: String?): Instant? {
    if (text.isNullOrBlank()) return null
    val trimmed = text.trim()
    return try {
        Instant.parse(trimmed)
    } catch (e: Exception) {
        parseRfc822Date(trimmed)
    }
}

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun parseRfc822Date(text: String): Instant? {
    // Drop an optional leading "Mon, " weekday name.
    val comma = text.indexOf(',')
    val withoutWeekday = (if (comma in 0..4) text.substring(comma + 1) else text).trim()

    val parts = withoutWeekday.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size < 4) return null

    val day = parts[0].toIntOrNull() ?: return null
    val monthIndex = MONTHS.indexOfFirst { it.equals(parts[1].take(3), ignoreCase = true) }
    if (monthIndex < 0) return null
    val yearRaw = parts[2].toIntOrNull() ?: return null
    val year = if (yearRaw < 100) yearRaw + 1900 else yearRaw

    val timeParts = parts[3].split(":")
    if (timeParts.size < 2) return null
    val hour = timeParts[0].toIntOrNull() ?: return null
    val minute = timeParts[1].toIntOrNull() ?: return null
    val second = timeParts.getOrNull(2)?.toIntOrNull() ?: 0

    val offsetSeconds = if (parts.size > 4) parseTimezoneOffsetSeconds(parts[4]) else 0

    val days = daysFromCivil(year, monthIndex + 1, day)
    val epochSeconds = days * 86_400L + hour * 3_600L + minute * 60L + second - offsetSeconds
    return try {
        Instant.fromEpochSeconds(epochSeconds)
    } catch (e: Exception) {
        null
    }
}

/** Numeric `+HHMM`/`-HHMM` offsets are honored; `GMT`/`UT`/`UTC`/`Z` and any unrecognized named zone fall back to 0 — permissive, matching this parser's overall "good enough for real feeds" scope. */
private fun parseTimezoneOffsetSeconds(zone: String): Int {
    if (zone.isEmpty()) return 0
    if (zone.equals("GMT", true) || zone.equals("UT", true) || zone.equals(
            "UTC",
            true
        ) || zone == "Z"
    ) return 0
    val sign = when (zone[0]) {
        '+' -> 1
        '-' -> -1
        else -> return 0
    }
    val digits = zone.drop(1)
    if (digits.length != 4 || digits.any { !it.isDigit() }) return 0
    val hh = digits.substring(0, 2).toInt()
    val mm = digits.substring(2, 4).toInt()
    return sign * (hh * 3_600 + mm * 60)
}

/** Howard Hinnant's "days from civil" algorithm (proleptic Gregorian), days since 1970-01-01 — the one calendar/epoch conversion this file needs, with no platform date library available from `commonMain`. */
private fun daysFromCivil(y: Int, m: Int, d: Int): Long {
    val yy = (if (m <= 2) y - 1 else y).toLong()
    val era = (if (yy >= 0) yy else yy - 399) / 400
    val yoe = yy - era * 400 // [0, 399]
    val doy = (153L * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1 // [0, 365]
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy // [0, 146096]
    return era * 146_097L + doe - 719_468L
}
