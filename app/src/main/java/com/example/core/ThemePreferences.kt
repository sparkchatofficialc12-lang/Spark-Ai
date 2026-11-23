package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MusicTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.sparkDataStore: DataStore<Preferences> by preferencesDataStore(name = "spark_ai_prefs")

class ThemePreferences(context: Context) {
    private val appContext = context.applicationContext
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val appLockKey = booleanPreferencesKey("app_lock_enabled")
    private val uploadedTracksKey = stringSetPreferencesKey("uploaded_audio_tracks")

    val darkModeFlow: Flow<Boolean> = appContext.sparkDataStore.data.map { prefs ->
        prefs[darkModeKey] ?: true
    }

    val appLockFlow: Flow<Boolean> = appContext.sparkDataStore.data.map { prefs ->
        prefs[appLockKey] ?: false
    }

    val uploadedTracksFlow: Flow<List<MusicTrack>> = appContext.sparkDataStore.data.map { prefs ->
        prefs[uploadedTracksKey].orEmpty().mapNotNull { raw ->
            try {
                val obj = JSONObject(raw)
                MusicTrack(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    artist = obj.optString("artist"),
                    category = obj.optString("category"),
                    bpm = obj.optInt("bpm", 100),
                    durationSec = obj.optInt("durationSec", 180),
                    mood = obj.optString("mood", "Custom User Audio"),
                    licenseType = obj.optString("licenseType", "User-Supplied Rights"),
                    localUri = obj.optString("localUri")
                ).takeIf { it.id.isNotBlank() && it.localUri.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.id }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        appContext.sparkDataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        appContext.sparkDataStore.edit { prefs ->
            prefs[appLockKey] = enabled
        }
    }

    suspend fun saveUploadedTrack(track: MusicTrack) {
        val json = JSONObject().apply {
            put("id", track.id)
            put("title", track.title)
            put("artist", track.artist)
            put("category", track.category)
            put("bpm", track.bpm)
            put("durationSec", track.durationSec)
            put("mood", track.mood)
            put("licenseType", track.licenseType)
            put("localUri", track.localUri)
        }.toString()
        appContext.sparkDataStore.edit { prefs ->
            val current = prefs[uploadedTracksKey].orEmpty().toMutableSet()
            current.add(json)
            prefs[uploadedTracksKey] = current
        }
    }
}
