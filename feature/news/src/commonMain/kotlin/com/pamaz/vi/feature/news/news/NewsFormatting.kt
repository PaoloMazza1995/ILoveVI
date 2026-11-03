package com.pamaz.vi.feature.news.news

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** "just now" / "12m ago" / "3h ago" / "5d ago" relative to now — no locale/date-formatting library wired into this module, so this stays in whole minutes/hours/days rather than switching to calendar dates for older items. */
internal fun relativeTimeText(instant: Instant, now: Instant = Clock.System.now()): String {
    val elapsed = now - instant
    return when {
        elapsed < 1.minutes -> "just now"
        elapsed < 1.hours -> "${elapsed.inWholeMinutes}m ago"
        elapsed < 1.days -> "${elapsed.inWholeHours}h ago"
        else -> "${elapsed.inWholeDays}d ago"
    }
}
