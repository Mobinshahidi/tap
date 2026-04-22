package com.tap.apk.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tap.apk.models.FlashMode
import com.tap.apk.models.TapAction
import com.tap.apk.models.TapEvent
import com.tap.apk.models.TapPatternConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tapDataStore by preferencesDataStore(name = "tap_settings")

class TapSettingsDataStore(private val context: Context) {

    val settingsFlow: Flow<Map<TapEvent, TapPatternConfig>> = context.tapDataStore.data.map { prefs ->
        mapOf(
            TapEvent.Single to readConfig(TapEvent.Single, prefs),
            TapEvent.Double to readConfig(TapEvent.Double, prefs),
            TapEvent.Triple to readConfig(TapEvent.Triple, prefs),
        )
    }

    suspend fun getConfig(event: TapEvent): TapPatternConfig {
        val prefs = context.tapDataStore.data.first()
        return readConfig(event, prefs)
    }

    suspend fun updateConfig(event: TapEvent, config: TapPatternConfig) {
        context.tapDataStore.edit { prefs ->
            val p = prefix(event)
            prefs[booleanPreferencesKey("${p}_enabled")] = config.enabled
            prefs[longPreferencesKey("${p}_cooldown")] = config.cooldownMs
            when (val action = config.action) {
                TapAction.None -> {
                    prefs[stringPreferencesKey("${p}_action")] = "none"
                    prefs.remove(stringPreferencesKey("${p}_value"))
                    prefs.remove(stringPreferencesKey("${p}_mode"))
                }

                is TapAction.Flashlight -> {
                    prefs[stringPreferencesKey("${p}_action")] = "flashlight"
                    prefs[stringPreferencesKey("${p}_mode")] = action.mode.name
                    prefs.remove(stringPreferencesKey("${p}_value"))
                }

                is TapAction.LaunchApp -> {
                    prefs[stringPreferencesKey("${p}_action")] = "launch_app"
                    prefs[stringPreferencesKey("${p}_value")] = action.packageName
                    prefs.remove(stringPreferencesKey("${p}_mode"))
                }

                is TapAction.Termux -> {
                    prefs[stringPreferencesKey("${p}_action")] = "termux"
                    prefs[stringPreferencesKey("${p}_value")] = action.command
                    prefs.remove(stringPreferencesKey("${p}_mode"))
                }
            }
        }
    }

    private fun readConfig(event: TapEvent, prefs: Preferences): TapPatternConfig {
        val p = prefix(event)
        val enabled = prefs[booleanPreferencesKey("${p}_enabled")] ?: true
        val cooldownMs = prefs[longPreferencesKey("${p}_cooldown")] ?: 1_000L
        val actionType = prefs[stringPreferencesKey("${p}_action")] ?: "none"
        val value = prefs[stringPreferencesKey("${p}_value")] ?: ""
        val mode = prefs[stringPreferencesKey("${p}_mode")] ?: FlashMode.Toggle.name

        val action = when (actionType) {
            "flashlight" -> TapAction.Flashlight(mode = runCatching { FlashMode.valueOf(mode) }.getOrDefault(FlashMode.Toggle))
            "launch_app" -> TapAction.LaunchApp(value)
            "termux" -> TapAction.Termux(value)
            else -> TapAction.None
        }
        return TapPatternConfig(enabled = enabled, cooldownMs = cooldownMs, action = action)
    }

    private fun prefix(event: TapEvent): String = when (event) {
        TapEvent.Single -> "single"
        TapEvent.Double -> "double"
        TapEvent.Triple -> "triple"
    }
}
