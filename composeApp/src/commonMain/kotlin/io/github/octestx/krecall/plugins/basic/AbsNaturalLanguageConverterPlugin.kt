package io.github.octestx.krecall.plugins.basic

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsNaturalLanguageConverterPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun convert(natural: String): AIResult<String>
}