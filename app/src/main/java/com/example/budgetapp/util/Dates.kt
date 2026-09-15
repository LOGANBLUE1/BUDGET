package com.example.budgetapp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Dates {
    fun format(millis: Long): String =
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(millis))
}
