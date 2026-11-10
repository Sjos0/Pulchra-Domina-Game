package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import com.example.R
import com.example.ui.components.PixelFrameBox
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ChapterOneStoryRepository
import com.example.domain.MacroRouteInfo
import com.example.ui.theme.*

@Composable
fun RoutesGalleryScreen(
    unlockedRoutes: Set<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val routesList = ChapterOneStoryRepository.routes.values.toList()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("routes_gallery_screen"),
        topBar = {
            Surface(
                color = AbyssalPurple,
                border = BorderStroke(0.dp, BorderGothic)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("back_from_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = PaleText
                        )
                    }

                    Column {
                        Text(
                            text = "Galeria de Macro-Rotas",
                            color = PaleText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${unlockedRoutes.size} de ${routesList.size} rotas desbloqueadas",
                            color = VioletLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        containerColor = ObsidianDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianDark,
                            AbyssalPurple,
                            ObsidianDark
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "No Capítulo 1, suas decisões com Maveth, Noor e a Hortênsia Negra determinam qual das 6 macro-rotas você alcançará.",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(routesList) { route ->
                val isUnlocked = unlockedRoutes.contains(route.code)
                RouteCard(route = route, isUnlocked = isUnlocked)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private val WoodText = Color(0xFFFFF3DC)
private val WoodMuted = Color(0xFFE8D6B8)
private val LockedTint = Color.Black.copy(alpha = 0.45f)
private val ChipBackground = Color.Black.copy(alpha = 0.72f)
private val CardTextShadow = TextStyle(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.85f),
        offset = Offset(1.5f, 1.5f),
        blurRadius = 3f
    )
)

@Composable
private fun RouteCard(
    route: MacroRouteInfo,
    isUnlocked: Boolean
) {
    PixelFrameBox(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 18.dp,
        woodRes = R.drawable.box_wood_flower,
        tint = if (isUnlocked) Color.Transparent else LockedTint
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha 1: chip da rota à esquerda, status à direita (sem disputar largura com o título)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RouteChip(
                    text = "Rota ${route.code}",
                    color = if (isUnlocked) route.color else MutedText
                )
                StatusTag(isUnlocked = isUnlocked)
            }

            // Linha 2: título com largura total; quebra em até duas linhas
            Text(
                text = route.title,
                color = if (isUnlocked) WoodText else WoodMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = CardTextShadow
            )

            Text(
                text = route.subtitle,
                color = if (isUnlocked) route.color else WoodMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = CardTextShadow
            )

            Text(
                text = if (isUnlocked) route.description else "Complete o Capítulo 1 tomando escolhas compatíveis com este caminho para revelar o desfecho desta rota.",
                color = if (isUnlocked) WoodText else WoodMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                style = CardTextShadow
            )
        }
    }
}

@Composable
private fun RouteChip(text: String, color: Color) {
    Surface(
        color = ChipBackground,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.8f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatusTag(isUnlocked: Boolean) {
    val tint = if (isUnlocked) AllanColor else MutedText
    val label = if (isUnlocked) "Desbloqueada" else "Bloqueada"
    Surface(
        color = ChipBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
