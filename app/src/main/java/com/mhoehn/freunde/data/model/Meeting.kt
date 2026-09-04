package com.mhoehn.freunde.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Meeting(
    @DocumentId
    val id: String = "",
    val date: Date = Date(),
    val location: String = "",
    val notes: String = ""
)
