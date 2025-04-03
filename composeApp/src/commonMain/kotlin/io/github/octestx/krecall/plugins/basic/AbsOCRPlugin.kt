package io.github.octestx.krecall.plugins.basic

import kotlinx.serialization.Serializable

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsOCRPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun recognize(screen: ByteArray): OCRResult
}

@Serializable
data class OCRResult(
    val text: String,
)