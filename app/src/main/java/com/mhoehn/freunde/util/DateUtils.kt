package com.mhoehn.freunde.util

import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

private val displayDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun Date.toLocalDate(): LocalDate = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toDate(): Date = Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())

fun Date.formatDisplay(): String = toLocalDate().format(displayDateFormatter)

/** Menschenlesbarer Abstand seit einem Datum, z.B. "seit 3 Monaten nicht gesehen". */
fun lastSeenLabel(lastMeetingDate: Date?): String {
    if (lastMeetingDate == null) return "Noch kein Treffen erfasst"

    val period = Period.between(lastMeetingDate.toLocalDate(), LocalDate.now())
    val totalDays = period.toTotalMonths() * 30 + period.days

    return when {
        period.years > 0 -> "seit ${period.years} ${if (period.years == 1) "Jahr" else "Jahren"} nicht gesehen"
        period.toTotalMonths() > 0 -> {
            val months = period.toTotalMonths()
            "seit $months ${if (months == 1L) "Monat" else "Monaten"} nicht gesehen"
        }
        totalDays > 0 -> "seit $totalDays ${if (totalDays == 1L) "Tag" else "Tagen"} nicht gesehen"
        else -> "heute gesehen"
    }
}

fun daysSince(date: Date): Long = Period.between(date.toLocalDate(), LocalDate.now()).let {
    it.toTotalMonths() * 30 + it.days
}
