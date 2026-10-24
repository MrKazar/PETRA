package com.example.porjectpetra.data.catalog

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

// Format du fichier assets/exercices.json
@JsonClass(generateAdapter = true)
data class CatalogFileJson(
    val version: String,
    val exercices: List<CatalogExerciseJson>
)

@JsonClass(generateAdapter = true)
data class CatalogExerciseJson(
    val id: String,
    val nom: String,
    @Json(name = "groupe_musculaire") val groupeMusculaire: String,
    @Json(name = "muscles_secondaires") val musclesSecondaires: List<String> = emptyList(),
    val equipement: String,
    val type: String,
    @Json(name = "par_cote") val parCote: Boolean = false,
    // Musculation : trois phases. Cardio : bloc « cardio » à la place.
    val chauffe: CatalogPhaseJson? = null,
    val travail: CatalogPhaseJson? = null,
    val hypertrophie: CatalogPhaseJson? = null,
    val cardio: CatalogCardioJson? = null
)

@JsonClass(generateAdapter = true)
data class CatalogCardioJson(
    @Json(name = "echauffement_min") val echauffementMin: Int,
    @Json(name = "effort_min") val effortMin: Int,
    @Json(name = "retour_calme_min") val retourCalmeMin: Int,
    val intensite: String = "",
    val consigne: String = ""
)

@JsonClass(generateAdapter = true)
data class CatalogPhaseJson(
    val series: Int,
    val reps: String,
    val charge: String? = null,
    val objectif: String? = null,
    @Json(name = "repos_sec") val reposSec: Int? = null
)

/** Prescription d'une phase, telle que donnée par le JSON (ex. reps « 6-8 », repos 150 s). */
data class PhasePrescription(
    /** Texte d'origine du JSON, affiché à titre indicatif. */
    val repsText: String,
    /** Nombre fixe retenu pour la séance : borne haute de la première fourchette (« 6-8 » → 8). */
    val reps: Int,
    /** Vrai si la prescription est une durée en secondes (gainage : « 30-45 s »). */
    val isSeconds: Boolean,
    /** Repos après la série ; null si le JSON n'en donne pas (chauffe). */
    val restSeconds: Int?
)

data class CatalogExercise(
    val id: String,
    val name: String,
    val muscleGroupKey: String,
    val muscleGroup: String,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val isCompound: Boolean,
    /** Les répétitions se font de chaque côté. */
    val isUnilateral: Boolean,
    /** Faux pour les exercices au poids du corps : pas de charge à saisir. */
    val usesLoad: Boolean,
    val warmup: PhasePrescription,
    val work: PhasePrescription,
    val hypertrophy: PhasePrescription,
    /** Non nul pour un exercice de cardio : échauffement / effort / retour au calme, en minutes. */
    val cardio: CardioPrescription? = null
) {
    val isCardio: Boolean get() = cardio != null
}

data class CardioPrescription(
    val warmupMinutes: Int,
    val effortMinutes: Int,
    val cooldownMinutes: Int,
    val intensity: String,
    val instructions: String
)

interface ExerciseCatalog {
    val exercises: List<CatalogExercise>
    fun byId(id: String?): CatalogExercise?
    val muscleGroups: List<String> get() = exercises.map { it.muscleGroup }.distinct().sorted()
}

object EmptyExerciseCatalog : ExerciseCatalog {
    override val exercises: List<CatalogExercise> = emptyList()
    override fun byId(id: String?): CatalogExercise? = null
}

class ListExerciseCatalog(override val exercises: List<CatalogExercise>) : ExerciseCatalog {
    private val byId = exercises.associateBy { it.id }
    override fun byId(id: String?): CatalogExercise? = id?.let { byId[it] }
}

/** Catalogue lu une seule fois depuis assets/exercices.json. */
class AssetExerciseCatalog(context: Context) : ExerciseCatalog {
    private val delegate: ListExerciseCatalog by lazy {
        val json = context.applicationContext.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        ListExerciseCatalog(ExerciseCatalogParser.parse(json))
    }

    override val exercises: List<CatalogExercise> get() = delegate.exercises
    override fun byId(id: String?): CatalogExercise? = delegate.byId(id)

    companion object {
        const val ASSET_NAME = "exercices.json"
    }
}

object ExerciseCatalogParser {
    private val adapter = Moshi.Builder().build().adapter(CatalogFileJson::class.java)

    /** Équipements sans charge additionnelle à saisir. */
    private val bodyweightEquipment = setOf("poids du corps", "barres parallèles", "barre de traction", "roue", "banc")

    private val groupLabels = mapOf(
        "abdominaux" to "Abdominaux",
        "adducteurs" to "Adducteurs",
        "avant_bras" to "Avant-bras",
        "biceps" to "Biceps",
        "dos" to "Dos",
        "epaules" to "Épaules",
        "fessiers" to "Fessiers",
        "ischio_jambiers" to "Ischio-jambiers",
        "lombaires" to "Lombaires",
        "mollets" to "Mollets",
        "obliques" to "Obliques",
        "pectoraux" to "Pectoraux",
        "quadriceps" to "Quadriceps",
        "trapezes" to "Trapèzes",
        "triceps" to "Triceps",
        "cardio" to "Cardio"
    )

    private val firstRange = Regex("""^\s*(\d+)(?:\s*-\s*(\d+))?\s*(s\b)?""")

    fun groupLabel(key: String): String =
        groupLabels[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }

    fun parse(json: String): List<CatalogExercise> {
        val file = adapter.fromJson(json) ?: error("Catalogue d'exercices vide ou illisible")
        return file.exercices.map { e ->
            e.cardio?.let { return@map cardioExercise(e, it) }
            CatalogExercise(
                id = e.id,
                name = e.nom,
                muscleGroupKey = e.groupeMusculaire,
                muscleGroup = groupLabel(e.groupeMusculaire),
                secondaryMuscles = e.musclesSecondaires.map { groupLabel(it) },
                equipment = e.equipement,
                isCompound = e.type == "polyarticulaire",
                isUnilateral = e.parCote,
                usesLoad = e.equipement !in bodyweightEquipment,
                warmup = prescription(e.chauffe ?: error("Chauffe manquante pour ${e.id}"), e.id),
                work = prescription(e.travail ?: error("Travail manquant pour ${e.id}"), e.id),
                hypertrophy = prescription(e.hypertrophie ?: error("Hypertrophie manquante pour ${e.id}"), e.id)
            )
        }
    }

    /** Cardio : les trois phases deviennent des durées (en secondes), enchaînées sans repos. */
    private fun cardioExercise(e: CatalogExerciseJson, c: CatalogCardioJson): CatalogExercise {
        fun timed(minutes: Int) = PhasePrescription(repsText = "$minutes min", reps = minutes * 60, isSeconds = true, restSeconds = 0)
        return CatalogExercise(
            id = e.id,
            name = e.nom,
            muscleGroupKey = e.groupeMusculaire,
            muscleGroup = groupLabel(e.groupeMusculaire),
            secondaryMuscles = emptyList(),
            equipment = e.equipement,
            isCompound = false,
            isUnilateral = false,
            usesLoad = false,
            warmup = timed(c.echauffementMin),
            work = timed(c.effortMin),
            hypertrophy = timed(c.retourCalmeMin),
            cardio = CardioPrescription(c.echauffementMin, c.effortMin, c.retourCalmeMin, c.intensite, c.consigne)
        )
    }

    /** « 10-12 puis 5-6 en montant la charge » → 12 ; « 3-6 » → 6 ; « 15 » → 15 ; « 30-45 s » → 45 s. */
    fun prescription(phase: CatalogPhaseJson, exerciseId: String = ""): PhasePrescription {
        val match = firstRange.find(phase.reps)
            ?: error("Répétitions illisibles « ${phase.reps} » pour $exerciseId")
        val low = match.groupValues[1].toInt()
        val high = match.groupValues[2].toIntOrNull() ?: low
        return PhasePrescription(
            repsText = phase.reps,
            reps = high,
            isSeconds = match.groupValues[3].isNotEmpty(),
            restSeconds = phase.reposSec
        )
    }
}
