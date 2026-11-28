package com.example.budgetapp.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DatesTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private val now = at("2026-11-28T13:30")

    @Test
    fun `keeps the original time when the day is unchanged`() {
        val original = at("2026-11-20T08:15")
        assertEquals(original, Dates.timestampForDay(LocalDate.of(2026, 11, 20), original, now, zone))
    }

    @Test
    fun `uses now when moved to today`() {
        val original = at("2026-11-20T08:15")
        assertEquals(now, Dates.timestampForDay(LocalDate.of(2026, 11, 28), original, now, zone))
    }

    @Test
    fun `uses noon for any other day`() {
        assertEquals(at("2026-11-25T12:00"), Dates.timestampForDay(LocalDate.of(2026, 11, 25), now, now, zone))
    }

    @Test
    fun `picker millis round trip`() {
        val day = LocalDate.of(2026, 2, 28)
        assertEquals(day, Dates.fromPickerMillis(Dates.toPickerMillis(day)))
    }
}
