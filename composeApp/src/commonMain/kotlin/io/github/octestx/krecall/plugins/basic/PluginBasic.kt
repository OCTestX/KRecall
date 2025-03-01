package io.github.octestx.krecall.plugins.basic

import androidx.compose.runtime.Composable
import org.koin.java.KoinJavaComponent.get
import java.io.File

abstract class PluginBasic(val pluginId: String) {
    abstract fun loadInner()
    @Composable
    abstract fun UI()
    protected abstract fun tryInitInner(): Exception?
    fun tryInit(): Exception? {
        val exception = tryInitInner()
        if (exception != null) {
            leastInitException = exception
        }
        return exception
    }
    abstract val initialized: Boolean
    private var leastInitException: Exception? = null
    fun getLeastInitOrLoadException(): Exception? {
        return leastInitException
    }
    protected val pluginDir: File = get<IPluginContext>(IPluginContext::class.java).getPluginDir(pluginId)
}