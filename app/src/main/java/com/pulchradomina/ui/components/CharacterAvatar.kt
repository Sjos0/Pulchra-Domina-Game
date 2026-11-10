package com.pulchradomina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulchradomina.domain.story.CharacterType
import com.pulchradomina.ui.theme.*

@Composable
fun CharacterAvatar(
    character: CharacterType,
    modifier: Modifier = Modifier
) {
    val (iconVector, bgGradient) = when (character) {
        CharacterType.MAVETH -> Pair(
            Icons.Default.NightlightRound,
            Brush.verticalGradient(listOf(HydrangeaGlow.copy(alpha = 0.5f), ObsidianDark))
        )
        CharacterType.NOOR -> Pair(
            Icons.Default.WbSunny,
            Brush.verticalGradient(listOf(NoorColor.copy(alpha = 0.4f), AbyssalPurple))
        )
        CharacterType.ALLAN -> Pair(
            Icons.Default.LocalHospital,
            Brush.verticalGradient(listOf(AllanColor.copy(alpha = 0.4f), AbyssalPurple))
        )
        CharacterType.EDGAR -> Pair(
            Icons.Default.Person,
            Brush.verticalGradient(listOf(EdgarColor.copy(alpha = 0.35f), ObsidianDark))
        )
        CharacterType.NARRATOR -> Pair(
            Icons.Default.AutoAwesome,
            Brush.verticalGradient(listOf(BorderGothic, ObsidianDark))
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(bgGradient)
                .border(1.5.dp, character.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = character.displayName,
                tint = character.color,
                modifier = Modifier.size(28.dp)
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = character.displayName,
                    color = character.color,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                if (character.title.isNotBlank()) {
                    Text(
                        text = " • ${character.title}",
                        color = MutedText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
