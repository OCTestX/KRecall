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
    protected abstract fun tryInitInner(): Exception?
    fun tryInit(): Exception? {
        return if (initialized.value.not()) tryInitInner()
        else null
    }
    abstract val initialized: StateFlow<Boolean>
    protected val pluginDir: File = get<IPluginContext>(IPluginContext::class.java).getPluginDir(pluginId)
}