// GERADO por tools/walk_cycle/build_walk_cycle.py — não editar à mão.
package com.pulchradomina.ui.game.night

/**
 * Walk cycle lateral do Edgar (quadros em night_street/walk/walk_NN.png, centrados no idle, pés na linha do chão).
 * A animação anda pela distância percorrida: cada ciclo = [CYCLE_UNITS] unidades.
 * Contatos: quadros em que um pé toca o chão; cada um dispara um passo.
 */
internal object WalkCycleLayout {
    const val FRAME_COUNT = 71
    /** Fps nativo da sprite sheet (referência). */
    const val NATIVE_FPS = 30
    const val FRAME_WIDTH_U = 165.00f
    const val FRAME_HEIGHT_U = 329.01f
    /** Distância percorrida num ciclo completo (dois passos), em unidades de referência. */
    const val CYCLE_UNITS = 282.47f
    /** Quadros (0-based) em que um pé toca o chão, na ordem do ciclo. */
    val CONTACT_FRAMES: IntArray = intArrayOf(32, 68)
    /** Mesma escala do idle: a figura de 3680 px tem 330 unidades. */
    const val UNITS_PER_SOURCE_PX = 0.089674f
}
