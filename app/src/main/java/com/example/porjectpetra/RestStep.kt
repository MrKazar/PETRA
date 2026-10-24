package com.example.porjectpetra.timer

/**
 * Une série à effectuer pendant la séance.
 * title  : ex. "Développé couché · Chauffe"
 * detail : ex. "12 rép."
 * restSeconds : repos à prendre APRÈS cette série (0 = pas de repos, par ex. dernière série)
 */
data class RestStep(
    val title: String,
    val detail: String,
    val restSeconds: Int
)

/** Repos simple (un seul minuteur) : singleRest(90) */
fun singleRest(seconds: Int, title: String = "Repos") =
    listOf(RestStep(title = title, detail = "", restSeconds = seconds))

/** Séance factice pour tester : 1 chauffe + 2 travail + 1 hypertrophie, sur 2 exercices. */
fun demoWorkoutSteps(): List<RestStep> = listOf(
    RestStep("Développé couché · Chauffe", "12 rép.", 45),
    RestStep("Développé couché · Travail 1", "6 rép.", 150),
    RestStep("Développé couché · Travail 2", "6 rép.", 150),
    RestStep("Développé couché · Hypertrophie", "10 rép.", 90),
    RestStep("Rowing barre · Chauffe", "12 rép.", 45),
    RestStep("Rowing barre · Travail 1", "8 rép.", 120),
    RestStep("Rowing barre · Travail 2", "8 rép.", 120),
    RestStep("Rowing barre · Hypertrophie", "12 rép.", 0)
)
