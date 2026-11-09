package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UpdateManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun UpdatesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf("Toque em Verificar para consultar o GitHub.") }
    var checkResult by remember { mutableStateOf<UpdateManager.CheckResult?>(null) }
    var needsPermission by remember { mutableStateOf(false) }

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
                // Header
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

                // Current version box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AbyssalPurple)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Versão Instalada: v${updateManager.getCurrentVersion()} (Build 5)",
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
                            text = "Fonte: GitHub Releases (Sjos0/Pulchra-Domina-Game)",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                }

                // Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val icon = when (checkResult?.status) {
                        UpdateManager.Status.UP_TO_DATE -> Icons.Default.CheckCircle
                        UpdateManager.Status.UPDATE_AVAILABLE -> Icons.Default.Download
                        UpdateManager.Status.ERROR, UpdateManager.Status.NO_RELEASES -> Icons.Default.Warning
                        null -> Icons.Default.SystemUpdate
                    }
                    val tint = when (checkResult?.status) {
                        UpdateManager.Status.UP_TO_DATE -> AllanColor
                        UpdateManager.Status.UPDATE_AVAILABLE -> VioletLight
                        else -> MutedText
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = statusMessage,
                        color = PaleText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = VioletAccent,
                        trackColor = AbyssalPurple
                    )
                    Text(
                        text = "Baixando… $downloadProgress%",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                }

                if (needsPermission) {
                    Text(
                        text = "É necessário permitir a instalação de apps deste aplicativo. Toque em \"Permitir instalação\".",
                        color = VioletLight,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Button(
                        onClick = {
                            updateManager.openInstallPermissionSettings()
                            needsPermission = false
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Permitir instalação", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isChecking = true
                                statusMessage = "Consultando GitHub…"
                                val result = updateManager.checkForUpdate()
                                checkResult = result
                                statusMessage = result.message
                                isChecking = false
                            }
                        },
                        enabled = !isChecking && !isDownloading,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderGothic)
                    ) {
                        Text(
                            if (isChecking) "Verificando…" else "Verificar",
                            color = PaleText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val canDownload = checkResult?.status == UpdateManager.Status.UPDATE_AVAILABLE
                            && checkResult?.release?.downloadUrl != null
                            && !isDownloading

                    Button(
                        onClick = {
                            val url = checkResult?.release?.downloadUrl ?: return@Button
                            scope.launch {
                                // Permissão de instalar
                                if (!updateManager.canRequestPackageInstalls()) {
                                    needsPermission = true
                                    statusMessage = "Permissão de instalação necessária."
                                    return@launch
                                }
                                isDownloading = true
                                downloadProgress = 0
                                statusMessage = "Baixando atualização…"
                                val file = updateManager.downloadApk(url) { p ->
                                    downloadProgress = p
                                }
                                isDownloading = false
                                if (file != null) {
                                    statusMessage = "Download concluído. Abrindo instalador…"
                                    val ok = updateManager.installApk(file)
                                    if (ok) {
                                        statusMessage = "Instalador aberto. Conclua a instalação na tela do sistema. O APK permanece no cache até a próxima verificação."
                                    } else {
                                        statusMessage = "Não foi possível abrir o instalador. Tente baixar de novo."
                                    }
                                } else {
                                    statusMessage = "Falha no download do APK."
                                }
                            }
                        },
                        enabled = canDownload,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletAccent,
                            disabledContainerColor = AbyssalPurple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (isDownloading) "Baixando…" else "Atualizar",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "O APK é baixado para o cache do app, instalado pelo sistema e apagado em seguida.",
                    color = MutedText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_updates_button")) {
                    Text("Fechar", color = MutedText)
                }
            }
        }
    }
}
