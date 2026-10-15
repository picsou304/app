package com.dailyvlog.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface AppClock {
    fun currentTimeMillis(): Long

    fun currentInstant(): Instant = Instant.ofEpochMilli(currentTimeMillis())

    fun currentZone(): ZoneId = ZoneId.of("Europe/Paris")

    fun currentZonedDateTime(zoneId: ZoneId = currentZone()): ZonedDateTime =
        ZonedDateTime.ofInstant(currentInstant(), zoneId)

    fun currentLocalDate(zoneId: ZoneId = currentZone()): LocalDate =
        currentZonedDateTime(zoneId).toLocalDate()
}
