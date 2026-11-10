package com.example.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.ContentDrawScope
import androidx.compose.ui.graphics.Color

/**
 * Feedback de clique para qualquer botão do app: enquanto o dedo estiver
 * pressionado, o elemento fica mais escuro. Substitui o ripple padrão.
 * Fornecido via LocalIndication em MainActivity, então vale para Material
 * (Button, OutlinedButton, TextButton, IconButton) e para os botões custom.
 */
object PressDarkenIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val pressed = interactionSource.collectIsPressedAsState()
        return remember(interactionSource) { DarkenInstance(pressed) }
    }
}

private class DarkenInstance(private val pressed: State<Boolean>) : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        drawContent()
        if (pressed.value) {
            drawRect(color = Color.Black.copy(alpha = PRESS_DARKEN_ALPHA))
        }
    }
}

private const val PRESS_DARKEN_ALPHA = 0.35f
