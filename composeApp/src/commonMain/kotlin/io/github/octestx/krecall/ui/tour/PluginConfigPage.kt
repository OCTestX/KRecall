package io.github.octestx.krecall.ui.tour

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.kotlin.fibonacci.ui.toast
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.*
import io.klogging.noCoLogger
import ui.core.AbsUIPage

class PluginConfigPage(model: PluginConfigModel): AbsUIPage<Any?, PluginConfigPage.PluginConfigState, PluginConfigPage.PluginConfigAction>(model) {
    private val ologger = noCoLogger<PluginConfigPage>()
    @Composable
    override fun UI(state: PluginConfigState) {
        Column {
            Row {
                val allInitialized = PluginManager.allPluginsInitialized.collectAsState()
                if (allInitialized.value) {
                    Text("AllPluginsInitialized")
                    Button(onClick = {
                        state.action(PluginConfigAction.ConfigDone)
                    }) {
                        Text("ConfigDone")
                    }
                } else {
                    Button(onClick = {
                        PluginManager.initAllPlugins()
                    }) {
                        Text("InitAllPlugins")
                    }
                }

            }
            Box(Modifier.fillMaxSize()) {
                val scrollState = rememberLazyListState()
                LazyColumn(state = scrollState) {
                    item {
                        PluginCard(state.getScreenPlugin, state.availableGetScreenPlugins, "截屏插件") {
                            state.action(PluginConfigAction.SelectGetScreenPlugin(it))
                        }
                    }
                    item {
                        PluginCard(state.storagePlugin, state.availableStoragePlugins, "存储插件") {
                            state.action(PluginConfigAction.SelectStoragePlugin(it))
                        }
                    }
                    item {
                        PluginCard(state.naturalLanguageConverterPlugin, state.availableNaturalLanguageConverterPlugins, "自然语言转换插件") {
                            state.action(PluginConfigAction.SelectNaturalLanguageConverterPlugin(it))
                        }
                    }
                    item {
                        PluginCard(state.screenLanguageConverterPlugin, state.availableScreenLanguageConverterPlugins, "图片转换插件") {
                            state.action(PluginConfigAction.SelectScreenLanguageConverterPlugin(it))
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd) // 右侧对齐
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f)), // 半透明背景
                    adapter = rememberScrollbarAdapter(
                        scrollState = scrollState,
                    )
                )
            }
        }
    }
    @Composable
    private fun <P: PluginBasic> PluginCard(pluginData: Result<P>, availablePlugins: List<P>, type: String, selectedPlugin: (pluginId: String) -> Unit) {
        Column {
            Row {
                Text("Available: ")
                LazyRow {
                    items(availablePlugins) { plugin ->
                        Button(onClick = {
                            selectedPlugin(plugin.pluginId)
                        }, enabled = (pluginData.getOrNull()?.pluginId ?: System.nanoTime()) != plugin.pluginId) {
                            Text(plugin.pluginId)
                        }
                    }
                }
            }
            var err: Throwable? by remember { mutableStateOf(null) }
            pluginData.onFailure {
                Text("$type: 未加载[${it.message}]", color = MaterialTheme.colorScheme.secondary)
            }
            pluginData.onSuccess {
                val initialized by it.initialized.collectAsState()
                Row {
                    if (initialized) {
                        Text("$type[${it.pluginId}]: 已初始化", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    } else if (err == null) {
                        Text("$type[${it.pluginId}]: 未初始化", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    } else {
                        Text("$type[${it.pluginId}]: 未初始化[${err?.message}]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    }
                    Button(onClick = {
                        it.tryInit().also { initResult ->
                            if (initResult is PluginBasic.InitResult.Failed) err = initResult.exception
                        }
                    }) {
                        Text("INIT", modifier = Modifier.padding(4.dp))
                    }
                }
                Surface(Modifier.padding(12.dp)) {
                    it.UI()
                }
            }
            LaunchedEffect(pluginData) {
                pluginData.onFailure {
                    ologger.error(it) { "插件异常: ${it.message}" }
                }
            }
            LaunchedEffect(err) {
                err?.also {
                    ologger.error(it) { "插件运行时异常: ${it.message}" }
                }
            }
        }
    }

    sealed class PluginConfigAction : AbsUIAction() {
        //只有当插件全部初始化完毕后用户才能主动调用这个事件
        data object ConfigDone: PluginConfigAction()
        data class SelectGetScreenPlugin(val pluginId: String): PluginConfigAction()
        data class SelectStoragePlugin(val pluginId: String): PluginConfigAction()
        data class SelectNaturalLanguageConverterPlugin(val pluginId: String): PluginConfigAction()
        data class SelectScreenLanguageConverterPlugin(val pluginId: String): PluginConfigAction()
    }
    data class PluginConfigState(
        val getScreenPlugin: Result<AbsGetScreenPlugin>,
        val availableGetScreenPlugins: List<AbsGetScreenPlugin>,
        val storagePlugin: Result<AbsStoragePlugin>,
        val availableStoragePlugins: List<AbsStoragePlugin>,
        val naturalLanguageConverterPlugin: Result<AbsNaturalLanguageConverterPlugin>,
        val availableNaturalLanguageConverterPlugins: List<AbsNaturalLanguageConverterPlugin>,
        val screenLanguageConverterPlugin: Result<AbsScreenLanguageConverterPlugin>,
        val availableScreenLanguageConverterPlugins: List<AbsScreenLanguageConverterPlugin>,
        val action: (PluginConfigAction) -> Unit,
    ): AbsUIState<PluginConfigAction>()

    class PluginConfigModel(private val configDone: () -> Unit): AbsUIModel<Any?, PluginConfigState, PluginConfigAction>() {
        val ologger = noCoLogger<PluginConfigModel>()
        @Composable
        override fun CreateState(params: Any?): PluginConfigState {
            return PluginConfigState(
                PluginManager.getScreenPlugin(),
                PluginManager.availableScreenPlugins.values.toList(),
                PluginManager.getStoragePlugin(),
                PluginManager.availableStoragePlugins.values.toList(),
                PluginManager.getNaturalLanguageConverterPlugin(),
                PluginManager.availableNaturalLanguageConverterPlugins.values.toList(),
                PluginManager.getScreenLanguageConverterPlugin(),
                PluginManager.availableScreenLanguageConverterPlugins.values.toList(),
            ) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: PluginConfigAction) {
            when(action) {
                PluginConfigAction.ConfigDone -> {
                    if (PluginManager.getScreenPlugin().getOrNull()?.initialized?.value != true) {
                        ologger.info { "GetScreenPlugin is not initialized" }
                    } else if (PluginManager.getStoragePlugin().getOrNull()?.initialized?.value != true) {
                        ologger.info { "StoragePlugin is not initialized" }
                    } else if (PluginManager.getNaturalLanguageConverterPlugin().getOrNull()?.initialized?.value != true) {
                        ologger.info { "NaturalLanguageConverterPlugin is not initialized" }
                    } else if (PluginManager.getScreenLanguageConverterPlugin().getOrNull()?.initialized?.value != true) {
                        ologger.info { "ScreenLanguageConverterPlugin is not initialized" }
                    } else {
                        ologger.info { "All plugins are initialized" }
                        toast.applyShow("TODO, ConfigDone")
                    }
                    configDone()
                }

                is PluginConfigAction.SelectGetScreenPlugin -> {
                    PluginManager.setScreenPlugin(action.pluginId)
                }

                is PluginConfigAction.SelectNaturalLanguageConverterPlugin -> {
                    PluginManager.setNaturalLanguageConverterPlugin(action.pluginId)
                }
                is PluginConfigAction.SelectScreenLanguageConverterPlugin -> {
                    PluginManager.setScreenLanguageConverterPlugin(action.pluginId)
                }
                is PluginConfigAction.SelectStoragePlugin -> {
                    PluginManager.setStoragePlugin(action.pluginId)
                }
            }
        }
    }
}