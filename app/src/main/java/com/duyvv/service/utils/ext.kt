package com.duyvv.service.utils

import java.util.Locale

fun Int.toMinuteSecond(): String {
    val minutes = this / 1000 / 60
    val seconds = this / 1000 % 60
    return String.format(Locale(""), "%02d:%02d", minutes, seconds)
}