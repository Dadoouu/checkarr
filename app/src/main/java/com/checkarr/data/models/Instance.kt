package com.checkarr.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

enum class InstanceType { RADARR, SONARR, PROWLARR }

@Serializable
data class Instance(
    val id: String = UUID.randomUUID().toString(),
    val type: InstanceType = InstanceType.RADARR,
    val label: String = "",
    val url: String = "",
    val apiKey: String = "",
    val headers: Map<String, String> = emptyMap(),
    val color: String = "blue"
) {
    val displayName: String get() = label.ifBlank {
        url.removePrefix("http://").removePrefix("https://").trimEnd('/')
    }

    val isValid: Boolean get() = url.isNotBlank() && apiKey.isNotBlank()
}
