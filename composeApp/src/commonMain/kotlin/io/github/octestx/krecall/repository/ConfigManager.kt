package io.github.octestx.krecall.repository

import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.plugins.PluginSelector
import io.klogging.noCoLogger
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import java.io.File

object ConfigManager {
    private val ologger = noCoLogger<ConfigManager>()
    lateinit var config: KRecallConfig private set
    lateinit var pluginConfig: KRecallPluginConfig private set
    fun reload() {
        reloadKRecallConfig()
        reloadKRecallPluginConfig()
    }
    private fun reloadKRecallConfig() {
        val file = File(FileTree.configDir.toString(), "config.json")
        config = runCatching {
            ojson.decodeFromString<KRecallConfig>(file.readText())
        }.getOrElse {
            val config2 = DefaultConfig
            file.writeText(ojson.encodeToString(config2))
            config2
        }
    }
    @Synchronized
    fun save(newConfig: KRecallConfig) {
        val file = File(FileTree.configDir.toString(), "config.json")
        config = newConfig
        file.writeText(ojson.encodeToString(newConfig))
        reloadKRecallConfig()
    }
    private val DefaultConfig = KRecallConfig(
        15 * 1000,
        false
    )

    @Serializable
    data class KRecallConfig(
        val collectScreenDelay: Long,//Default 15 * 1000 [15s]
        val initialized: Boolean
    )





    private fun reloadKRecallPluginConfig() {
        val file = File(FileTree.configDir.toString(), "configPlugin.json")
        pluginConfig = runCatching {
            ojson.decodeFromString<KRecallPluginConfig>(file.readText())
        }.getOrElse {
            val config2 = DefaultPluginConfig
            file.writeText(ojson.encodeToString(config2))
            config2
        }
    }
    @Synchronized
    fun savePluginConfig(newConfig: KRecallPluginConfig) {
        val file = File(FileTree.configDir.toString(), "configPlugin.json")
        pluginConfig = newConfig
        file.writeText(ojson.encodeToString(pluginConfig))
    }

    private val DefaultPluginConfig = KRecallPluginConfig(
        PluginSelector.recommendGetScreenPlugin().pluginId,
        PluginSelector.recommendStoragePlugin().pluginId,
        PluginSelector.recommendNaturalLanguageConverterPlugin().pluginId,
        PluginSelector.recommendScreenLanguageConverterPlugin().pluginId,
    )

    @Serializable
    data class KRecallPluginConfig(
        val getScreenPluginId: String,
        val storagePluginId: String,
        val naturalLanguageConverterPluginId: String,
        val screenLanguageConverterPluginId: String,
    )
}