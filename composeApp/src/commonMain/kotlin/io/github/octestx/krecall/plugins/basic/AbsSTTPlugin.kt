package io.github.octestx.krecall.plugins.basic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsSTTPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun emit(audioData: ByteArray, bytesRead: Int)
    abstract suspend fun getResult(): STTResult
}

@Serializable
data class STTResult(
    val text: String,
    val words: List<WordResult> = emptyList()
)

// 词语详细信息
@Serializable
data class WordResult(
    @SerialName("conf") val confidence: Double,
    @SerialName("start") val startTime: Double,
    @SerialName("end") val endTime: Double,
    @SerialName("word") val word: String
)