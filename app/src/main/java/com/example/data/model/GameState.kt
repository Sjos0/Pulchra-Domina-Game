package com.example.data.model

/**
 * Representa o estado do jogo Pulchra Domina no Capítulo 1 e posteriores.
 */
data class GameState(
    val pont_cap: Int = 0,
    val pont_total: Int = 0,
    val confianca_maveth: Int = 0,
    val pegou_flor: Boolean = false,
    val escolhas_feitas: List<String> = emptyList(),
    val capitulo_atual: String = "capitulo1",
    val nodeId: String = "inicio",
    val macroRotaFinal: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
