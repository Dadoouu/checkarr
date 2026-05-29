@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.checkarr.data.models.*
import com.checkarr.navigation.Screen
import com.checkarr.ui.activity.ActivityScreen
import com.checkarr.ui.calendar.CalendarScreen
import com.checkarr.ui.dashboard.DashboardScreen
import com.checkarr.ui.dashboard.DashboardViewModel
import com.checkarr.ui.jellyfin.JellyfinScreen
import com.checkarr.ui.jellyfin.JellyfinViewModel
import com.checkarr.ui.maintainerr.MaintainerrScreen
import com.checkarr.ui.maintainerr.MaintainerrViewModel
import com.checkarr.ui.movies.*
import com.checkarr.ui.prowlarr.ProwlarrScreen
import com.checkarr.ui.prowlarr.ProwlarrViewModel
import com.checkarr.ui.qbittorrent.QBittorrentScreen
import com.checkarr.ui.seerr.SeerrScreen
import com.checkarr.ui.seerr.SeerrViewModel
import com.checkarr.ui.series.*
import com.checkarr.ui.settings.*
import kotlinx.coroutines.launch

data class NavSection(val title: String, val items: List<NavItem>)
data class NavItem(val label: String, val route: String, val icon: ImageVector, val color: Color)

val navSections = listOf(
    NavSection("", listOf(
        NavItem("Tableau de bord", Screen.Dashboard.route, Icons.Default.Dashboard, Color(0xFF6B7280))
    )),
    NavSection("Médias", listOf(
        NavItem("Films", Screen.Movies.route, Icons.Default.Movie, Color(0xFFFFAC33)),
        NavItem("Séries", Screen.Series.route, Icons.Default.Tv, Color(0xFF00D4FF)),
        NavItem("Calendrier", Screen.Calendar.route, Icons.Default.CalendarMonth, Color(0xFF22C55E)),
        NavItem("Activité", Screen.Activity.route, Icons.Default.WifiTethering, Color(0xFF8B5CF6))
    )),
    NavSection("Téléchargement", listOf(
        NavItem("qBittorrent", Screen.QBittorrent.route, Icons.Default.Download, Color(0xFF6BBF4E))
    )),
    NavSection("Serveurs", listOf(
        NavItem("Jellyfin", Screen.Jellyfin.route, Icons.Default.CellTower, Color(0xFF00A4DC)),
        NavItem("Maintainerr", Screen.Maintainerr.route, Icons.Default.CleaningServices, Color(0xFFEC4899)),
        NavItem("Prowlarr", Screen.Prowlarr.route, Icons.Default.ManageSearch, Color(0xFFFF6B35)),
        NavItem("Seerr", Screen.Seerr.route, Icons.Default.VideoCall, Color(0xFF7C3AED))
    )),
    NavSection("", listOf(
        NavItem("Paramètres", Screen.Settings.route, Icons.Default.Settings, Color(0xFF6B7280))
    ))
)

@Composable
fun RuddarrApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val radarrInstance by viewModel.selectedRadarrInstance.collectAsState()
    val sonarrInstance by viewModel.selectedSonarrInstance.collectAsState()
    val jellyfinInstance by viewModel.selectedJellyfinInstance.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    val moviesViewModel: MoviesViewModel = viewModel()
    val seriesViewModel: SeriesViewModel = viewModel()
    val jellyfinViewModel: JellyfinViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val prowlarrViewModel: ProwlarrViewModel = viewModel()
    val seerrViewModel: SeerrViewModel = viewModel()
    val maintainerrViewModel: MaintainerrViewModel = viewModel()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(currentRoute = currentRoute) { route ->
                scope.launch { drawerState.close() }
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) {
        Scaffold { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        radarrInstance = radarrInstance,
                        sonarrInstance = sonarrInstance,
                        jellyfinInstance = jellyfinInstance,
                        appConfig = appConfig,
                        viewModel = dashboardViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Movies.route) {
                    MoviesScreen(instance = radarrInstance, navController = navController, viewModel = moviesViewModel)
                }
                composable(Screen.MovieSearch.route) {
                    val qp by moviesViewModel.qualityProfiles.collectAsState()
                    val rf by moviesViewModel.rootFolders.collectAsState()
                    MovieSearchScreen(instance = radarrInstance, qualityProfiles = qp, rootFolders = rf, navController = navController, viewModel = moviesViewModel)
                }
                composable(Screen.MovieDetail.route) { back ->
                    val id = back.arguments?.getString("movieId")?.toIntOrNull() ?: return@composable
                    MovieDetailScreen(movieId = id, instance = radarrInstance, navController = navController, viewModel = moviesViewModel)
                }
                composable(Screen.MovieReleases.route) { back ->
                    val id = back.arguments?.getString("movieId")?.toIntOrNull() ?: return@composable
                    MovieReleasesScreen(movieId = id, instance = radarrInstance, navController = navController, viewModel = moviesViewModel)
                }
                composable(Screen.Series.route) {
                    SeriesScreen(instance = sonarrInstance, navController = navController, viewModel = seriesViewModel)
                }
                composable(Screen.SeriesSearch.route) {
                    SeriesSearchScreen(instance = sonarrInstance, navController = navController, viewModel = seriesViewModel)
                }
                composable(Screen.SeriesDetail.route) { back ->
                    val id = back.arguments?.getString("seriesId")?.toIntOrNull() ?: return@composable
                    SeriesDetailScreen(seriesId = id, instance = sonarrInstance, navController = navController, viewModel = seriesViewModel)
                }
                composable(Screen.SeasonDetail.route) { back ->
                    val sid = back.arguments?.getString("seriesId")?.toIntOrNull() ?: return@composable
                    val sn = back.arguments?.getString("seasonNumber")?.toIntOrNull() ?: return@composable
                    SeasonDetailScreen(seriesId = sid, seasonNumber = sn, instance = sonarrInstance, navController = navController, viewModel = seriesViewModel)
                }
                composable(Screen.EpisodeReleases.route) { back ->
                    val id = back.arguments?.getString("episodeId")?.toIntOrNull() ?: return@composable
                    EpisodeReleasesScreen(episodeId = id, instance = sonarrInstance, navController = navController, viewModel = seriesViewModel)
                }
                composable(Screen.Calendar.route) {
                    CalendarScreen(radarrInstance = radarrInstance, sonarrInstance = sonarrInstance, navController = navController)
                }
                composable(Screen.Activity.route) {
                    ActivityScreen(radarrInstance = radarrInstance, sonarrInstance = sonarrInstance, navController = navController)
                }
                composable(Screen.QBittorrent.route) {
                    QBittorrentScreen(config = appConfig.serviceConfigs[ServiceType.QBITTORRENT.name])
                }
                composable(Screen.Jellyfin.route) {
                    JellyfinScreen(instance = jellyfinInstance, navController = navController, viewModel = jellyfinViewModel)
                }
                composable(Screen.Prowlarr.route) {
                    val prowlarrConfig = appConfig.serviceConfigs[ServiceType.PROWLARR.name]
                    val prowlarrInstance = prowlarrConfig
                        ?.takeIf { it.enabled && it.url.isNotBlank() && it.apiKey.isNotBlank() }
                        ?.let { Instance(type = InstanceType.PROWLARR, label = "Prowlarr", url = it.url, apiKey = it.apiKey) }
                    ProwlarrScreen(instance = prowlarrInstance, viewModel = prowlarrViewModel)
                }
                composable(Screen.Seerr.route) {
                    val seerrConfig = appConfig.serviceConfigs[ServiceType.SEERR.name]
                    SeerrScreen(
                        url = seerrConfig?.url?.takeIf { it.isNotBlank() },
                        apiKey = seerrConfig?.apiKey?.takeIf { it.isNotBlank() },
                        viewModel = seerrViewModel
                    )
                }
                composable(Screen.Maintainerr.route) {
                    val mtConfig = appConfig.serviceConfigs[ServiceType.MAINTAINERR.name]
                    MaintainerrScreen(
                        url = mtConfig?.url?.takeIf { it.isNotBlank() },
                        apiKey = mtConfig?.apiKey?.takeIf { it.isNotBlank() },
                        viewModel = maintainerrViewModel
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Setup.route) {
                    SetupScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Appearance.route) {
                    AppearanceScreen(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppDrawer(currentRoute: String?, onNavigate: (String) -> Unit) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Radar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("Checkarr", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        navSections.forEach { section ->
            if (section.title.isNotBlank()) {
                Text(section.title.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp))
            }
            section.items.forEach { item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, null, tint = if (currentRoute == item.route) item.color else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = { Text(item.label, fontWeight = if (currentRoute == item.route) FontWeight.SemiBold else FontWeight.Normal) },
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = item.color.copy(alpha = 0.12f))
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String, icon: ImageVector, color: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(64.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Bientôt disponible", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
