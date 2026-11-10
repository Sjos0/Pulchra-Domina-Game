package com.pulchradomina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulchradomina.ui.theme.*

@Composable
fun RelationshipIndicator(
    confiancaMaveth: Int,
    pegouFlor: Boolean,
    pontCap: Int,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = SurfaceGothic.copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGothic)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Indicador de Afinidade com Maveth
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("maveth_trust_indicator")
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MavethColor.copy(alpha = 0.2f))
                        .border(1.dp, MavethColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = MavethColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Column {
                    Text(
                        text = "Maveth",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Afinidade: $confiancaMaveth",
                        color = SilverMist,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Indicador da Hortênsia Preta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (pegouFlor) VioletAccent.copy(alpha = 0.25f) else ObsidianDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("flower_status_indicator")
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = if (pegouFlor) "Hortênsia Preta em posse" else "Sem Flor",
                    tint = if (pegouFlor) VioletLight else MutedText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (pegouFlor) "Flor Negra" else "Sem Flor",
                    color = if (pegouFlor) VioletLight else MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Pontos do Capítulo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Pontuação do Capítulo",
                    tint = RoseDark,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$pontCap",
                    color = PaleText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botão Menu
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu do Jogo",
                    tint = PaleText
                )
            }
        }
    }
}
