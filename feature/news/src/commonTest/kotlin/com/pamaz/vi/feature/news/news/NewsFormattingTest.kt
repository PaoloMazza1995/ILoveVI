package com.pamaz.vi.feature.news.news

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class NewsFormattingTest {
    private val now = Instant.parse("2026-01-02T12:00:00Z")

    @Test
    fun underAMinute_isJustNow() {
        assertEquals("just now", relativeTimeText(now, now))
    }

    @Test
    fun minutesAgo() {
        assertEquals("12m ago", relativeTimeText(now - 12.minutes, now))
    }

    @Test
    fun hoursAgo() {
        assertEquals("3h ago", relativeTimeText(now - 3.hours, now))
    }

    @Test
    fun daysAgo() {
        assertEquals("5d ago", relativeTimeText(now - 5.days, now))
    }
}
