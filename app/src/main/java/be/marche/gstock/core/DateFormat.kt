package be.marche.gstock.core

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Parses an ISO-8601 date string from the Laravel backend (e.g. "2026-06-11T10:15:30.000000Z")
 * and formats it as a human-readable date/time in the given [locale] and the device time zone.
 * Falls back to the raw string if it cannot be parsed.
 */
fun String.toReadableDateTime(locale: Locale): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
    return try {
        OffsetDateTime.parse(this)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(formatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(this).format(formatter)
        } catch (_: DateTimeParseException) {
            this
        }
    }
}
