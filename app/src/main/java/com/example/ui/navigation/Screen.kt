package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Crop : Screen("crop")
    data object Segment : Screen("segment")
    data object Editor : Screen("editor")
    data object Preview : Screen("preview")
    data object Settings : Screen("settings")
}
