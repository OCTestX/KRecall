package io.github.octestx.krecall.plugins

import io.github.octestx.krecall.plugins.basic.*
import io.github.octestx.krecall.repository.ConfigManager
import io.klogging.noCoLogger
import org.koin.core.module.Module

object PluginManager {
    data class PluginState<P: PluginBasic>(
        //if plugin is null, it means the plugin is not loaded or catch on loading
        val plugin: P?,
        val initialized: Boolean,
        val err: Exception?
    )
    private val ologger = noCoLogger<PluginManager>()
    private lateinit var classLoader: ClassLoader
//    private lateinit var reflections: Reflections
    lateinit var pluginModule: Module private set

    private var _getScreenPlugin: Result<AbsGetScreenPlugin>? = null
    val getScreenPlugin get() = _getScreenPlugin
    private var _storagePlugin: Result<AbsStoragePlugin>? = null
    val storagePlugin get() = _storagePlugin
    private var _naturalLanguageConverterPlugin: Result<AbsNaturalLanguageConverterPlugin>? = null
    val naturalLanguageConverterPlugin get() = _naturalLanguageConverterPlugin
    private var _screenLanguageConverterPlugin: Result<AbsScreenLanguageConverterPlugin>? = null
    val screenLanguageConverterPlugin get() = _screenLanguageConverterPlugin
    //TODO: 以后可以添加新插件支持
    fun init() {
        ologger.info { "InitPlugins" }
        loadPlugins()
    }
    fun loadPlugins() {
        val config = ConfigManager.pluginConfig
        val getScreenPlugin = PluginSelector.getScreenPlugin(config.getScreenPluginId)
        val storagePlugin = PluginSelector.storagePlugin(config.storagePluginId)
        val naturalLanguageConverterPlugin = PluginSelector.naturalLanguageConverterPlugin(config.naturalLanguageConverterPluginId)
        val screenLanguageConverterPlugin = PluginSelector.screenLanguageConverterPlugin(config.screenLanguageConverterPluginId)
        if (getScreenPlugin.isFailure || storagePlugin.isFailure || naturalLanguageConverterPlugin.isFailure || screenLanguageConverterPlugin.isFailure) {
            val errorMessage = """
                getScreenPlugin: ${getScreenPlugin.exceptionOrNull()?.message}
                storagePlugin: ${storagePlugin.exceptionOrNull()?.message}
                naturalLanguageConverterPlugin: ${naturalLanguageConverterPlugin.exceptionOrNull()?.message}
                screenLanguageConverterPlugin: ${screenLanguageConverterPlugin.exceptionOrNull()?.message}
            """.trimIndent()
            ologger.error { errorMessage }
        }
        _getScreenPlugin = getScreenPlugin
        _storagePlugin = storagePlugin
        _naturalLanguageConverterPlugin = naturalLanguageConverterPlugin
        _screenLanguageConverterPlugin = screenLanguageConverterPlugin
    }
    private inline fun <reified P: PluginBasic> getPluginState(pluginId: String): PluginState<P> {
        val plugin = PluginSelector.plugins[pluginId]
        return if (plugin == null) {
            PluginState(null, false, Exception("Plugin $pluginId not found"))
        } else if (plugin !is P) {
            PluginState(null, false, Exception("Plugin $pluginId [${plugin::class.java}] is not ${P::class.java.name}"))
        } else {
            PluginState(plugin, plugin.initialized, plugin.getLeastInitOrLoadException())
        }
    }
    fun getGetScreenPluginState(): PluginState<AbsGetScreenPlugin> {
        return getPluginState(ConfigManager.pluginConfig.getScreenPluginId)
    }
    fun getStoragePluginState(): PluginState<AbsStoragePlugin> {
        return getPluginState(ConfigManager.pluginConfig.storagePluginId)
    }
    fun getNaturalLanguageConverterPluginState(): PluginState<AbsNaturalLanguageConverterPlugin> {
        return getPluginState(ConfigManager.pluginConfig.naturalLanguageConverterPluginId)
    }
    fun getScreenLanguageConverterPluginState(): PluginState<AbsScreenLanguageConverterPlugin> {
        return getPluginState(ConfigManager.pluginConfig.screenLanguageConverterPluginId)
    }
}