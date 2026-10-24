package com.example.porjectpetra.data.model

/** Vocabulaire sportif de PETRA, en français. Source unique pour les listes et la migration des anciennes données. */
object SportVocabulary {
    const val CORPS_ENTIER = "Corps entier"
    const val CARDIO = "Cardio"
    const val GENERAL = "Général"

    /** Groupes des exercices libres (hors catalogue) : ceux du catalogue, plus cardio et corps entier. */
    val muscleGroups = listOf(
        "Pectoraux", "Dos", "Épaules", "Biceps", "Triceps", "Avant-bras", "Trapèzes", "Lombaires",
        "Quadriceps", "Ischio-jambiers", "Fessiers", "Adducteurs", "Mollets", "Abdominaux", "Obliques",
        CORPS_ENTIER, CARDIO
    )

    val programCategories = listOf(
        CORPS_ENTIER, "Haut du corps", "Bas du corps", "Poussée / Tirage / Jambes", "Cardio & gainage", "Autre"
    )

    /** Anciennes valeurs anglaises (versions précédentes de l'application) → français. */
    val legacyMuscleGroups = mapOf(
        "Chest" to "Pectoraux",
        "Back" to "Dos",
        "Legs" to "Jambes",
        "Shoulders" to "Épaules",
        "Arms" to "Bras",
        "Core" to "Abdos",
        "Full Body" to CORPS_ENTIER
    )

    val legacyCategories = mapOf(
        "Full Body" to CORPS_ENTIER,
        "Upper Body" to "Haut du corps",
        "Lower Body" to "Bas du corps",
        "Push/Pull/Legs" to "Poussée / Tirage / Jambes",
        "Cardio & Core" to "Cardio & gainage",
        "General" to GENERAL
    )

    /** Programmes et exercices d'exemple créés par les versions précédentes, traduits. */
    val legacyProgramNames = mapOf(
        "Full Body Blast" to "Corps entier — Fondamentaux",
        "Upper Body Focus" to "Haut du corps — Hypertrophie"
    )

    val legacyProgramDescriptions = mapOf(
        "Complete full body workout targeting all major muscle groups." to
            "Séance complète qui sollicite tous les grands groupes musculaires.",
        "Targeting chest, back, shoulders, and arms for hypertrophy." to
            "Pectoraux, dos, épaules et bras, orientée prise de masse musculaire."
    )

    val legacyExerciseNames = mapOf(
        "Barbell Squat" to "Squat barre",
        "Bench Press" to "Développé couché",
        "Bent-Over Row" to "Rowing barre buste penché",
        "Overhead Press" to "Développé militaire",
        "Plank" to "Gainage planche",
        "Incline Dumbbell Press" to "Développé incliné haltères",
        "Lat Pulldown" to "Tirage vertical à la poulie",
        "Dumbbell Lateral Raise" to "Élévations latérales haltères",
        "Bicep Barbell Curl" to "Curl biceps à la barre",
        "Tricep Rope Pushdown" to "Extension triceps à la poulie (corde)"
    )

    /** Exercice relié au catalogue lors de la migration : id, nom et groupe du catalogue. */
    data class CatalogLink(val catalogId: String, val name: String, val muscleGroup: String)

    /**
     * Exercices d'exemple des versions précédentes (noms déjà francisés en v3) reliés au catalogue,
     * pour qu'ils suivent le cycle chauffe / travail / hypertrophie.
     */
    val legacyCatalogLinks = mapOf(
        "Squat barre" to CatalogLink("squat_barre", "Squat barre (high bar)", "Quadriceps"),
        "Développé couché" to CatalogLink("dev_couche_barre", "Développé couché à la barre", "Pectoraux"),
        "Rowing barre buste penché" to CatalogLink("rowing_barre", "Rowing barre penché", "Dos"),
        "Développé militaire" to CatalogLink("dev_militaire_barre", "Développé militaire à la barre", "Épaules"),
        "Gainage planche" to CatalogLink("planche", "Gainage planche", "Abdominaux"),
        "Développé incliné haltères" to CatalogLink("dev_incline_halteres", "Développé incliné aux haltères", "Pectoraux"),
        "Tirage vertical à la poulie" to CatalogLink("tirage_vertical_large", "Tirage vertical poitrine prise large", "Dos"),
        "Élévations latérales haltères" to CatalogLink("elevations_laterales_halteres", "Élévations latérales aux haltères", "Épaules"),
        "Curl biceps à la barre" to CatalogLink("curl_barre", "Curl barre droite", "Biceps"),
        "Extension triceps à la poulie (corde)" to CatalogLink("pushdown_corde", "Extension poulie haute à la corde", "Triceps")
    )
}
