package io.github.octestx.krecall.plugins.stt

import io.github.octestx.krecall.plugins.basic.WordResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 最终结果（完整句子）
@Serializable
data class VoskFinalResult(
    @SerialName("text") val text: String,
    @SerialName("result") val words: List<WordResult> = emptyList()
)

// 中间结果（实时片段）
@Serializable
data class VoskPartialResult(
    @SerialName("partial") val text: String?,
    @SerialName("partial_result") val words: List<WordResult> = emptyList()
)