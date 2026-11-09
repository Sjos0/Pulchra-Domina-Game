package com.example.data.local

import android.content.Context
import com.example.data.SavePort
import com.example.data.model.GameState
import kotlinx.coroutines.flow.Flow

/** Adaptador: SaveManager → SavePort. */
class SaveManagerPort(context: Context) : SavePort {
    private val manager = SaveManager(context)

    override val gameStateFlow: Flow<GameState?> = manager.gameStateFlow
    override val unlockedRoutesFlow: Flow<Set<String>> = manager.unlockedRoutesFlow

    override suspend fun saveGame(state: GameState) = manager.saveGame(state)

    override suspend fun unlockRoute(routeId: String) = manager.saveUnlockedRoute(routeId)
}
