package com.ccomet.ailock.util

import com.ccomet.ailock.data.model.UsageRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")

    fun todayStartMillis(): Long = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    fun isToday(timestamp: Long): Boolean =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate() == LocalDate.now(zone)

    fun currentDayOfWeek(): DayOfWeek = LocalDate.now(zone).dayOfWeek

    fun formatDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(zone).format(dateFormatter)

    fun todayRecords(records: List<UsageRecord>): List<UsageRecord> = records.filter { isToday(it.openedAt) }

    fun minutesLabel(minutes: Int): String =
        if (minutes < 60) "${minutes}분" else "${minutes / 60}시간 ${minutes % 60}분"
}
