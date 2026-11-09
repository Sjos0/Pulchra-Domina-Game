package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.UpdateManager
import com.example.ui.theme.MutedText
import com.example.ui.theme.PixelFontFamily
import com.example.ui.theme.VioletLight
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PixelFrameBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 12.dp)
                .testTag("updates_dialog"),
            contentPadding = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFFFFE0A0),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Atualizações do Jogo",
                            color = Color(0xFFFFF0D0),
                            fontFamily = PixelFontFamily,
                            fontSize = 15.sp,
                            lineHeight = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFFFFE0A0)
                        )
                    }
                }

                Text(
                    text = "Versão Instalada: v${updateManager.getCurrentVersion()} (Build ${BuildConfig.VERSION_CODE})",
                    color = Color(0xFFFFF0D0),
                    fontFamily = PixelFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "Capítulo 1: O Encontro na Balada (MVP)",
                    color = VioletLight,
                    fontFamily = PixelFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Text(
                    text = "Fonte: GitHub Releases (Sjos0/Pulchra-Domina-Game)",
                    color = Color(0xFFD0B090),
                    fontFamily = PixelFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Text(
                    text = statusMessage,
                    color = Color(0xFFFFF0D0),
                    fontFamily = PixelFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE8A040),
                        trackColor = Color(0xFF4A2A10)
                    )
                }

                // Botões empilhados — texto completo, sem corte
                PixelWoodButton(
                    text = if (isChecking) "Verificando…" else "Verificar",
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
                    height = 52.dp,
                    fontSize = 14.sp,
                    testTag = "check_update_button"
                )

                val canDownload = checkResult?.status == UpdateManager.Status.UPDATE_AVAILABLE &&
                    checkResult?.release?.downloadUrl != null &&
                    !isDownloading && !isChecking

                PixelWoodButton(
                    text = if (isDownloading) "Baixando… $downloadProgress%" else "Atualizar",
                    onClick = {
                        val url = checkResult?.release?.downloadUrl ?: return@PixelWoodButton
                        scope.launch {
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
                                statusMessage = if (ok) {
                                    "Instalador aberto. Conclua a instalação na tela do sistema."
                                } else {
                                    "Não foi possível abrir o instalador. Tente baixar de novo."
                                }
                            } else {
                                statusMessage = "Falha no download do APK."
                            }
                        }
                    },
                    enabled = canDownload,
                    height = 52.dp,
                    fontSize = 14.sp,
                    testTag = "download_update_button"
                )

                if (needsPermission) {
                    Text(
                        text = "Ative a permissão de instalar apps desconhecidos para este app.",
                        color = Color(0xFFFFB0A0),
                        fontFamily = PixelFontFamily,
                        fontSize = 11.sp
                    )
                    PixelWoodButton(
                        text = "Permitir instalação",
                        onClick = {
                            updateManager.openInstallPermissionSettings()
                            needsPermission = false
                        },
                        height = 52.dp,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "O APK é baixado para o cache do app, instalado pelo sistema e apagado em seguida.",
                    color = Color(0xFFD0B090),
                    fontFamily = PixelFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("dismiss_updates_button")
                ) {
                    Text(
                        "Fechar",
                        color = MutedText,
                        fontFamily = PixelFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
