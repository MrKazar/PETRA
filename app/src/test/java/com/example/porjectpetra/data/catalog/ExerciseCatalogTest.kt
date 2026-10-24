package com.example.porjectpetra.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Le vrai catalogue de l'application (assets/exercices.json), lu depuis les sources. */
val testCatalog: ExerciseCatalog by lazy {
    val file = listOf("src/main/assets/exercices.json", "app/src/main/assets/exercices.json")
        .map(::File)
        .first { it.exists() }
    ListExerciseCatalog(ExerciseCatalogParser.parse(file.readText(Charsets.UTF_8)))
}

class ExerciseCatalogTest {

    @Test
    fun catalog_parsesAllExercises_withUniqueIds() {
        assertEquals(114, testCatalog.exercises.size)
        assertEquals(114, testCatalog.exercises.map { it.id }.toSet().size)
        assertTrue(testCatalog.exercises.all { it.warmup.reps > 0 && it.work.reps > 0 && it.hypertrophy.reps > 0 })
        assertTrue(testCatalog.exercises.all { it.work.restSeconds != null && it.hypertrophy.restSeconds != null })
    }

    @Test
    fun reps_takeUpperBoundOfFirstRange() {
        val bench = testCatalog.byId("dev_couche_barre")!!
        assertEquals("Pectoraux", bench.muscleGroup)
        assertEquals(12, bench.warmup.reps) // « 10-12 puis 5-6 en montant la charge »
        assertEquals(6, bench.work.reps)    // « 3-6 »
        assertEquals(180, bench.work.restSeconds)
        assertEquals(10, bench.hypertrophy.reps) // « 6-10 »
        assertEquals(120, bench.hypertrophy.restSeconds)

        val dips = testCatalog.byId("dips_pecs")!!
        assertEquals(8, dips.warmup.reps) // « 5-8 (assisté si besoin) »
        assertFalse(dips.usesLoad)
    }

    @Test
    fun plank_isTimed() {
        val plank = testCatalog.byId("planche")!!
        assertTrue(plank.work.isSeconds)
        assertEquals(20, plank.warmup.reps)
        assertEquals(45, plank.work.reps)
        assertEquals(60, plank.hypertrophy.reps)
        assertFalse(plank.usesLoad)
    }

    @Test
    fun cycle_isOneWarmupTwoWorkOneHypertrophy_withDerivedLoads() {
        val cycle = ExerciseCycle.build(testCatalog.byId("dev_couche_barre")!!, workLoadKg = 100.0)
        assertEquals(listOf("Chauffe", "Travail 1", "Travail 2", "Hypertrophie"), cycle.map { it.label })
        assertEquals(listOf(1, 2, 3, 4), cycle.map { it.setNumber })
        assertEquals(listOf(12, 6, 6, 10), cycle.map { it.reps })
        assertEquals(listOf(50.0, 100.0, 100.0, 80.0), cycle.map { it.weightKg })
        assertEquals(listOf(45, 180, 180, 120), cycle.map { it.restSeconds })
    }

    @Test
    fun loads_areRoundedToHalfKilo() {
        val cycle = ExerciseCycle.build(testCatalog.byId("dev_incline_halteres")!!, workLoadKg = 22.0)
        assertEquals(11.0, cycle[0].weightKg, 0.0)
        assertEquals(17.5, cycle[3].weightKg, 0.0) // 17,6 kg → 17,5 kg
    }

    @Test
    fun bodyweightExercise_hasNoLoad() {
        val cycle = ExerciseCycle.build(testCatalog.byId("pompes")!!, workLoadKg = 80.0)
        assertTrue(cycle.all { it.weightKg == 0.0 })
    }

    @Test
    fun formatting() {
        assertEquals("3 min", ExerciseCycle.formatRest(180))
        assertEquals("2 min 30", ExerciseCycle.formatRest(150))
        assertEquals("45 s", ExerciseCycle.formatRest(45))
        assertEquals("8 rép. / côté", ExerciseCycle.formatReps(8, isSeconds = false, unilateral = true))
        assertEquals("45 s", ExerciseCycle.formatReps(45, isSeconds = true, unilateral = false))
    }
}
