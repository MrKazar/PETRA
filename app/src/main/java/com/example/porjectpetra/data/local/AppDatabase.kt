package com.example.porjectpetra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.porjectpetra.data.local.dao.BodyMetricDao
import com.example.porjectpetra.data.local.dao.ProgramDao
import com.example.porjectpetra.data.local.dao.RoutineDao
import com.example.porjectpetra.data.local.dao.WorkoutSessionDao
import androidx.room.migration.Migration
import com.example.porjectpetra.data.model.SportVocabulary
import com.example.porjectpetra.data.local.entity.BodyMetric
import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.LoggedSet
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.local.entity.TaskLog
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import com.example.porjectpetra.data.local.entity.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkoutProgram::class,
        Exercise::class,
        WorkoutSession::class,
        LoggedSet::class,
        BodyMetric::class,
        Routine::class,
        DailyTask::class,
        TaskLog::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    // Les montées de version conservent les données ; seul un retour en arrière efface la base.
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /** v2 : routines, tâches quotidiennes et journal de réalisation. Les données sportives sont conservées. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `triggerType` TEXT NOT NULL, " +
                        "`scheduledMinutes` INTEGER, `triggerEvent` TEXT NOT NULL, `activeDays` INTEGER NOT NULL, " +
                        "`orderIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`routineId` INTEGER, `title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                        "`triggerType` TEXT NOT NULL, `scheduledMinutes` INTEGER, `triggerEvent` TEXT NOT NULL, " +
                        "`estimatedMinutes` INTEGER NOT NULL, `activeDays` INTEGER NOT NULL, `recurrence` TEXT NOT NULL, " +
                        "`oneOffEpochDay` INTEGER, `orderIndex` INTEGER NOT NULL, `createdEpochDay` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_routineId` ON `tasks` (`routineId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_logs_taskId_epochDay` ON `task_logs` (`taskId`, `epochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_logs_epochDay` ON `task_logs` (`epochDay`)")
            }
        }

        /**
         * v3 : francisation des données créées en anglais par les versions précédentes
         * (catégories, groupes musculaires, programmes et exercices d'exemple). Seules les valeurs
         * exactes d'origine sont traduites : ce que l'utilisateur a renommé n'est pas touché.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                fun translate(table: String, column: String, values: Map<String, String>) {
                    values.forEach { (en, fr) ->
                        db.execSQL("UPDATE `$table` SET `$column` = ? WHERE `$column` = ?", arrayOf<Any>(fr, en))
                    }
                }
                translate("workout_programs", "category", SportVocabulary.legacyCategories)
                translate("workout_programs", "name", SportVocabulary.legacyProgramNames)
                translate("workout_programs", "description", SportVocabulary.legacyProgramDescriptions)
                translate("exercises", "muscleGroup", SportVocabulary.legacyMuscleGroups)
                translate("exercises", "name", SportVocabulary.legacyExerciseNames)
                // L'historique garde les noms tels qu'enregistrés : on le traduit aussi pour rester cohérent.
                translate("workout_sessions", "programName", SportVocabulary.legacyProgramNames)
                translate("logged_sets", "exerciseName", SportVocabulary.legacyExerciseNames)
                translate("body_metrics", "notes", mapOf("Initial baseline measurement" to "Mesure de départ"))
            }
        }

        /**
         * v4 : exercices reliés au catalogue (cycle chauffe / travail / hypertrophie) et phase de chaque
         * série enregistrée. Les exercices d'exemple sont reliés au catalogue ; les exercices créés par
         * l'utilisateur restent des exercices libres.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `catalogId` TEXT")
                db.execSQL("ALTER TABLE `logged_sets` ADD COLUMN `phase` TEXT NOT NULL DEFAULT ''")
                SportVocabulary.legacyCatalogLinks.forEach { (oldName, link) ->
                    db.execSQL(
                        "UPDATE `exercises` SET `catalogId` = ?, `name` = ?, `muscleGroup` = ?, `targetSets` = 4, " +
                            "`targetDurationSeconds` = 0 WHERE `name` = ? AND `catalogId` IS NULL",
                        arrayOf<Any>(link.catalogId, link.name, link.muscleGroup, oldName)
                    )
                    db.execSQL(
                        "UPDATE `logged_sets` SET `exerciseName` = ? WHERE `exerciseName` = ?",
                        arrayOf<Any>(link.name, oldName)
                    )
                }
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope?
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope?.launch(Dispatchers.IO) {
                        populateInitialData(database.programDao(), database.bodyMetricDao())
                    }
                }
            }
        }

        /** Exercice d'exemple relié au catalogue : 4 séries (cycle fixe), [workLoadKg] = charge de travail. */
        private fun seedExercise(programId: Long, order: Int, catalogId: String, name: String, group: String, workLoadKg: Double) =
            Exercise(
                programId = programId, name = name, muscleGroup = group, targetSets = 4,
                targetWeightKg = workLoadKg, orderIndex = order, catalogId = catalogId
            )

        private suspend fun populateInitialData(programDao: ProgramDao, bodyMetricDao: BodyMetricDao) {
            if (programDao.getProgramCount() == 0) {
                // Programme 1 : corps entier
                val fullBodyId = programDao.insertProgram(
                    WorkoutProgram(
                        name = "Corps entier — Fondamentaux",
                        description = "Séance complète qui sollicite tous les grands groupes musculaires.",
                        category = "Corps entier",
                        targetFrequencyPerWeek = 3
                    )
                )
                programDao.insertExercises(
                    listOf(
                        seedExercise(fullBodyId, 0, "squat_barre", "Squat barre (high bar)", "Quadriceps", 60.0),
                        seedExercise(fullBodyId, 1, "dev_couche_barre", "Développé couché à la barre", "Pectoraux", 50.0),
                        seedExercise(fullBodyId, 2, "rowing_barre", "Rowing barre penché", "Dos", 40.0),
                        seedExercise(fullBodyId, 3, "dev_militaire_barre", "Développé militaire à la barre", "Épaules", 30.0),
                        seedExercise(fullBodyId, 4, "planche", "Gainage planche", "Abdominaux", 0.0)
                    )
                )

                // Programme 2 : haut du corps
                val upperBodyId = programDao.insertProgram(
                    WorkoutProgram(
                        name = "Haut du corps — Hypertrophie",
                        description = "Pectoraux, dos, épaules et bras, orientée prise de masse musculaire.",
                        category = "Haut du corps",
                        targetFrequencyPerWeek = 2
                    )
                )
                programDao.insertExercises(
                    listOf(
                        seedExercise(upperBodyId, 0, "dev_incline_halteres", "Développé incliné aux haltères", "Pectoraux", 22.0),
                        seedExercise(upperBodyId, 1, "tirage_vertical_large", "Tirage vertical poitrine prise large", "Dos", 45.0),
                        seedExercise(upperBodyId, 2, "elevations_laterales_halteres", "Élévations latérales aux haltères", "Épaules", 8.0),
                        seedExercise(upperBodyId, 3, "curl_barre", "Curl barre droite", "Biceps", 20.0),
                        seedExercise(upperBodyId, 4, "pushdown_corde", "Extension poulie haute à la corde", "Triceps", 25.0)
                    )
                )

                // Mesure corporelle de départ
                bodyMetricDao.insertMetric(
                    BodyMetric(
                        weightKg = 75.5,
                        bodyFatPercentage = 18.2,
                        chestCm = 100.0,
                        waistCm = 82.0,
                        armsCm = 36.0,
                        thighsCm = 58.0,
                        notes = "Mesure de départ"
                    )
                )
            }
        }
    }
}
