package com.example.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * Feedback de clique para qualquer botão do app: enquanto o dedo estiver
 * pressionado, o elemento escurece. Substitui o ripple padrão.
 * Fornecido via LocalIndication em MainActivity: vale para Material
 * (Button, OutlinedButton, TextButton, IconButton) e para os botões custom.
 *
 * Usa IndicationNodeFactory: Indication.rememberUpdatedInstance e
 * IndicationInstance estão deprecated com nível ERROR no Compose 1.7+.
 */
object PressDarkenIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressDarkenNode(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}

private class PressDarkenNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private var isPressed = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> isPressed = true
                    is PressInteraction.Release -> isPressed = false
                    is PressInteraction.Cancel -> isPressed = false
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (isPressed) {
            drawRect(color = Color.Black.copy(alpha = PRESS_DARKEN_ALPHA))
        }
    }
}

private const val PRESS_DARKEN_ALPHA = 0.35f
