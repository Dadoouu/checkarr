@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.checkarr.data.models.*
import com.checkarr.ui.MainViewModel

@Composable
fun SetupScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val appConfig by viewModel.appConfig.collectAsState()

    var setupMode by remember(appConfig.setupMode) { mutableStateOf(appConfig.setupMode) }
    var baseInput by remember(appConfig.baseIp, appConfig.baseDomain) {
        mutableStateOf(if (appConfig.setupMode == "domain") appConfig.baseDomain else appConfig.baseIp)
    }
    var serviceConfigs by remember(appConfig.serviceConfigs) {
        mutableStateOf(appConfig.serviceConfigs.toMutableMap())
    }

    fun autoFill() {
        if (baseInput.isBlank() || setupMode == "manual") return
        ServiceType.values().forEach { svc ->
            val url = if (setupMode == "ip") {
                val ip = baseInput.trim().trimEnd('/')
                val withProto = if (ip.startsWith("http")) ip else "http://$ip"
                "$withProto:${svc.defaultPort}"
            } else {
                val domain = baseInput.trim().trimEnd('/')
                val withProto = if (domain.startsWith("http")) domain else "https://$domain"
                "${withProto.substringBefore("://").plus("://")}${svc.defaultSubdomain}.${domain.removePrefix("https://").removePrefix("http://")}"
            }
            val existing = serviceConfigs[svc.name] ?: ServiceConfig()
            serviceConfigs = serviceConfigs.toMutableMap().also { it[svc.name] = existing.copy(url = url) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration des services") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveAppConfig(AppConfig(
                                setupMode = setupMode,
                                baseIp = if (setupMode == "ip") baseInput else appConfig.baseIp,
                                baseDomain = if (setupMode == "domain") baseInput else appConfig.baseDomain,
                                serviceConfigs = serviceConfigs
                            ))
                            navController.popBackStack()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Sauvegarder") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode selector
            Text("Mode de configuration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ip" to "Adresse IP", "domain" to "Domaine", "manual" to "Manuel").forEach { (mode, label) ->
                    FilterChip(selected = setupMode == mode, onClick = { setupMode = mode }, label = { Text(label) })
                }
            }

            if (setupMode != "manual") {
                OutlinedTextField(
                    value = baseInput,
                    onValueChange = { baseInput = it },
                    label = { Text(if (setupMode == "ip") "Adresse IP" else "Nom de domaine") },
                    placeholder = { Text(if (setupMode == "ip") "192.168.1.100" else "monserveur.com") },
                    leadingIcon = { Icon(if (setupMode == "ip") Icons.Default.Computer else Icons.Default.Language, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(onClick = { autoFill() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remplir les URLs automatiquement")
                }
                HorizontalDivider()
            }

            Text("Services", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            ServiceType.values().forEach { svc ->
                val config = serviceConfigs[svc.name] ?: ServiceConfig()
                ServiceConfigCard(
                    serviceType = svc,
                    config = config,
                    onConfigChange = {
                        serviceConfigs = serviceConfigs.toMutableMap().also { m -> m[svc.name] = it }
                    }
                )
            }
        }
    }
}

@Composable
fun ServiceConfigCard(serviceType: ServiceType, config: ServiceConfig, onConfigChange: (ServiceConfig) -> Unit) {
    var showSecret by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color(serviceType.color).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(serviceType.label.take(1), style = MaterialTheme.typography.titleMedium,
                        color = Color(serviceType.color), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(serviceType.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Port par défaut: ${serviceType.defaultPort}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = config.enabled, onCheckedChange = { onConfigChange(config.copy(enabled = it)) })
            }

            if (config.enabled) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = config.url,
                    onValueChange = { onConfigChange(config.copy(url = it)) },
                    label = { Text("URL") },
                    placeholder = { Text("http://ip:${serviceType.defaultPort}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                when (serviceType) {
                    ServiceType.QBITTORRENT -> {
                        OutlinedTextField(
                            value = config.username,
                            onValueChange = { onConfigChange(config.copy(username = it)) },
                            label = { Text("Nom d'utilisateur") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = config.password,
                            onValueChange = { onConfigChange(config.copy(password = it)) },
                            label = { Text("Mot de passe") },
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecret = !showSecret }) {
                                    Icon(if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    else -> {
                        OutlinedTextField(
                            value = config.apiKey,
                            onValueChange = { onConfigChange(config.copy(apiKey = it)) },
                            label = { Text("Clé API") },
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecret = !showSecret }) {
                                    Icon(if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}
