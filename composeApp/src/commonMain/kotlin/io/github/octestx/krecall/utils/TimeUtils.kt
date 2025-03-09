package io.github.octestx.krecall.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object TimeUtils {
    fun formatTimestampToChinese(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.year}年" +
                "${localDateTime.monthNumber}月" +
                "${localDateTime.dayOfMonth}日-" +
                "${localDateTime.hour.toString().padStart(2, '0')}:" +
                "${localDateTime.minute.toString().padStart(2, '0')}:" +
                localDateTime.second.toString().padStart(2, '0')
    }
}