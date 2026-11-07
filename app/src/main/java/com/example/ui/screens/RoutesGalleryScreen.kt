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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ChapterOneStory
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

    val routesList = ChapterOneStory.ROUTES.values.toList()

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

@Composable
private fun RouteCard(
    route: MacroRouteInfo,
    isUnlocked: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isUnlocked) SurfaceCard else SurfaceGothic.copy(alpha = 0.6f),
        border = BorderStroke(
            1.2.dp,
            if (isUnlocked) route.color.copy(alpha = 0.7f) else BorderGothic
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (isUnlocked) route.color.copy(alpha = 0.25f) else ObsidianDark,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Rota ${route.code}",
                            color = if (isUnlocked) route.color else MutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = route.title,
                        color = if (isUnlocked) PaleText else MutedText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = if (isUnlocked) "Desbloqueada" else "Bloqueada",
                        tint = if (isUnlocked) AllanColor else MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isUnlocked) "Desbloqueada" else "Bloqueada",
                        color = if (isUnlocked) AllanColor else MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = route.subtitle,
                color = if (isUnlocked) route.color else MutedText.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = if (isUnlocked) route.description else "Complete o Capítulo 1 tomando escolhas compatíveis com este caminho para revelar o desfecho desta rota.",
                color = if (isUnlocked) SilverMist else MutedText.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
