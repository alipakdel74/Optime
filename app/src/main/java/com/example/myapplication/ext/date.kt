package com.example.myapplication.ext

import java.text.SimpleDateFormat
import java.util.*

const val datePatternDayMonthTextYearAndTime = "dd. MMM yyyy  HH:mm"

fun Date.toIsoDate(): String {
    val format = SimpleDateFormat(datePatternDayMonthTextYearAndTime, Locale.ENGLISH)
    return format.format(this)
}