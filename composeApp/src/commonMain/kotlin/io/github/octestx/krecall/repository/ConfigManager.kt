package io.github.octestx.krecall.repository

import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.plugins.PluginSelector
import io.klogging.noCoLogger
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import java.io.File

object ConfigManager {
    private val ologger = noCoLogger<ConfigManager>()
    lateinit var pluginConfig: KRecallPluginConfig private set
    fun reload(configPath: Path) {
        val file = File(configPath.toString())
        pluginConfig = runCatching {
            ojson.decodeFromString<KRecallPluginConfig>(file.readText())
        }.getOrElse {
            val config2 = DEFAULT
            file.writeText(ojson.encodeToString(config2))
            config2
        }
    }

    private val DEFAULT = KRecallPluginConfig(
        PluginSelector.recommendGetScreenPlugin().pluginId,
        PluginSelector.recommendStoragePlugin().pluginId,
        PluginSelector.recommendNaturalLanguageConverterPlugin().pluginId,
        PluginSelector.recommendScreenLanguageConverterPlugin().pluginId,
        false
    )

    @Serializable
    data class KRecallPluginConfig(
        val getScreenPluginId: String,
        val storagePluginId: String,
        val naturalLanguageConverterPluginId: String,
        val screenLanguageConverterPluginId: String,
        val initialized: Boolean
    )
}