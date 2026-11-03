package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ankita_assistant_prefs")

class AssistantPreferences(private val context: Context) {

    companion object {
        val KEY_ASSISTANT_ENABLED = booleanPreferencesKey("assistant_master_enabled")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEY_LANGUAGE = stringPreferencesKey("language_preference") // "bn", "en", "auto"
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_WAKE_SENSITIVITY = floatPreferencesKey("wake_sensitivity")
        val KEY_SCREEN_OFF_LISTENING = booleanPreferencesKey("screen_off_listening")
        val KEY_AI_MODE = booleanPreferencesKey("ai_mode_enabled")
        val KEY_PICOVOICE_ACCESS_KEY = stringPreferencesKey("picovoice_access_key")
    }

    val assistantEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ASSISTANT_ENABLED] ?: true
    }

    val wakeWordEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_WORD_ENABLED] ?: true
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "bn"
    }

    val speechRateFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_RATE] ?: 1.0f
    }

    val speechPitchFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_PITCH] ?: 1.05f
    }

    val wakeSensitivityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_SENSITIVITY] ?: 0.75f
    }

    val screenOffListeningFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCREEN_OFF_LISTENING] ?: true
    }

    val aiModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_MODE] ?: false
    }

    val picovoiceKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PICOVOICE_ACCESS_KEY] ?: ""
    }

    suspend fun setAssistantEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[KEY_SPEECH_PITCH] = pitch }
    }

    suspend fun setWakeSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[KEY_WAKE_SENSITIVITY] = sensitivity }
    }

    suspend fun setScreenOffListening(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SCREEN_OFF_LISTENING] = enabled }
    }

    suspend fun setAiMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AI_MODE] = enabled }
    }

    suspend fun setPicovoiceKey(key: String) {
        context.dataStore.edit { it[KEY_PICOVOICE_ACCESS_KEY] = key }
    }
}
