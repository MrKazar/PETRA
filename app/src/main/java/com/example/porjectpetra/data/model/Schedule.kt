package com.example.porjectpetra.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Comment une tâche ou une routine se place dans la journée. */
enum class TriggerType(val label: String) {
    /** À une heure précise (ex. 21 h 30). */
    SCHEDULED("À heure fixe"),
    /** Après un événement de la journée (ex. « après le réveil »), sans heure imposée. */
    EVENT("Après un événement"),
    /** Dans la journée, sans heure ni événement. */
    ANYTIME("Dans la journée");

    companion object {
        fun from(value: String): TriggerType = entries.firstOrNull { it.name == value } ?: ANYTIME
    }
}

enum class Recurrence(val label: String) {
    /** Se répète chaque semaine sur les jours actifs. */
    WEEKLY("Jours choisis"),
    /** Une seule fois, à une date donnée. */
    ONCE("Une seule fois");

    companion object {
        fun from(value: String): Recurrence = entries.firstOrNull { it.name == value } ?: WEEKLY
    }
}

enum class TaskStatus {
    DONE,
    SKIPPED,
    /** Reportée au lendemain : la tâche réapparaît le jour suivant. */
    POSTPONED;

    companion object {
        fun from(value: String): TaskStatus? = entries.firstOrNull { it.name == value }
    }
}

/** Événements proposés par défaut. L'utilisateur peut en saisir d'autres. */
object DayEvents {
    val suggestions = listOf(
        "Après le réveil",
        "Avant l'entraînement",
        "Après l'entraînement",
        "Après le travail",
        "Avant le coucher"
    )

    /** Position approximative dans la journée (minutes), uniquement pour ordonner l'affichage. */
    fun approximateMinutes(event: String): Int = when (event.trim().lowercase(Locale.FRENCH)) {
        "après le réveil" -> 0
        "avant l'entraînement" -> 700
        "après l'entraînement" -> 710
        "après le travail" -> 1080
        "avant le coucher" -> 1430
        else -> 720
    }
}

/** Jours actifs encodés en masque de bits : bit 0 = lundi … bit 6 = dimanche. */
object DayMask {
    const val ALL_DAYS = 0b1111111
    const val WEEKDAYS = 0b0011111
    val shortLabels = listOf("L", "M", "M", "J", "V", "S", "D")
    val longLabels = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")

    fun contains(mask: Int, dayIndex: Int): Boolean = mask and (1 shl dayIndex) != 0
    fun toggle(mask: Int, dayIndex: Int): Int = mask xor (1 shl dayIndex)

    fun describe(mask: Int): String = when (mask and ALL_DAYS) {
        ALL_DAYS -> "Tous les jours"
        WEEKDAYS -> "En semaine"
        0b1100000 -> "Le week-end"
        0 -> "Aucun jour"
        else -> (0..6).filter { contains(mask, it) }.joinToString(" ") { longLabels[it].take(3) + "." }
    }
}

/**
 * Dates « locales » exprimées en jours depuis le 1er janvier 1970 (epochDay), dans le fuseau du téléphone.
 * minSdk 24 : on évite java.time et on s'appuie sur Calendar / TimeZone.
 */
object PetraDates {
    const val DAY_MS = 86_400_000L

    fun epochDay(millis: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(millis + tz.getOffset(millis), DAY_MS)

    /** 0 = lundi … 6 = dimanche. Le 1er janvier 1970 était un jeudi. */
    fun dayIndex(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7L).toInt()

    fun startOfWeek(epochDay: Long): Long = epochDay - dayIndex(epochDay)

    /** Millisecondes jusqu'au prochain minuit local. */
    fun millisUntilNextDay(now: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - now).coerceAtLeast(1_000L)
    }

    /** Ex. « samedi 24 octobre ». */
    fun formatLongDate(epochDay: Long): String {
        val fmt = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return fmt.format(epochDay * DAY_MS + DAY_MS / 2)
    }

    /** Ex. « 07:30 ». */
    fun formatMinutes(minutesOfDay: Int): String =
        String.format(Locale.FRENCH, "%02d:%02d", minutesOfDay / 60, minutesOfDay % 60)
}
