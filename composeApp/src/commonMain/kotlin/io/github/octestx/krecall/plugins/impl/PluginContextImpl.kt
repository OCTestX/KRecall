package io.github.octestx.krecall.plugins.impl

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import io.github.octestx.krecall.plugins.basic.IPluginContext
import io.github.octestx.krecall.repository.DataDB
import io.github.octestx.krecall.repository.FileTree
import java.io.File

class PluginContextImpl: IPluginContext {
    override fun getPluginDir(pluginId: String): File {
        val path = FileTree.pluginData(pluginId)
        return File(path.toString())
    }

    override fun getPluginScreenDir(pluginId: String): File {
        val path = FileTree.pluginScreenDir(pluginId)
        return File(path.toString())
    }

    override fun markScreenData(timestamp: Long, mark: String) {
        DataDB.markScreenData(timestamp, mark)
    }

    override fun listTimestampWithMark(mark: String): List<Long> = DataDB.listTimestampWithMark(mark)

    override fun listTimestampWithNotMark(mark: String): List<Long> = DataDB.listTimestampWithNotMark(mark)
}

