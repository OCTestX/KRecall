package io.github.octestx.krecall.plugins.basic

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import org.koin.java.KoinJavaComponent.get
import java.io.File

abstract class PluginBasic(val pluginId: String) {
    abstract fun load()
    abstract fun unload()
    @Composable
    abstract fun UI()
    protected abstract fun tryInitInner(): InitResult
    fun tryInit(): InitResult {
        return if (initialized.value.not()) tryInitInner()
        else InitResult.Success
    }
    abstract val initialized: StateFlow<Boolean>
    protected val pluginDir: File = get<IPluginContext>(IPluginContext::class.java).getPluginDir(pluginId)

    sealed class InitResult {
        data object Success: InitResult()
        // failed也会跳转到UI配置
        data class Failed(val exception: Exception): InitResult()
        data object RequestConfigUI: InitResult()
    }
}