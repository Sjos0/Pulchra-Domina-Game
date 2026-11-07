package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "pulchra_domina_save")

class SaveManager(private val context: Context) {

    companion object {
        private val KEY_HAS_SAVE = booleanPreferencesKey("has_save")
        private val KEY_PONT_CAP = intPreferencesKey("pont_cap")
        private val KEY_PONT_TOTAL = intPreferencesKey("pont_total")
        private val KEY_CONFIANCA_MAVETH = intPreferencesKey("confianca_maveth")
        private val KEY_PEGOU_FLOR = booleanPreferencesKey("pegou_flor")
        private val KEY_ESCOLHAS_FEITAS = stringPreferencesKey("escolhas_feitas")
        private val KEY_CAPITULO_ATUAL = stringPreferencesKey("capitulo_atual")
        private val KEY_NODE_ID = stringPreferencesKey("node_id")
        private val KEY_MACRO_ROTA = stringPreferencesKey("macro_rota")
        private val KEY_TIMESTAMP = longPreferencesKey("timestamp")
        private val KEY_ROTAS_DESBLOQUEADAS = stringSetPreferencesKey("rotas_desbloqueadas")
    }

    val gameStateFlow: Flow<GameState?> = context.gameDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val hasSave = prefs[KEY_HAS_SAVE] ?: false
            if (!hasSave) {
                null
            } else {
                val rawChoices = prefs[KEY_ESCOLHAS_FEITAS] ?: ""
                val choicesList = if (rawChoices.isBlank()) emptyList() else rawChoices.split(";;")
                GameState(
                    pont_cap = prefs[KEY_PONT_CAP] ?: 0,
                    pont_total = prefs[KEY_PONT_TOTAL] ?: 0,
                    confianca_maveth = prefs[KEY_CONFIANCA_MAVETH] ?: 0,
                    pegou_flor = prefs[KEY_PEGOU_FLOR] ?: false,
                    escolhas_feitas = choicesList,
                    capitulo_atual = prefs[KEY_CAPITULO_ATUAL] ?: "capitulo1",
                    nodeId = prefs[KEY_NODE_ID] ?: "inicio",
                    macroRotaFinal = prefs[KEY_MACRO_ROTA],
                    timestamp = prefs[KEY_TIMESTAMP] ?: System.currentTimeMillis()
                )
            }
        }

    val unlockedRoutesFlow: Flow<Set<String>> = context.gameDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_ROTAS_DESBLOQUEADAS] ?: emptySet()
        }

    suspend fun saveGame(state: GameState) {
        context.gameDataStore.edit { prefs ->
            prefs[KEY_HAS_SAVE] = true
            prefs[KEY_PONT_CAP] = state.pont_cap
            prefs[KEY_PONT_TOTAL] = state.pont_total
            prefs[KEY_CONFIANCA_MAVETH] = state.confianca_maveth
            prefs[KEY_PEGOU_FLOR] = state.pegou_flor
            prefs[KEY_ESCOLHAS_FEITAS] = state.escolhas_feitas.joinToString(";;")
            prefs[KEY_CAPITULO_ATUAL] = state.capitulo_atual
            prefs[KEY_NODE_ID] = state.nodeId
            if (state.macroRotaFinal != null) {
                prefs[KEY_MACRO_ROTA] = state.macroRotaFinal
            }
            prefs[KEY_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun saveUnlockedRoute(routeId: String) {
        context.gameDataStore.edit { prefs ->
            val current = prefs[KEY_ROTAS_DESBLOQUEADAS] ?: emptySet()
            prefs[KEY_ROTAS_DESBLOQUEADAS] = current + routeId
        }
    }

    suspend fun clearSave() {
        context.gameDataStore.edit { prefs ->
            prefs[KEY_HAS_SAVE] = false
            prefs.remove(KEY_NODE_ID)
            prefs.remove(KEY_PONT_CAP)
            prefs.remove(KEY_CONFIANCA_MAVETH)
            prefs.remove(KEY_PEGOU_FLOR)
            prefs.remove(KEY_ESCOLHAS_FEITAS)
            prefs.remove(KEY_MACRO_ROTA)
        }
    }
}
