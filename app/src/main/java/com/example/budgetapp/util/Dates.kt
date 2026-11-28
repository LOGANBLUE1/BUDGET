package com.example.budgetapp.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object Dates {
    /** Date and time, for things that happen at a moment (closing a budget, a backup). */
    fun format(millis: Long): String =
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(millis))

    /** Date only, for expenses: their time of day isn't shown or edited. */
    fun formatDay(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))

    fun formatDay(day: LocalDate): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(day)

    fun localDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** Material's date picker works in UTC midnights. */
    fun toPickerMillis(day: LocalDate): Long = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    /**
     * The timestamp to store when an expense's day is set to [day].
     *
     * Only the day is chosen, so the time is made up, but in a way that keeps the order of
     * expenses sensible: the original timestamp if the day didn't change, "now" for today,
     * and noon for any other day.
     */
    fun timestampForDay(
        day: LocalDate,
        original: Long,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = when (day) {
        localDate(original, zone) -> original
        localDate(now, zone) -> now
        else -> day.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
    }
}
