package com.example.porjectpetra

import com.example.porjectpetra.update.UpdateManager
import com.example.porjectpetra.data.repository.PlanRepositoryImpl
import com.example.porjectpetra.data.repository.PlanRepository
import android.app.Application
import com.example.porjectpetra.data.TodayProvider
import com.example.porjectpetra.data.catalog.AssetExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCatalog
import com.example.porjectpetra.data.local.AppDatabase
import com.example.porjectpetra.data.preferences.DataStoreUserPreferencesRepository
import com.example.porjectpetra.data.preferences.UserPreferencesRepository
import com.example.porjectpetra.data.repository.ProgramRepository
import com.example.porjectpetra.data.repository.ProgramRepositoryImpl
import com.example.porjectpetra.data.repository.RoutineRepository
import com.example.porjectpetra.data.repository.RoutineRepositoryImpl
import com.example.porjectpetra.data.repository.WorkoutSessionRepository
import com.example.porjectpetra.data.repository.WorkoutSessionRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitnessApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val programRepository: ProgramRepository by lazy { ProgramRepositoryImpl(database.programDao()) }
    val workoutSessionRepository: WorkoutSessionRepository by lazy { WorkoutSessionRepositoryImpl(database.workoutSessionDao()) }
    val routineRepository: RoutineRepository by lazy { RoutineRepositoryImpl(database.routineDao()) }
    val userPreferences: UserPreferencesRepository by lazy { DataStoreUserPreferencesRepository(this) }
    val todayProvider: TodayProvider by lazy { TodayProvider(this, applicationScope) }
    val exerciseCatalog: ExerciseCatalog by lazy { AssetExerciseCatalog(this) }
    val planRepository: PlanRepository by lazy { PlanRepositoryImpl(database.planDao()) }
    val updateManager: UpdateManager by lazy { UpdateManager(this, applicationScope) }

    override fun onCreate() {
        super.onCreate()
        // Routine du matin d'exemple : créée une seule fois, au premier lancement de PETRA.
        // Si l'utilisateur la supprime ensuite, elle ne revient pas.
        applicationScope.launch(Dispatchers.IO) {
            if (userPreferences.consumeFirstLaunch()) {
                routineRepository.seedDefaultRoutinesIfEmpty()
            }
        }
    }
}
