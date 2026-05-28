@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.checkarr.data.models.ServiceType
import com.checkarr.navigation.Screen
import com.checkarr.ui.MainViewModel
import com.checkarr.ui.theme.accentColor

@Composable
fun SettingsScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val appConfig by viewModel.appConfig.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val accentColorName by viewModel.accentColor.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Paramètres") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {

            item {
                SectionHeaderLocal("Services")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Configuration des services") },
                            supportingContent = {
                                val enabled = appConfig.serviceConfigs.values.count { it.enabled }
                                Text("$enabled service(s) activé(s)")
                            },
                            leadingContent = { Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { navController.navigate(Screen.Setup.route) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
                        // Show enabled services
                        ServiceType.values().forEach { svc ->
                            val cfg = appConfig.serviceConfigs[svc.name]
                            if (cfg?.enabled == true) {
                                ListItem(
                                    headlineContent = { Text(svc.label, style = MaterialTheme.typography.bodyMedium) },
                                    supportingContent = { Text(cfg.url, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.size(8.dp).background(Color(svc.color), CircleShape)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeaderLocal("Apparence")
                ListItem(
                    headlineContent = { Text("Thème") },
                    supportingContent = { Text(when(theme) { "dark" -> "Sombre"; "light" -> "Clair"; else -> "Système" }) },
                    leadingContent = { Icon(Icons.Default.DarkMode, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { navController.navigate(Screen.Appearance.route) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
                ListItem(
                    headlineContent = { Text("Couleur d'accentuation") },
                    supportingContent = {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(accentColor(accentColorName)))
                    },
                    leadingContent = { Icon(Icons.Default.Palette, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { navController.navigate(Screen.Appearance.route) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
            }

            item {
                SectionHeaderLocal("À propos")
                ListItem(
                    headlineContent = { Text("Checkarr") },
                    supportingContent = { Text("Version 1.0.0") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderLocal(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun AppearanceScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val theme by viewModel.theme.collectAsState()
    val accentColorName by viewModel.accentColor.collectAsState()
    val accentColors = listOf(
        "blue" to Color(0xFF3B82F6), "indigo" to Color(0xFF6366F1),
        "purple" to Color(0xFFA855F7), "pink" to Color(0xFFEC4899),
        "red" to Color(0xFFEF4444), "orange" to Color(0xFFF97316),
        "yellow" to Color(0xFFEAB308), "green" to Color(0xFF22C55E),
        "mint" to Color(0xFF14B8A6), "cyan" to Color(0xFF06B6D4)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apparence") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Thème", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            listOf("system" to "Système", "dark" to "Sombre", "light" to "Clair").forEach { (t, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        if (theme == t) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        else Spacer(Modifier.size(24.dp))
                    },
                    modifier = Modifier.clickable { viewModel.setTheme(t) }
                )
            }
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Couleur d'accentuation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accentColors.forEach { (name, color) ->
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                            .border(width = if (accentColorName == name) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                            .clickable { viewModel.setAccentColor(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (accentColorName == name) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
