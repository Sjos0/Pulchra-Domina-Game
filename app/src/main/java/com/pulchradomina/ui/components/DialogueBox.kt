package com.pulchradomina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulchradomina.domain.story.CharacterType
import com.pulchradomina.ui.theme.*

@Composable
fun DialogueBox(
    location: String,
    character: CharacterType,
    text: String,
    innerThoughts: String?,
    hasChoices: Boolean,
    canAdvance: Boolean,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dialogue_box")
            .then(
                if (canAdvance) Modifier.clickable { onAdvance() } else Modifier
            ),
        color = SurfaceCard.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, BorderGothic),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Localização e Tipo de Cena
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Localização da cena",
                    tint = VioletLight.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = location,
                    color = VioletLight.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            // Texto do Diálogo
            Text(
                text = text,
                color = PaleText,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = if (character == CharacterType.NARRATOR) FontWeight.Normal else FontWeight.Medium,
                modifier = Modifier.padding(bottom = if (innerThoughts != null) 10.dp else 0.dp)
            )

            // Pensamento Interno
            if (innerThoughts != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AbyssalPurple.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = innerThoughts,
                        color = MutedText,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                }
            }

            // Indicador de avanço linear (sem escolhas e com próximo nó)
            if (canAdvance) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VioletAccent.copy(alpha = 0.2f))
                        .clickable { onAdvance() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("advance_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Toque para avançar",
                        color = VioletLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Avançar",
                        tint = VioletLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
