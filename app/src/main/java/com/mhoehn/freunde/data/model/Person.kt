package com.mhoehn.freunde.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Child(
    val name: String = "",
    val birthYear: Int? = null
)

/** Infos, die sich selten ändern (Partner, Kinder, Wohnort, Beruf o.ä.). */
data class FixedInfo(
    val partnerName: String = "",
    val children: List<Child> = emptyList(),
    val otherInfo: String = ""
)

/** Aktueller Stand, wird bei Bedarf überschrieben statt historisiert. */
data class TempInfo(
    val currentJob: String = "",
    val hobbies: String = "",
    val vacation: String = "",
    val notes: String = ""
)

data class Person(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val photoUri: String? = null,
    val fixedInfo: FixedInfo = FixedInfo(),
    val tempInfo: TempInfo = TempInfo(),
    val birthday: Date? = null,
    // Wird beim Anlegen/Ändern/Löschen eines Treffens aus der meetings-Subcollection nachgeführt.
    val lastMeetingDate: Date? = null,
    // Verhindert wiederholte "lange nicht gesehen"-Notifications, bis ein neues Treffen erfasst wird.
    val longTimeNoSeeNotified: Boolean = false
)
