package io.github.octestx.krecall.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import io.github.kotlin.fibonacci.ui.toast
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.*
import io.klogging.noCoLogger
import ui.core.AbsUIPage

object PluginConfigPage: AbsUIPage<Any?, PluginConfigPage.PluginConfigState, PluginConfigPage.PluginConfigAction>(model = PluginConfigModel()) {
    private val ologger = noCoLogger<PluginConfigPage>()
    @Composable
    override fun UI(state: PluginConfigState) {
        LazyColumn {
            item { PluginCard(state.getScreenPlugin, "截屏插件") }
            item { PluginCard(state.storagePlugin, "存储插件") }
            item { PluginCard(state.naturalLanguageConverterPlugin, "自然语言转换插件") }
            item { PluginCard(state.screenLanguageConverterPlugin, "图片转换插件") }
        }
    }
    @Composable
    private fun <P: PluginBasic> PluginCard(pluginData: PluginManager.PluginState<P>, type: String) {
        Column {
            if (pluginData.plugin == null) {
                Text("$type: 未加载[${pluginData.err!!.message}]", color = MaterialTheme.colorScheme.secondary)
            }
            if (pluginData.initialized) {
                Text("$type: 已初始化", color = MaterialTheme.colorScheme.primary)
            } else {
                Text("$type: 未初始化[${pluginData.err?.message}]", color = MaterialTheme.colorScheme.secondary)
            }
            LaunchedEffect(pluginData) {
                if (pluginData.err != null) {
                    ologger.error(pluginData.err) { "插件异常: ${pluginData.err.message}" }
                }
            }
        }
    }

    sealed class PluginConfigAction : AbsUIAction() {
        data object InitPlugin: PluginConfigAction()
        //只有当插件全部初始化完毕后用户才能主动调用这个事件
        data object ConfigDone: PluginConfigAction()
    }
    data class PluginConfigState(
        val getScreenPlugin: PluginManager.PluginState<AbsGetScreenPlugin>,
        val storagePlugin: PluginManager.PluginState<AbsStoragePlugin>,
        val naturalLanguageConverterPlugin: PluginManager.PluginState<AbsNaturalLanguageConverterPlugin>,
        val screenLanguageConverterPlugin: PluginManager.PluginState<AbsScreenLanguageConverterPlugin>
    ): AbsUIState<PluginConfigAction>()

    class PluginConfigModel: AbsUIModel<Any?, PluginConfigState, PluginConfigAction>() {
        private var getScreenPluginInitialized by mutableStateOf(false)
        private var storagePluginInitialized by mutableStateOf(false)
        private var naturalLanguageConverterPluginInitialized by mutableStateOf(false)
        private var screenLanguageConverterPluginInitialized by mutableStateOf(false)
        @Composable
        override fun CreateState(params: Any?): PluginConfigState {
            return PluginConfigState(
                PluginManager.getGetScreenPluginState(),
                PluginManager.getStoragePluginState(),
                PluginManager.getNaturalLanguageConverterPluginState(),
                PluginManager.getScreenLanguageConverterPluginState(),
            )
        }
        override fun actionExecute(params: Any?, action: PluginConfigAction) {
            when(action) {
                PluginConfigAction.InitPlugin -> {
                    if (PluginManager.getScreenPlugin?.isFailure != false || PluginManager.storagePlugin?.isFailure != false || PluginManager.naturalLanguageConverterPlugin?.isFailure != false || PluginManager.screenLanguageConverterPlugin?.isFailure != false) {
                        toast.applyShow("有些插件未导入，无法初始化")
                        return
                    }
                    try {
                        if (getScreenPluginInitialized.not()) {
                            PluginManager.getScreenPlugin!!.getOrThrow().tryInit()?.also { cause ->
                                throw ExceptionInInitializerError("GetScreenPlugin cannot init").apply { initCause(cause) }
                            }
                        }
                        if (storagePluginInitialized.not()) {
                            PluginManager.storagePlugin!!.getOrThrow().tryInit()?.also { cause ->
                                throw ExceptionInInitializerError("StoragePlugin cannot init").apply { initCause(cause) }
                            }
                        }
                        if (naturalLanguageConverterPluginInitialized.not()) {
                            PluginManager.naturalLanguageConverterPlugin!!.getOrThrow().tryInit()?.also { cause ->
                                throw ExceptionInInitializerError("NaturalLanguageConverterPlugin cannot init").apply { initCause(cause) }
                            }
                        }
                        if (screenLanguageConverterPluginInitialized.not()) {
                            PluginManager.screenLanguageConverterPlugin!!.getOrThrow().tryInit()?.also { cause ->
                                throw ExceptionInInitializerError("ScreenLanguageConverterPlugin cannot init").apply { initCause(cause) }
                            }
                        }
                    } catch (e: Throwable) {
                        toast.applyShowForError(e)
                    }
                }
                PluginConfigAction.ConfigDone -> {
                    toast.applyShow("TODO, ConfigDone")
                }
            }
        }
    }
}