package io.github.octestx.krecall.plugins.basic

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsScreenLanguageConverterPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun convert(screen: ByteArray): AIResult<String>
}