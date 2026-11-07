package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun UpdatesDialog(
    onDismiss: () -> Unit
) {
    var isChecking by remember { mutableStateOf(false) }
    var checkedResult by remember { mutableStateOf("Você está utilizando a versão mais recente e otimizada.") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("updates_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard,
            border = BorderStroke(1.5.dp, BorderGothic),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = VioletLight
                        )
                        Text(
                            text = "Atualizações do Jogo",
                            color = PaleText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_updates_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MutedText
                        )
                    }
                }

                HorizontalDivider(color = BorderGothic)

                // Informações da Versão Atual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AbyssalPurple)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Versão Instalada: v1.0.0 (Build 1)",
                            color = SilverMist,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Capítulo 1: O Encontro na Balada (MVP)",
                            color = VioletLight,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Compatibilidade: Android 8.0+ (API 26)",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AllanColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = checkedResult,
                        color = PaleText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Text(
                    text = "O sistema de distribuição compila artefatos assinados diretamente na pasta APK da raiz do projeto, mantendo versionamento rigoroso e baixo consumo de dados.",
                    color = MutedText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_updates_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entendido", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
