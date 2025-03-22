package io.github.octestx.krecall.plugins.basic

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsOCRPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun recognize(screen: ByteArray): AIResult<String>
}