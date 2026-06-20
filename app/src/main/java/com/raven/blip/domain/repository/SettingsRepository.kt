package com.raven.blip.domain.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.raven.blip.domain.model.AppSettings
import com.raven.blip.domain.model.OverlayCorner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blip_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_CORNER = stringPreferencesKey("corner")
        val KEY_BUBBLE_INTERVAL_MS = longPreferencesKey("bubble_interval_ms")
        val KEY_QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val KEY_QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val KEY_LAST_COMPLETED_AT = longPreferencesKey("last_completed_at")
        val KEY_SKIN_ID = stringPreferencesKey("skin_id")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            corner = try {
                OverlayCorner.valueOf(prefs[KEY_CORNER] ?: OverlayCorner.BOTTOM_END.name)
            } catch (e: Exception) {
                OverlayCorner.BOTTOM_END
            },
            bubbleIntervalMs = prefs[KEY_BUBBLE_INTERVAL_MS] ?: (15 * 60 * 1000L),
            quietHoursStart = prefs[KEY_QUIET_HOURS_START],
            quietHoursEnd = prefs[KEY_QUIET_HOURS_END],
            lastCompletedAt = prefs[KEY_LAST_COMPLETED_AT] ?: 0L,
            skinId = prefs[KEY_SKIN_ID] ?: "default"
        )
    }

    suspend fun updateCorner(corner: OverlayCorner) {
        dataStore.edit { it[KEY_CORNER] = corner.name }
    }

    suspend fun updateBubbleInterval(intervalMs: Long) {
        dataStore.edit { it[KEY_BUBBLE_INTERVAL_MS] = intervalMs }
    }

    suspend fun updateQuietHours(start: Int?, end: Int?) {
        dataStore.edit { prefs ->
            if (start != null) prefs[KEY_QUIET_HOURS_START] = start else prefs.remove(KEY_QUIET_HOURS_START)
            if (end != null) prefs[KEY_QUIET_HOURS_END] = end else prefs.remove(KEY_QUIET_HOURS_END)
        }
    }

    suspend fun updateLastCompletedAt(timeMs: Long) {
        dataStore.edit { it[KEY_LAST_COMPLETED_AT] = timeMs }
    }

    suspend fun updateSkinId(skinId: String) {
        dataStore.edit { it[KEY_SKIN_ID] = skinId }
    }
}
