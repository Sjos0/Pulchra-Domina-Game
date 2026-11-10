package com.pulchradomina.ui.viewmodel

/**
 * Estado de navegação e chrome da UI (sem lógica de história).
 */
data class NavigationState(
    val currentScreen: AppScreen = AppScreen.BOOT_SEQUENCE,
    val isMenuOpen: Boolean = false,
    val showUpdateDialog: Boolean = false
)
