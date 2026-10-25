package com.example.porjectpetra.data.model

import kotlin.random.Random

/** Type de finisher, choisi par l'utilisateur à la fin de la séance. */
enum class FinisherPool(val label: String) {
    ABDOS("Abdos"),
    DOS("Dos"),
    PECS("Pecs"),
    JAMBES("Jambes"),
    EPAULES_BRAS("Épaules / bras")
}

/** Mini-séance d'un seul tour, à fond, faite à la fin de l'entraînement. */
data class Finisher(
    val id: String,
    val name: String,
    val pool: FinisherPool,
    val steps: List<String>,
    val subtitle: String = ""
)

/** Finishers testés et validés. */
val VALIDATED_FINISHERS = listOf(
    Finisher(
        id = "abdos", name = "Abdos", pool = FinisherPool.ABDOS,
        steps = listOf("30 abdos", "10 abdos lestés", "15 Russian twists", "45 s de gainage", "30 s de gainage latéral de chaque côté")
    ),
    Finisher(
        id = "dos", name = "Dos", pool = FinisherPool.DOS,
        steps = listOf("10 tractions pronation", "10 pompes", "10 tractions prise neutre", "10 pompes", "10 tractions supination")
    ),
    Finisher(
        id = "pecs", name = "Pecs", pool = FinisherPool.PECS, subtitle = "~60 pompes sans s'arrêter",
        steps = listOf("20 pompes classiques", "10 pompes militaires", "10 pompes archer", "10 pompes déclinées", "10 pompes inclinées")
    ),
    Finisher(
        id = "jambes", name = "Jambes", pool = FinisherPool.JAMBES,
        steps = listOf("20 squats", "10 fentes sautées par jambe", "15 squats sautés", "45 s de chaise contre le mur", "20 montées sur pointes")
    )
)

/** Finishers proposés, pas encore validés. */
val PROPOSED_FINISHERS = listOf(
    Finisher(
        id = "abdos_2", name = "Abdos 2", pool = FinisherPool.ABDOS,
        steps = listOf("20 relevés de jambes", "20 mountain climbers par jambe", "15 V-ups", "1 min de gainage avec toucher d'épaules")
    ),
    Finisher(
        id = "dos_2", name = "Dos 2", pool = FinisherPool.DOS,
        steps = listOf("15 rowings inversés", "10 tractions australiennes", "20 superman", "Suspension à la barre jusqu'à l'échec")
    ),
    Finisher(
        id = "pecs_2", name = "Pecs 2", pool = FinisherPool.PECS,
        steps = listOf("10 pompes explosives", "15 pompes larges", "10 pompes diamant", "Gainage position haute jusqu'à l'échec")
    ),
    Finisher(
        id = "jambes_2", name = "Jambes 2", pool = FinisherPool.JAMBES,
        steps = listOf("15 squats bulgares par jambe", "20 ponts fessiers", "10 squats sautés", "30 s de chaise contre le mur")
    ),
    Finisher(
        id = "epaules_bras", name = "Épaules / bras", pool = FinisherPool.EPAULES_BRAS,
        steps = listOf("10 pompes piquées", "15 dips sur banc", "20 élévations latérales légères", "30 s bras tendus en croix")
    ),
    Finisher(
        id = "abdos_3", name = "Abdos 3", pool = FinisherPool.ABDOS,
        steps = listOf("20 crunchs bicyclette par côté", "15 relevés de bassin", "20 battements de jambes par jambe", "30 s de hollow hold")
    ),
    Finisher(
        id = "abdos_4", name = "Abdos 4", pool = FinisherPool.ABDOS,
        steps = listOf("20 dead bugs", "20 crunchs", "20 touchers de pieds jambes tendues", "45 s de gainage sur les coudes")
    ),
    Finisher(
        id = "dos_3", name = "Dos 3", pool = FinisherPool.DOS,
        steps = listOf("5 tractions lentes (5 s en descente)", "10 tractions", "15 rowings inversés", "30 s de suspension active")
    ),
    Finisher(
        id = "dos_4", name = "Dos 4", pool = FinisherPool.DOS,
        steps = listOf("15 élévations en Y allongé sur le ventre", "15 superman avec 2 s de maintien", "10 tractions australiennes", "10 bird dogs par côté")
    ),
    Finisher(
        id = "pecs_3", name = "Pecs 3", pool = FinisherPool.PECS,
        steps = listOf("10 pompes lentes (3 s en descente)", "10 pompes larges", "10 pompes serrées", "Pompes sur les genoux jusqu'à l'échec")
    ),
    Finisher(
        id = "pecs_4", name = "Pecs 4", pool = FinisherPool.PECS,
        steps = listOf("15 dips", "15 pompes", "10 dips", "10 pompes", "20 s en position basse de pompe")
    ),
    Finisher(
        id = "jambes_3", name = "Jambes 3", pool = FinisherPool.JAMBES,
        steps = listOf("15 fentes arrière par jambe", "20 squats sumo", "10 squats sautés", "30 s de chaise contre le mur")
    ),
    Finisher(
        id = "jambes_4", name = "Jambes 4", pool = FinisherPool.JAMBES,
        steps = listOf("8 squats pistol assistés par jambe", "10 ponts fessiers sur une jambe par jambe", "30 jumping jacks", "30 montées sur pointes")
    ),
    Finisher(
        id = "epaules_bras_2", name = "Épaules / bras 2", pool = FinisherPool.EPAULES_BRAS,
        steps = listOf("10 pompes piquées pieds surélevés", "15 dips sur banc", "10 pompes diamant", "30 s de planche bras tendus avec toucher d'épaules")
    ),
    Finisher(
        id = "epaules_bras_3", name = "Épaules / bras 3", pool = FinisherPool.EPAULES_BRAS,
        steps = listOf("20 cercles de bras vers l'avant puis 20 vers l'arrière", "15 pompes piquées", "20 dips sur banc", "20 s de maintien bras tendus en haut des dips")
    )
)

/** Inclure les finishers proposés dans le catalogue (sinon : seulement les validés). */
const val INCLUDE_PROPOSED_FINISHERS = true

val FINISHERS: List<Finisher> =
    if (INCLUDE_PROPOSED_FINISHERS) VALIDATED_FINISHERS + PROPOSED_FINISHERS else VALIDATED_FINISHERS

/** Types qui ont au moins un finisher dans le catalogue. */
val availableFinisherPools: List<FinisherPool>
    get() = FinisherPool.entries.filter { pool -> FINISHERS.any { it.pool == pool } }

/**
 * Tire un finisher au hasard dans [pool], jamais le même que [lastId] quand le type en compte plusieurs.
 * Null si le type n'a aucun finisher.
 */
fun pickFinisher(
    pool: FinisherPool,
    lastId: String?,
    random: Random = Random.Default,
    catalog: List<Finisher> = FINISHERS
): Finisher? {
    val candidates = catalog.filter { it.pool == pool }
    if (candidates.size <= 1) return candidates.firstOrNull()
    val choices = candidates.filter { it.id != lastId }
    return choices[random.nextInt(choices.size)]
}
