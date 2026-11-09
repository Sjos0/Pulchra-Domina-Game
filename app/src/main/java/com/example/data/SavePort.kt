package com.example.data

import com.example.data.model.GameState
import kotlinx.coroutines.flow.Flow

/**
 * Porta de persistência (Interface Segregation / Dependency Inversion).
 */
interface SavePort {
    val gameStateFlow: Flow<GameState?>
    val unlockedRoutesFlow: Flow<Set<String>>
    suspend fun saveGame(state: GameState)
    suspend fun unlockRoute(routeId: String)
}
