package app.ytdlclean.ui.navigation

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.ytdlclean.data.DownloadManager
import app.ytdlclean.di.AppContainer
import app.ytdlclean.ui.downloads.DownloadsScreen
import app.ytdlclean.ui.downloads.DownloadsViewModel
import app.ytdlclean.ui.home.HomeScreen
import app.ytdlclean.ui.home.HomeViewModel
import app.ytdlclean.ui.settings.SettingsScreen
import app.ytdlclean.ui.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavHost(initialSharedUrl: String?) {
    RequestNotificationPermission()

    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState().value

    val items = listOf(
        NavItem(Routes.HOME, "Download", Icons.Outlined.Download),
        NavItem(Routes.DOWNLOADS, "Library", Icons.Outlined.VideoLibrary),
        NavItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = backStackEntry?.destination?.route == item.route ||
                        backStackEntry?.destination?.route?.startsWith(item.route) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = appFactory { HomeViewModel(it) })
                HomeScreen(
                    viewModel = vm,
                    initialSharedUrl = initialSharedUrl,
                    onNavigateLibrary = {
                        navController.navigate(Routes.DOWNLOADS) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.DOWNLOADS) {
                val vm: DownloadsViewModel = viewModel(factory = appFactory { DownloadsViewModel(it) })
                DownloadsScreen(viewModel = vm)
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = appFactory { SettingsViewModel(it) })
                SettingsScreen(viewModel = vm)
            }
        }
    }
}

/** A [ViewModelProvider.Factory] that hands the shared [DownloadManager] to the view model. */
@Composable
private fun appFactory(create: (DownloadManager) -> ViewModel): ViewModelProvider.Factory {
    val container = AppContainer.get(LocalContext.current.applicationContext as Application)
    return viewModelFactory { initializer { create(container) } }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored; download works regardless */ }
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
