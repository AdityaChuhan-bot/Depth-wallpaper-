package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.crop.ImageCropScreen
import com.example.ui.screens.editor.WallpaperEditorScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.preview.LockScreenPreviewScreen
import com.example.ui.screens.segment.DepthSegmentScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.viewmodel.DepthLockViewModel

@Composable
fun DepthLockNavGraph(
    navController: NavHostController,
    viewModel: DepthLockViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCrop = { navController.navigate(Screen.Crop.route) },
                onNavigateToEditor = { navController.navigate(Screen.Editor.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPreview = { navController.navigate(Screen.Preview.route) }
            )
        }

        composable(Screen.Crop.route) {
            ImageCropScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSegment = { navController.navigate(Screen.Segment.route) }
            )
        }

        composable(Screen.Segment.route) {
            DepthSegmentScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { navController.navigate(Screen.Editor.route) }
            )
        }

        composable(Screen.Editor.route) {
            WallpaperEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSegment = { navController.navigate(Screen.Segment.route) },
                onNavigateToPreview = { navController.navigate(Screen.Preview.route) }
            )
        }

        composable(Screen.Preview.route) {
            LockScreenPreviewScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
