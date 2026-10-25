package com.example.porjectpetra.data.preferences

import androidx.datastore.preferences.core.longPreferencesKey
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.porjectpetra.data.model.FinisherPool
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.petraDataStore: DataStore<Preferences> by preferencesDataStore(name = "petra_preferences")

data class UserSettings(
    val displayName: String = "",
    val weeklySessionGoal: Int = DEFAULT_WEEKLY_SESSION_GOAL,
    val finishersEnabled: Boolean = false
) {
    companion object {
        const val DEFAULT_WEEKLY_SESSION_GOAL = 3
    }
}

/** Réglages des finishers : activation globale et dernier finisher tiré par type. */
interface FinisherPreferences {
    val finishersEnabled: Flow<Boolean>
    suspend fun setFinishersEnabled(enabled: Boolean)

    suspend fun lastFinisherId(pool: FinisherPool): String?
    suspend fun setLastFinisherId(pool: FinisherPool, finisherId: String)
}

/** Finishers désactivés (aperçus, tests). */
object DisabledFinisherPreferences : FinisherPreferences {
    override val finishersEnabled: Flow<Boolean> = flowOf(false)
    override suspend fun setFinishersEnabled(enabled: Boolean) = Unit
    override suspend fun lastFinisherId(pool: FinisherPool): String? = null
    override suspend fun setLastFinisherId(pool: FinisherPool, finisherId: String) = Unit
}

interface UserPreferencesRepository : FinisherPreferences {
    val settings: Flow<UserSettings>
    suspend fun setDisplayName(name: String)
    suspend fun setWeeklySessionGoal(goal: Int)

    /**
     * Lundi (epochDay) d'une semaine A du planning sur deux semaines. Fixé au premier accès
     * (semaine en cours = A) ; [swapPlanWeeks] inverse A et B.
     */
    suspend fun planAnchorMonday(currentMonday: Long): Long
    val planAnchor: Flow<Long?>
    suspend fun swapPlanWeeks(currentMonday: Long)

    /** Vrai une seule fois : sert à ne créer la routine d'exemple qu'au premier lancement. */
    suspend fun consumeFirstLaunch(): Boolean
}

class DataStoreUserPreferencesRepository(context: Context) : UserPreferencesRepository {
    private val dataStore = context.applicationContext.petraDataStore

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val WEEKLY_SESSION_GOAL = intPreferencesKey("weekly_session_goal")
        val DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")
        val PLAN_ANCHOR = longPreferencesKey("plan_anchor_monday")
        val FINISHERS_ENABLED = booleanPreferencesKey("finishers_enabled")
        fun lastFinisher(pool: FinisherPool) = stringPreferencesKey("last_finisher_${pool.name}")
    }

    override val planAnchor: Flow<Long?> = dataStore.data.map { it[Keys.PLAN_ANCHOR] }

    override suspend fun planAnchorMonday(currentMonday: Long): Long {
        dataStore.data.first()[Keys.PLAN_ANCHOR]?.let { return it }
        dataStore.edit { it[Keys.PLAN_ANCHOR] = currentMonday }
        return currentMonday
    }

    override suspend fun swapPlanWeeks(currentMonday: Long) {
        val anchor = planAnchorMonday(currentMonday)
        // Décaler l'ancre d'une semaine inverse A et B.
        dataStore.edit { it[Keys.PLAN_ANCHOR] = anchor + 7 }
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            weeklySessionGoal = prefs[Keys.WEEKLY_SESSION_GOAL] ?: UserSettings.DEFAULT_WEEKLY_SESSION_GOAL,
            finishersEnabled = prefs[Keys.FINISHERS_ENABLED] ?: false
        )
    }

    override val finishersEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.FINISHERS_ENABLED] ?: false }.distinctUntilChanged()

    override suspend fun setFinishersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.FINISHERS_ENABLED] = enabled }
    }

    override suspend fun lastFinisherId(pool: FinisherPool): String? = dataStore.data.first()[Keys.lastFinisher(pool)]

    override suspend fun setLastFinisherId(pool: FinisherPool, finisherId: String) {
        dataStore.edit { it[Keys.lastFinisher(pool)] = finisherId }
    }

    override suspend fun setDisplayName(name: String) {
        dataStore.edit { it[Keys.DISPLAY_NAME] = name.trim() }
    }

    override suspend fun setWeeklySessionGoal(goal: Int) {
        dataStore.edit { it[Keys.WEEKLY_SESSION_GOAL] = goal.coerceIn(1, 14) }
    }

    override suspend fun consumeFirstLaunch(): Boolean {
        if (dataStore.data.first()[Keys.DEFAULTS_SEEDED] == true) return false
        dataStore.edit { it[Keys.DEFAULTS_SEEDED] = true }
        return true
    }
}
