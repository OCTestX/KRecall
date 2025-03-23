package io.github.octestx.krecall.plugins.basic

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsSTTPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun recognize(audioData: ByteArray, controller: STTResultController)

    interface STTResultController {
        fun newLine()
        fun getCurrentLineId(): String
        fun changeLine(lineId: String, line: String)
    }
}