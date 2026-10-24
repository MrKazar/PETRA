package com.example.porjectpetra.data.catalog

import java.util.Locale
import kotlin.math.roundToInt

enum class SetPhase(val label: String) {
    WARMUP("Chauffe"),
    WORK("Travail"),
    HYPERTROPHY("Hypertrophie");

    companion object {
        fun from(value: String?): SetPhase? = entries.firstOrNull { it.name == value }
    }
}

/** Une série du cycle, entièrement déterminée par le catalogue et la charge de travail. */
data class CycleSet(
    val setNumber: Int,
    val phase: SetPhase,
    /** Numéro dans la phase (Travail 1, Travail 2). */
    val indexInPhase: Int,
    val reps: Int,
    val isSeconds: Boolean,
    val weightKg: Double,
    val restSeconds: Int
) {
    val label: String get() = if (phase == SetPhase.WORK) "${phase.label} $indexInPhase" else phase.label
}

/**
 * Cycle fixe par exercice : 1 série de chauffe, 2 de travail, 1 d'hypertrophie.
 * Répétitions et repos viennent du catalogue ; les charges sont calculées depuis la charge de travail.
 */
object ExerciseCycle {
    const val WARMUP_SETS = 1
    const val WORK_SETS = 2
    const val HYPERTROPHY_SETS = 1
    const val TOTAL_SETS = WARMUP_SETS + WORK_SETS + HYPERTROPHY_SETS

    /** Chauffe : milieu de la fourchette 40-60 % de la charge de travail. */
    const val WARMUP_LOAD_RATIO = 0.5

    /** Hypertrophie : charge modérée, plus légère que le travail (plus de répétitions). */
    const val HYPERTROPHY_LOAD_RATIO = 0.8

    /** Le JSON ne donne pas de repos pour la chauffe. */
    const val WARMUP_REST_SECONDS = 45

    /** Pas d'arrondi des charges (en kg). */
    const val LOAD_STEP_KG = 0.5

    fun build(exercise: CatalogExercise, workLoadKg: Double): List<CycleSet> {
        val load = if (exercise.usesLoad) workLoadKg.coerceAtLeast(0.0) else 0.0
        val sets = mutableListOf<CycleSet>()
        fun add(phase: SetPhase, count: Int, p: PhasePrescription, ratio: Double, defaultRest: Int) {
            repeat(count) { i ->
                sets += CycleSet(
                    setNumber = sets.size + 1,
                    phase = phase,
                    indexInPhase = i + 1,
                    reps = p.reps,
                    isSeconds = p.isSeconds,
                    weightKg = roundLoad(load * ratio),
                    restSeconds = p.restSeconds ?: defaultRest
                )
            }
        }
        add(SetPhase.WARMUP, WARMUP_SETS, exercise.warmup, WARMUP_LOAD_RATIO, WARMUP_REST_SECONDS)
        add(SetPhase.WORK, WORK_SETS, exercise.work, 1.0, exercise.work.restSeconds ?: 120)
        add(SetPhase.HYPERTROPHY, HYPERTROPHY_SETS, exercise.hypertrophy, HYPERTROPHY_LOAD_RATIO, 90)
        return sets
    }

    fun roundLoad(kg: Double): Double = (kg / LOAD_STEP_KG).roundToInt() * LOAD_STEP_KG

    fun formatKg(kg: Double): String =
        if (kg % 1.0 == 0.0) kg.toInt().toString() else String.format(Locale.FRENCH, "%.1f", kg)

    fun formatRest(seconds: Int): String = when {
        seconds < 60 -> "$seconds s"
        seconds % 60 == 0 -> "${seconds / 60} min"
        else -> "${seconds / 60} min ${seconds % 60}"
    }

    fun formatReps(reps: Int, isSeconds: Boolean, unilateral: Boolean): String =
        (if (isSeconds) "$reps s" else "$reps rép.") + if (unilateral) " / côté" else ""
}
