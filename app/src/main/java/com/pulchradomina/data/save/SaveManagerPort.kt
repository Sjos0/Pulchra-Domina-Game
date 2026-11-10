package com.pulchradomina.data.save

import android.content.Context
import com.pulchradomina.data.save.SavePort
import com.pulchradomina.data.model.GameState
import kotlinx.coroutines.flow.Flow

/** Adaptador: SaveManager → SavePort. */
class SaveManagerPort(context: Context) : SavePort {
    private val manager = SaveManager(context)

    override val gameStateFlow: Flow<GameState?> = manager.gameStateFlow
    override val unlockedRoutesFlow: Flow<Set<String>> = manager.unlockedRoutesFlow

    override suspend fun saveGame(state: GameState) = manager.saveGame(state)

    override suspend fun unlockRoute(routeId: String) = manager.saveUnlockedRoute(routeId)
}
