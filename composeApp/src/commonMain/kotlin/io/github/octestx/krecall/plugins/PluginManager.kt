package io.github.octestx.krecall.plugins

import io.github.octestx.krecall.plugins.basic.*
import io.github.octestx.krecall.repository.ConfigManager
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

object PluginManager {
    private val ologger = noCoLogger<PluginManager>()

    //TODO: 以后可以添加新插件支持
    private val _getScreenPlugin: MutableStateFlow<Result<AbsGetScreenPlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    private val _storagePlugin: MutableStateFlow<Result<AbsStoragePlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    private val _naturalLanguageConverterPlugin: MutableStateFlow<Result<AbsNaturalLanguageConverterPlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    private val _screenLanguageConverterPlugin: MutableStateFlow<Result<AbsScreenLanguageConverterPlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))

    val getScreenPlugin: StateFlow<Result<AbsGetScreenPlugin>> get() = _getScreenPlugin
    val storagePlugin: StateFlow<Result<AbsStoragePlugin>> get() = _storagePlugin
    val naturalLanguageConverterPlugin: StateFlow<Result<AbsNaturalLanguageConverterPlugin>> get() = _naturalLanguageConverterPlugin
    val screenLanguageConverterPlugin: StateFlow<Result<AbsScreenLanguageConverterPlugin>> get() = _screenLanguageConverterPlugin

    suspend fun init() {
        ologger.info { "InitPlugins" }
        reloadPlugins()
        createAllPluginsInitializedFlow()
    }

    private fun reloadPlugins() {
        _getScreenPlugin.value.getOrNull()?.unload()
        _storagePlugin.value.getOrNull()?.unload()
        _naturalLanguageConverterPlugin.value.getOrNull()?.unload()
        _screenLanguageConverterPlugin.value.getOrNull()?.unload()
        val config = ConfigManager.pluginConfig
        val getScreenPlugin = PluginSelector.getScreenPlugin(config.getScreenPluginId)
        val storagePlugin = PluginSelector.storagePlugin(config.storagePluginId)
        val naturalLanguageConverterPlugin = PluginSelector.naturalLanguageConverterPlugin(config.naturalLanguageConverterPluginId)
        val screenLanguageConverterPlugin = PluginSelector.screenLanguageConverterPlugin(config.screenLanguageConverterPluginId)
        if (getScreenPlugin.isFailure || storagePlugin.isFailure || naturalLanguageConverterPlugin.isFailure || screenLanguageConverterPlugin.isFailure) {
            val errorMessage = """
                getScreenPlugin: ${getScreenPlugin.exceptionOrNull()?.message}                storagePlugin: ${storagePlugin.exceptionOrNull()?.message}                naturalLanguageConverterPlugin: ${naturalLanguageConverterPlugin.exceptionOrNull()?.message}                screenLanguageConverterPlugin: ${screenLanguageConverterPlugin.exceptionOrNull()?.message}            """.trimIndent()
            ologger.error { errorMessage }
        }
        _getScreenPlugin.value = getScreenPlugin.apply { onSuccess { it.load() } }
        _storagePlugin.value = storagePlugin.apply { onSuccess { it.load() } }
        _naturalLanguageConverterPlugin.value = naturalLanguageConverterPlugin.apply { onSuccess { it.load() } }
        _screenLanguageConverterPlugin.value = screenLanguageConverterPlugin.apply { onSuccess { it.load() } }
    }

    val availableScreenPlugins: Map<String, AbsGetScreenPlugin> = PluginSelector.plugins.filter { it.value is AbsGetScreenPlugin }.mapValues { it.value as AbsGetScreenPlugin }
    fun setScreenPlugin(pluginId: String) {
        if (pluginId == _getScreenPlugin.value.getOrNull()?.pluginId) {
            return
        }
        if (availableScreenPlugins.containsKey(pluginId)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(getScreenPluginId = pluginId))
            reloadPlugins()
        } else {
            ologger.error { "Plugin $pluginId not found" }
        }
    }
    fun getScreenPlugin(): Result<AbsGetScreenPlugin> {
        return _getScreenPlugin.value
    }

    val availableStoragePlugins: Map<String, AbsStoragePlugin> = PluginSelector.plugins.filter { it.value is AbsStoragePlugin }.mapValues { it.value as AbsStoragePlugin }
    fun setStoragePlugin(pluginId: String) {
        if (pluginId == _storagePlugin.value.getOrNull()?.pluginId) {
            return
        }
        if (availableStoragePlugins.containsKey(pluginId)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(storagePluginId = pluginId))
            reloadPlugins()
        } else {
            ologger.error { "Plugin $pluginId not found" }
        }
    }
    fun getStoragePlugin(): Result<AbsStoragePlugin> {
        return _storagePlugin.value
    }
    val availableNaturalLanguageConverterPlugins: Map<String, AbsNaturalLanguageConverterPlugin> = PluginSelector.plugins.filter { it.value is AbsNaturalLanguageConverterPlugin }.mapValues { it.value as AbsNaturalLanguageConverterPlugin }
    fun setNaturalLanguageConverterPlugin(pluginId: String) {
        if (pluginId == _naturalLanguageConverterPlugin.value.getOrNull()?.pluginId) {
            return
        }
        if (availableNaturalLanguageConverterPlugins.containsKey(pluginId)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(naturalLanguageConverterPluginId = pluginId))
            reloadPlugins()
        } else {
            ologger.error { "Plugin $pluginId not found" }
        }
    }
    fun getNaturalLanguageConverterPlugin(): Result<AbsNaturalLanguageConverterPlugin> {
        return _naturalLanguageConverterPlugin.value
    }
    val availableScreenLanguageConverterPlugins: Map<String, AbsScreenLanguageConverterPlugin> = PluginSelector.plugins.filter { it.value is AbsScreenLanguageConverterPlugin }.mapValues { it.value as AbsScreenLanguageConverterPlugin }
    fun setScreenLanguageConverterPlugin(pluginId: String) {
        if (pluginId == _screenLanguageConverterPlugin.value.getOrNull()?.pluginId) {
            return
        }
        if (availableScreenLanguageConverterPlugins.containsKey(pluginId)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(screenLanguageConverterPluginId = pluginId))
            reloadPlugins()
        } else {
            ologger.error { "Plugin $pluginId not found" }
        }
    }
    fun getScreenLanguageConverterPlugin(): Result<AbsScreenLanguageConverterPlugin> {
        return _screenLanguageConverterPlugin.value
    }

    fun initAllPlugins() {
        getScreenPlugin().getOrThrow().tryInit()?.apply { throw this }
        getStoragePlugin().getOrThrow().tryInit()?.apply { throw this }
        getNaturalLanguageConverterPlugin().getOrThrow().tryInit()?.apply { throw this }
        getScreenLanguageConverterPlugin().getOrThrow().tryInit()?.apply { throw this }
    }
    lateinit var allPluginsInitialized: StateFlow<Boolean> private set
    private suspend fun createAllPluginsInitializedFlow() {
        allPluginsInitialized = combine(
            _getScreenPlugin.value.map { it.initialized }.getOrThrow(),
            _storagePlugin.value.map { it.initialized }.getOrThrow(),
            _naturalLanguageConverterPlugin.value.map { it.initialized }.getOrThrow(),
            _screenLanguageConverterPlugin.value.map { it.initialized }.getOrThrow(),
        ) { plugins ->
            plugins.all { it }
        }.stateIn(CoroutineScope(Dispatchers.IO))
    }
}