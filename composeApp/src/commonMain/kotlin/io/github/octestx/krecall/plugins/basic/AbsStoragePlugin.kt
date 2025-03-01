package io.github.octestx.krecall.plugins.basic

import org.koin.java.KoinJavaComponent.get
import java.io.File
import java.io.OutputStream

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsStoragePlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun requireOutputStream(timestamp: Long): OutputStream
    abstract suspend fun requireFileBitItNotExits(timestamp: Long): File
    abstract suspend fun processed(timestamp: Long)
    /**
     * @exception NoSuchFileException
     */
    abstract suspend fun getScreenData(timestamp: Long): Result<ByteArray>
    protected val imageDir: File = get<IPluginContext>(IPluginContext::class.java).getPluginDir(pluginId)
    protected fun mark(timestamp: Long, mark: String) = get<IPluginContext>(IPluginContext::class.java).markScreenData(timestamp, mark)
    protected fun listTimestampWithMark(mark: String): List<Long> = get<IPluginContext>(IPluginContext::class.java).listTimestampWithMark(mark)
    protected fun listTimestampWithNotMark(mark: String): List<Long> = get<IPluginContext>(IPluginContext::class.java).listTimestampWithNotMark(mark)
}